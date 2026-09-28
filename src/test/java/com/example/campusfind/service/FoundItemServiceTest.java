package com.example.campusfind.service;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.campusfind.entity.Category;
import com.example.campusfind.entity.FoundItem;
import com.example.campusfind.entity.FoundStatus;
import com.example.campusfind.entity.LostReport;
import com.example.campusfind.entity.LostStatus;
import com.example.campusfind.entity.User;
import com.example.campusfind.entity.UserRole;
import com.example.campusfind.repository.FoundItemRepository;
import com.example.campusfind.repository.LostReportRepository;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class FoundItemServiceTest {

    @Mock FoundItemRepository repository;
    @Mock CategoryService categoryService;
    @Mock AuthService authService;
    @Mock LostReportRepository lostReportRepository;
    @Mock HttpSession session;

    private FoundItemService service;
    private User staff;
    private Category category;
    private FoundItem item;

    @BeforeEach
    void setUp() {
        service = new FoundItemService(repository, categoryService, authService, lostReportRepository);
        staff = new User("staff1", "hash", UserRole.STAFF, "Staff One", "staff@test.local");
        staff.setId(10L);
        category = new Category("Charger");
        category.setId(1L);
        item = new FoundItem();
        item.setId(20L);
        item.setReportingStaff(staff);
        item.setCategory(category);
        item.setItemName("White Charger");
        item.setDescription("USB-C charger");
        item.setLocation("Library");
        item.setDateFound(LocalDate.now());
        item.setStatus(FoundStatus.AVAILABLE);
    }

    @Test
    void rejectsAvailableToReturned() {
        when(authService.requireUser(session)).thenReturn(staff);
        when(repository.findById(20L)).thenReturn(java.util.Optional.of(item));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.updateStatus(20L, "RETURNED", session));

        assertEquals("A found item cannot be marked 'returned' unless it is first marked 'claimed'", ex.getMessage());
    }

    @Test
    void allowsClaimedThenReturn() {
        when(authService.requireUser(session)).thenReturn(staff);
        when(repository.findById(20L)).thenReturn(java.util.Optional.of(item));
        when(repository.save(any(FoundItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        item.setStatus(FoundStatus.CLAIMED);
        assertEquals("RETURNED", service.updateStatus(20L, "RETURNED", session).status());
        assertEquals(FoundStatus.RETURNED, item.getStatus());
        verify(repository).save(item);
    }

    @Test
    void rejectsUnlinkedAvailableToMatched() {
        when(authService.requireUser(session)).thenReturn(staff);
        when(repository.findById(20L)).thenReturn(java.util.Optional.of(item));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.updateStatus(20L, "MATCHED", session));
        assertEquals("A Found Item can only become MATCHED through Confirm Match", ex.getMessage());
    }

    @Test
    void allowsLinkedMatchThenReturn() {
        when(authService.requireUser(session)).thenReturn(staff);
        when(repository.findById(20L)).thenReturn(java.util.Optional.of(item));
        when(repository.save(any(FoundItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LostReport linked = new LostReport();
        linked.setId(30L);
        linked.setMatchedFoundItem(item);
        when(lostReportRepository.findByMatchedFoundItemId(20L)).thenReturn(java.util.Optional.of(linked));

        item.setStatus(FoundStatus.MATCHED);
        assertEquals("RETURNED", service.updateStatus(20L, "RETURNED", session).status());
        assertEquals(FoundStatus.RETURNED, item.getStatus());
        verify(repository).save(item);
    }

    @Test
    void returningLinkedFoundItemMarksLostReportReturned() {
        when(authService.requireUser(session)).thenReturn(staff);
        item.setStatus(FoundStatus.MATCHED);
        when(repository.findById(20L)).thenReturn(java.util.Optional.of(item));
        when(repository.save(any(FoundItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LostReport lost = new LostReport();
        lost.setStatus(LostStatus.MATCHED);
        when(lostReportRepository.findByMatchedFoundItemId(20L)).thenReturn(java.util.Optional.of(lost));
        when(lostReportRepository.save(any(LostReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(20L, "RETURNED", session);

        assertEquals(LostStatus.RETURNED, lost.getStatus());
        verify(lostReportRepository).save(lost);
    }

    @Test
    void rejectsStatusChangeByAnotherStaffMember() {
        User other = new User("other", "hash", UserRole.STAFF, "Other", "other@test.local");
        other.setId(99L);
        when(authService.requireUser(session)).thenReturn(other);
        when(repository.findById(20L)).thenReturn(java.util.Optional.of(item));

        ForbiddenException exception = assertThrows(ForbiddenException.class,
            () -> service.updateStatus(20L, "MATCHED", session));
        assertEquals("Only the reporting staff member or an admin can change this found item",
            exception.getMessage());
    }
}
