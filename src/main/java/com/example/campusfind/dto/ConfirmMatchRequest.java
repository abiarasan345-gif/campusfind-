package com.example.campusfind.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfirmMatchRequest(
        @NotNull @Positive Long lostReportId,
        @NotNull @Positive Long foundItemId
) {}
