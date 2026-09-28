package com.example.campusfind.dto;

import jakarta.validation.constraints.NotBlank;

public record CaseResolutionRequest(
        @NotBlank(message = "Case resolution is required") String resolution
) {}