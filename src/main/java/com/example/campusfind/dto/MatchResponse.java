package com.example.campusfind.dto;

import java.time.LocalDate;

public record MatchResponse(
        Long lostReportId,
        Long foundItemId,
        String lostItemName,
        String foundItemName,
        String category,
        String lostLocation,
        String foundLocation,
        int matchScore,
        String matchReason,
        LocalDate lostDate,
        LocalDate foundDate,
        String foundPhoneNumber,
        String lostStatus,
        String foundStatus,
        String caseState
) {}
