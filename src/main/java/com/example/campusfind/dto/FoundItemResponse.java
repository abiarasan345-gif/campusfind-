package com.example.campusfind.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FoundItemResponse(
        Long id, Long reportingStaffId, String reportingStaffName, Long categoryId, String categoryName,
        String itemName, String description, String location, LocalDate dateFound, String finderPhoneNumber,
        String status, String caseState, LocalDateTime createdAt
) {}
