package com.example.campusfind.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campusfind.dto.LostReportRequest;
import com.example.campusfind.dto.LostReportResponse;
import com.example.campusfind.entity.Category;
import com.example.campusfind.entity.LostReport;
import com.example.campusfind.entity.LostStatus;
import com.example.campusfind.entity.User;
import com.example.campusfind.entity.UserRole;
import com.example.campusfind.repository.LostReportRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class LostReportService {

    private final LostReportRepository lostReportRepository;
    private final CategoryService categoryService;
    private final AuthService authService;

    public LostReportService(LostReportRepository lostReportRepository,
                             CategoryService categoryService,
                             AuthService authService) {
        this.lostReportRepository = lostReportRepository;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<LostReportResponse> search(Long categoryId, String location, String keyword, String status) {
        LostStatus parsedStatus = parseStatus(status);
        return lostReportRepository.search(
                parsedStatus,
                categoryId,
                clean(location),
                clean(keyword)).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LostReportResponse getById(Long id) {
        return toResponse(lostReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lost report not found: " + id)));
    }

    @Transactional
    public LostReportResponse create(LostReportRequest request, HttpSession session) {
        User user = authService.requireUser(session);
        LostReport report = new LostReport();
        report.setReportingUser(user);
        apply(report, request);
        report.setStatus(LostStatus.OPEN);
        return toResponse(lostReportRepository.save(report));
    }

    @Transactional
    public LostReportResponse update(Long id, LostReportRequest request, HttpSession session) {
        User actor = authService.requireUser(session);
        LostReport report = getEntity(id);
        requireOwnerOrAdmin(actor, report.getReportingUser());
        if (report.getStatus() != LostStatus.OPEN) {
            throw new BadRequestException("Only OPEN lost reports can be edited");
        }
        apply(report, request);
        return toResponse(lostReportRepository.save(report));
    }

    @Transactional
    public void delete(Long id, HttpSession session) {
        User actor = authService.requireUser(session);
        LostReport report = getEntity(id);
        requireOwnerOrAdmin(actor, report.getReportingUser());
        if (report.getStatus() != LostStatus.OPEN) {
            throw new BadRequestException("Only OPEN lost reports can be deleted");
        }
        lostReportRepository.delete(report);
    }

    public LostReport getEntity(Long id) {
        return lostReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lost report not found: " + id));
    }

    private void apply(LostReport report, LostReportRequest request) {
        Category category = categoryService.getEntity(request.categoryId());
        report.setCategory(category);
        report.setItemName(request.itemName().trim());
        report.setDescription(request.description().trim());
        report.setLocation(request.location().trim());
        report.setDateLost(request.dateLost());
    }

    private void requireOwnerOrAdmin(User actor, User owner) {
        boolean allowed = actor.getRole() == UserRole.ADMIN || actor.getId().equals(owner.getId());
        if (!allowed) {
            throw new ForbiddenException("You can only modify your own lost reports");
        }
    }

    private LostStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LostStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid lost-report status. Use OPEN, MATCHED or RETURNED");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private LostReportResponse toResponse(LostReport report) {
        return new LostReportResponse(
                report.getId(),
                report.getReportingUser().getId(),
                report.getReportingUser().getFullName(),
                report.getCategory().getId(),
                report.getCategory().getName(),
                report.getItemName(),
                report.getDescription(),
                report.getLocation(),
                report.getDateLost(),
                report.getStatus().name(),
            report.getCaseState().name(),
                report.getMatchedFoundItem() == null ? null : report.getMatchedFoundItem().getId(),
                report.getCreatedAt());
    }
}
