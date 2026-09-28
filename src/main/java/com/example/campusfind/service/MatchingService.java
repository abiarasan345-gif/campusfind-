package com.example.campusfind.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campusfind.dto.ConfirmMatchRequest;
import com.example.campusfind.dto.MatchResponse;
import com.example.campusfind.dto.CaseResolutionRequest;
import com.example.campusfind.entity.CaseState;
import com.example.campusfind.entity.FoundItem;
import com.example.campusfind.entity.FoundStatus;
import com.example.campusfind.entity.LostReport;
import com.example.campusfind.entity.LostStatus;
import com.example.campusfind.entity.User;
import com.example.campusfind.entity.UserRole;
import com.example.campusfind.repository.FoundItemRepository;
import com.example.campusfind.repository.LostReportRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class MatchingService {

    private final LostReportRepository lostReportRepository;
    private final FoundItemRepository foundItemRepository;
    private final AuthService authService;

    public MatchingService(LostReportRepository lostReportRepository,
                           FoundItemRepository foundItemRepository,
                           AuthService authService) {
        this.lostReportRepository = lostReportRepository;
        this.foundItemRepository = foundItemRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findMatches(Long lostId, HttpSession session) {
        authService.requireUser(session);
        List<LostReport> lostReports = lostId == null
                ? lostReportRepository.findAll().stream().filter(x -> x.getStatus() == LostStatus.OPEN).toList()
                : List.of(lostReportRepository.findById(lostId)
                        .orElseThrow(() -> new NotFoundException("Lost report not found: " + lostId)));

        if (lostId != null && lostReports.get(0).getStatus() != LostStatus.OPEN) {
            return List.of();
        }

        List<FoundItem> foundItems = foundItemRepository.findAll().stream()
                .filter(x -> x.getStatus() == FoundStatus.AVAILABLE)
                .toList();

        List<MatchResponse> matches = new ArrayList<>();
        for (LostReport lost : lostReports) {
            for (FoundItem found : foundItems) {
                int score = score(lost, found);
                if (score >= 50) {
                        matches.add(toMatchResponse(lost, found, score));
                }
            }
        }

        matches.sort(Comparator.comparingInt(MatchResponse::matchScore).reversed());
        return matches;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findConfirmedMatches(HttpSession session) {
        authService.requireUser(session);
        return lostReportRepository.findAll().stream()
                .filter(lost -> lost.getMatchedFoundItem() != null)
                .map(lost -> toMatchResponse(lost, lost.getMatchedFoundItem(), score(lost, lost.getMatchedFoundItem())))
                .sorted(Comparator.comparingInt(MatchResponse::matchScore).reversed())
                .toList();
    }

    @Transactional
    public MatchResponse confirmMatch(ConfirmMatchRequest request, HttpSession session) {
        User actor = authService.requireUser(session);
        LostReport lost = lostReportRepository.findById(request.lostReportId())
                .orElseThrow(() -> new NotFoundException("Lost report not found: " + request.lostReportId()));
        FoundItem found = foundItemRepository.findById(request.foundItemId())
                .orElseThrow(() -> new NotFoundException("Found item not found: " + request.foundItemId()));

        requireOwnerOrAdmin(actor, found.getReportingStaff());

        if (lost.getStatus() != LostStatus.OPEN) {
            throw new BadRequestException("Only OPEN lost reports can be matched");
        }
        if (lost.getMatchedFoundItem() != null
                || lostReportRepository.findByMatchedFoundItemId(found.getId()).isPresent()) {
            throw new ConflictException("This lost report or found item is already linked to a match");
        }
        if (found.getStatus() != FoundStatus.AVAILABLE) {
            throw new BadRequestException("Only AVAILABLE found items can be matched");
        }

        int score = score(lost, found);
        if (score < 50) {
            throw new BadRequestException("The selected items do not meet the minimum match criteria");
        }

        found.setStatus(FoundStatus.MATCHED);
        found.setCaseState(CaseState.MATCHED);
        lost.setMatchedFoundItem(found);
        lost.setStatus(LostStatus.MATCHED);
        lost.setCaseState(CaseState.MATCHED);
        foundItemRepository.save(found);
        lostReportRepository.save(lost);

        return toMatchResponse(lost, found, score);
    }

    @Transactional
    public MatchResponse updateResolution(Long lostReportId, CaseResolutionRequest request, HttpSession session) {
        User actor = authService.requireUser(session);
        LostReport lost = lostReportRepository.findById(lostReportId)
                .orElseThrow(() -> new NotFoundException("Lost report not found: " + lostReportId));
        FoundItem found = lost.getMatchedFoundItem();
        if (found == null) {
            throw new BadRequestException("Only a confirmed match can be resolved");
        }
        requireCaseAccess(actor, lost, found);

        CaseState next = parseResolution(request.resolution());
        switch (next) {
            case PENDING -> {
                requireMatchedPair(lost, found);
                lost.setCaseState(CaseState.PENDING);
                found.setCaseState(CaseState.PENDING);
            }
            case MATCHED -> {
                requireMatchedPair(lost, found);
                lost.setCaseState(CaseState.MATCHED);
                found.setCaseState(CaseState.MATCHED);
            }
            case RETURNED -> {
                requireMatchedPair(lost, found);
                lost.setStatus(LostStatus.RETURNED);
                found.setStatus(FoundStatus.RETURNED);
                lost.setCaseState(CaseState.RETURNED);
                found.setCaseState(CaseState.RETURNED);
            }
            case CLOSED -> {
                if (lost.getStatus() != LostStatus.RETURNED || found.getStatus() != FoundStatus.RETURNED) {
                    throw new BadRequestException("A case can only be closed after the item is RETURNED");
                }
                lost.setCaseState(CaseState.CLOSED);
                found.setCaseState(CaseState.CLOSED);
            }
        }

        foundItemRepository.save(found);
        lostReportRepository.save(lost);
        return toMatchResponse(lost, found, score(lost, found));
    }

    private void requireMatchedPair(LostReport lost, FoundItem found) {
        if (lost.getStatus() != LostStatus.MATCHED || found.getStatus() != FoundStatus.MATCHED) {
            throw new BadRequestException("The linked case must be MATCHED before this resolution");
        }
    }

    private void requireCaseAccess(User actor, LostReport lost, FoundItem found) {
        boolean allowed = actor.getRole() == UserRole.ADMIN
                || actor.getId().equals(lost.getReportingUser().getId())
                || actor.getId().equals(found.getReportingStaff().getId());
        if (!allowed) {
            throw new ForbiddenException("Only a case participant or an admin can resolve this case");
        }
    }

    private CaseState parseResolution(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("STILL_PENDING".equals(normalized)) normalized = "PENDING";
        try {
            return CaseState.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Resolution must be MATCHED, RETURNED, STILL PENDING or CLOSED");
        }
    }

    private MatchResponse toMatchResponse(LostReport lost, FoundItem found, int score) {
        return new MatchResponse(
                lost.getId(),
                found.getId(),
                lost.getItemName(),
                found.getItemName(),
                lost.getCategory().getName(),
                lost.getLocation(),
                found.getLocation(),
                score,
                reason(lost, found, score),
                lost.getDateLost(),
                found.getDateFound(),
                found.getFinderPhoneNumber(),
                lost.getStatus().name(),
                found.getStatus().name(),
                lost.getCaseState().name());
    }

    private void requireOwnerOrAdmin(User actor, User owner) {
        boolean allowed = actor.getRole() == UserRole.ADMIN || actor.getId().equals(owner.getId());
        if (!allowed) {
            throw new ForbiddenException("Only the reporting staff member or an admin can confirm this match");
        }
    }

    int score(LostReport lost, FoundItem found) {
        int score = 0;
        if (lost.getCategory().getId().equals(found.getCategory().getId())) {
            score += 50;
        }
        if (normalize(lost.getLocation()).equals(normalize(found.getLocation()))) {
            score += 30;
        } else if (normalize(lost.getLocation()).contains(normalize(found.getLocation()))
                || normalize(found.getLocation()).contains(normalize(lost.getLocation()))) {
            score += 15;
        }

        Set<String> lostWords = keywords(lost.getItemName() + " " + lost.getDescription());
        Set<String> foundWords = keywords(found.getItemName() + " " + found.getDescription());
        long common = lostWords.stream().filter(foundWords::contains).count();
        if (common >= 3) {
            score += 20;
        } else if (common >= 1) {
            score += 10;
        }
        return score;
    }

    private String reason(LostReport lost, FoundItem found, int score) {
        List<String> reasons = new ArrayList<>();
        if (lost.getCategory().getId().equals(found.getCategory().getId())) reasons.add("same category");
        if (normalize(lost.getLocation()).equals(normalize(found.getLocation()))) reasons.add("same location");
        else if (normalize(lost.getLocation()).contains(normalize(found.getLocation()))
                || normalize(found.getLocation()).contains(normalize(lost.getLocation()))) reasons.add("similar location");
        if (score >= 60) reasons.add("keyword similarity");
        return String.join(", ", reasons);
    }

    private Set<String> keywords(String text) {
        Set<String> stopWords = Set.of("the", "and", "with", "for", "this", "that", "item", "near", "from", "lost", "found");
        Set<String> words = new HashSet<>();
        Arrays.stream(normalize(text).split("\\W+"))
                .filter(w -> w.length() >= 3 && !stopWords.contains(w))
                .forEach(words::add);
        return words;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
