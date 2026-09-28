package com.example.campusfind.service;

import org.springframework.stereotype.Service;

import com.example.campusfind.dto.DashboardResponse;
import com.example.campusfind.entity.FoundStatus;
import com.example.campusfind.entity.LostStatus;
import com.example.campusfind.entity.User;
import com.example.campusfind.entity.UserRole;
import com.example.campusfind.repository.FoundItemRepository;
import com.example.campusfind.repository.LostReportRepository;
import com.example.campusfind.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final LostReportRepository lostReportRepository;
    private final FoundItemRepository foundItemRepository;
    private final AuthService authService;

    public AdminService(UserRepository userRepository,
                        LostReportRepository lostReportRepository,
                        FoundItemRepository foundItemRepository,
                        AuthService authService) {
        this.userRepository = userRepository;
        this.lostReportRepository = lostReportRepository;
        this.foundItemRepository = foundItemRepository;
        this.authService = authService;
    }

    public DashboardResponse dashboard(HttpSession session) {
        User user = authService.requireUser(session);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only an admin can view the admin dashboard");
        }

        return new DashboardResponse(
                userRepository.count(),
                lostReportRepository.count(),
                foundItemRepository.count(),
                lostReportRepository.countByStatus(LostStatus.OPEN),
                foundItemRepository.countByStatus(FoundStatus.AVAILABLE),
                lostReportRepository.countByStatus(LostStatus.MATCHED),
                foundItemRepository.countByStatus(FoundStatus.MATCHED),
                foundItemRepository.countByStatus(FoundStatus.RETURNED),
                lostReportRepository.countByStatus(LostStatus.RETURNED));
    }
}
