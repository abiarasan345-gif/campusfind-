package com.example.campusfind.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LostReportResponse(
        Long id, Long reportingUserId, String reportingUserName, Long categoryId, String categoryName,
        String itemName, String description, String location, LocalDate dateLost, String status, String caseState,
        Long matchedFoundItemId, LocalDateTime createdAt
) {}
