package com.example.campusfind.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campusfind.dto.FoundItemRequest;
import com.example.campusfind.dto.FoundItemResponse;
import com.example.campusfind.entity.Category;
import com.example.campusfind.entity.CaseState;
import com.example.campusfind.entity.FoundItem;
import com.example.campusfind.entity.FoundStatus;
import com.example.campusfind.entity.User;
import com.example.campusfind.entity.UserRole;
import com.example.campusfind.repository.FoundItemRepository;
import com.example.campusfind.repository.LostReportRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class FoundItemService {

    private final FoundItemRepository foundItemRepository;
    private final CategoryService categoryService;
    private final AuthService authService;
    private final LostReportRepository lostReportRepository;

    public FoundItemService(FoundItemRepository foundItemRepository,
                            CategoryService categoryService,
                            AuthService authService,
                            LostReportRepository lostReportRepository) {
        this.foundItemRepository = foundItemRepository;
        this.categoryService = categoryService;
        this.authService = authService;
        this.lostReportRepository = lostReportRepository;
    }

    @Transactional(readOnly = true)
    public List<FoundItemResponse> search(Long categoryId, String location, String keyword, String status,
                                          HttpSession session) {
        FoundStatus parsedStatus = parseSearchStatus(status);
        boolean includePhone = canViewFinderContact(session);
        return foundItemRepository.search(
                parsedStatus,
                categoryId,
                clean(location),
                clean(keyword)).stream().map(item -> toResponse(item, includePhone)).toList();
    }

    @Transactional(readOnly = true)
    public FoundItemResponse getById(Long id, HttpSession session) {
        return toResponse(getEntity(id), canViewFinderContact(session));
    }

    @Transactional
    public FoundItemResponse create(FoundItemRequest request, HttpSession session) {
        User user = authService.requireUser(session);

        FoundItem item = new FoundItem();
        item.setReportingStaff(user);
        apply(item, request);
        item.setStatus(FoundStatus.AVAILABLE);
        item.setCaseState(CaseState.PENDING);
        return toResponse(foundItemRepository.save(item));
    }

    @Transactional
    public FoundItemResponse update(Long id, FoundItemRequest request, HttpSession session) {
        User actor = authService.requireUser(session);
        FoundItem item = getEntity(id);
        requireOwnerOrAdmin(actor, item.getReportingStaff());
        if (item.getStatus() == FoundStatus.RETURNED) {
            throw new BadRequestException("A returned found item cannot be edited");
        }
        apply(item, request);
        return toResponse(foundItemRepository.save(item));
    }

    @Transactional
    public void delete(Long id, HttpSession session) {
        User actor = authService.requireUser(session);
        FoundItem item = getEntity(id);
        requireOwnerOrAdmin(actor, item.getReportingStaff());
        if (item.getStatus() != FoundStatus.AVAILABLE) {
            throw new BadRequestException("Only AVAILABLE found items can be deleted");
        }
        foundItemRepository.delete(item);
    }

    @Transactional
    public FoundItemResponse updateStatus(Long id, String requestedStatus, HttpSession session) {
        User actor = authService.requireUser(session);
        FoundItem item = getEntity(id);
        requireOwnerOrAdmin(actor, item.getReportingStaff());

        FoundStatus next = parseStatus(requestedStatus);
        FoundStatus current = item.getStatus();

        if (current == next) {
            return toResponse(item);
        }

        if (current == FoundStatus.RETURNED || current == FoundStatus.UNAVAILABLE) {
            throw new BadRequestException("A " + current + " item cannot change status again");
        }
        if (next == FoundStatus.RETURNED && current != FoundStatus.CLAIMED && current != FoundStatus.MATCHED) {
            throw new BadRequestException("A found item cannot be marked 'returned' unless it is first marked 'claimed'");
        }
        if (current == FoundStatus.AVAILABLE
                && next != FoundStatus.CLAIMED
                && next != FoundStatus.MATCHED
                && next != FoundStatus.UNAVAILABLE) {
            throw new BadRequestException("An AVAILABLE item can only move to CLAIMED, MATCHED or UNAVAILABLE");
        }
        if (next == FoundStatus.MATCHED
                && lostReportRepository.findByMatchedFoundItemId(item.getId()).isEmpty()) {
            throw new BadRequestException("A Found Item can only become MATCHED through Confirm Match");
        }
        if ((current == FoundStatus.CLAIMED || current == FoundStatus.MATCHED) && next != FoundStatus.RETURNED) {
            throw new BadRequestException("A " + current + " item can only move to RETURNED");
        }

        item.setStatus(next);
        if (next == FoundStatus.CLAIMED || next == FoundStatus.MATCHED) {
            item.setCaseState(CaseState.MATCHED);
        }
        if (next == FoundStatus.UNAVAILABLE) {
            item.setCaseState(CaseState.PENDING);
        }
        if (next == FoundStatus.RETURNED) {
            item.setCaseState(CaseState.RETURNED);
        }
        FoundItem saved = foundItemRepository.save(item);
        if (next == FoundStatus.RETURNED) {
            lostReportRepository.findByMatchedFoundItemId(saved.getId()).ifPresent(lost -> {
                lost.setStatus(com.example.campusfind.entity.LostStatus.RETURNED);
                lost.setCaseState(CaseState.RETURNED);
                lostReportRepository.save(lost);
            });
        }
        return toResponse(saved);
    }

    public FoundItem getEntity(Long id) {
        return foundItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Found item not found: " + id));
    }

    private void apply(FoundItem item, FoundItemRequest request) {
        Category category = categoryService.getEntity(request.categoryId());
        item.setCategory(category);
        item.setItemName(request.itemName().trim());
        item.setDescription(request.description().trim());
        item.setLocation(request.location().trim());
        item.setDateFound(request.dateFound());
        item.setFinderPhoneNumber(request.finderPhoneNumber().trim());
    }

    private void requireStaffOrAdmin(User user) {
        if (user.getRole() != UserRole.STAFF && user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only staff or admin users can report found items");
        }
    }

    private void requireOwnerOrAdmin(User actor, User owner) {
        boolean allowed = actor.getRole() == UserRole.ADMIN || actor.getId().equals(owner.getId());
        if (!allowed) {
            throw new ForbiddenException("Only the reporting staff member or an admin can change this found item");
        }
    }

    private FoundStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        try {
            return FoundStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid found-item status. Use AVAILABLE, CLAIMED, MATCHED, UNAVAILABLE or RETURNED");
        }
    }

    private FoundStatus parseSearchStatus(String value) {
        return value == null || value.isBlank() ? null : parseStatus(value);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean canViewFinderContact(HttpSession session) {
        return authService.findCurrentUser(session).isPresent();
    }

    private FoundItemResponse toResponse(FoundItem item) {
        return toResponse(item, true);
    }

    private FoundItemResponse toResponse(FoundItem item, boolean includePhone) {
        return new FoundItemResponse(
                item.getId(),
                item.getReportingStaff().getId(),
                item.getReportingStaff().getFullName(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getItemName(),
                item.getDescription(),
                item.getLocation(),
                item.getDateFound(),
                includePhone ? item.getFinderPhoneNumber() : null,
                item.getStatus().name(),
            item.getCaseState().name(),
                item.getCreatedAt());
    }
}
