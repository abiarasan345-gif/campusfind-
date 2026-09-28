package com.example.campusfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.campusfind.dto.CaseResolutionRequest;
import com.example.campusfind.dto.ConfirmMatchRequest;
import com.example.campusfind.entity.CaseState;
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
class MatchingServiceTest {

    @Mock LostReportRepository lostRepo;
    @Mock FoundItemRepository foundRepo;
    @Mock AuthService authService;
    @Mock HttpSession session;

    private MatchingService service;

    @BeforeEach
    void setUp() {
        service = new MatchingService(lostRepo, foundRepo, authService);
    }

    @Test
    void sameCategoryAndLocationProducesStrongMatch() {
        User student = new User("student", "hash", UserRole.STUDENT, "Student", "student@test.local");
        Category charger = new Category("Charger");
        charger.setId(1L);

        LostReport lost = new LostReport();
        lost.setId(1L);
        lost.setReportingUser(student);
        lost.setCategory(charger);
        lost.setItemName("White charger");
        lost.setDescription("USB C phone charger");
        lost.setLocation("Library");
        lost.setDateLost(LocalDate.now());
        lost.setStatus(LostStatus.OPEN);

        FoundItem found = new FoundItem();
        found.setId(2L);
        found.setReportingStaff(student);
        found.setCategory(charger);
        found.setItemName("White USB C charger");
        found.setDescription("phone charger");
        found.setLocation("Library");
        found.setDateFound(LocalDate.now());
        found.setStatus(FoundStatus.AVAILABLE);

        assertTrue(service.score(lost, found) >= 80);
    }

    @Test
    void confirmsAndClosesReturnedCaseWithoutClosingOnMatch() {
        User staff = new User("staff", "hash", UserRole.STAFF, "Staff", "staff@test.local");
        staff.setId(10L);
        Category category = new Category("Bag");
        category.setId(2L);

        LostReport lost = new LostReport();
        lost.setId(11L);
        lost.setReportingUser(staff);
        lost.setCategory(category);
        lost.setItemName("ASUS Bag");
        lost.setDescription("Black colour ASUS laptop bag");
        lost.setLocation("AI Block 3rd Floor");
        lost.setDateLost(LocalDate.now());
        lost.setStatus(LostStatus.OPEN);

        FoundItem found = new FoundItem();
        found.setId(12L);
        found.setReportingStaff(staff);
        found.setCategory(category);
        found.setItemName("Black ASUS Bag");
        found.setDescription("Black colour bag");
        found.setLocation("AI Block 3rd Floor");
        found.setDateFound(LocalDate.now());
        found.setFinderPhoneNumber("+1 555 0100");
        found.setStatus(FoundStatus.AVAILABLE);

        when(authService.requireUser(session)).thenReturn(staff);
        when(lostRepo.findById(11L)).thenReturn(java.util.Optional.of(lost));
        when(foundRepo.findById(12L)).thenReturn(java.util.Optional.of(found));
        when(lostRepo.findByMatchedFoundItemId(12L)).thenReturn(java.util.Optional.empty());

        service.confirmMatch(new ConfirmMatchRequest(11L, 12L), session);

        assertEquals(LostStatus.MATCHED, lost.getStatus());
        assertEquals(FoundStatus.MATCHED, found.getStatus());
        assertEquals(CaseState.MATCHED, lost.getCaseState());
        assertEquals(CaseState.MATCHED, found.getCaseState());

        service.updateResolution(11L, new CaseResolutionRequest("RETURNED"), session);
        assertEquals(LostStatus.RETURNED, lost.getStatus());
        assertEquals(FoundStatus.RETURNED, found.getStatus());
        assertEquals(CaseState.RETURNED, lost.getCaseState());

        service.updateResolution(11L, new CaseResolutionRequest("CLOSED"), session);
        assertEquals(CaseState.CLOSED, lost.getCaseState());
        assertEquals(CaseState.CLOSED, found.getCaseState());
    }
}
