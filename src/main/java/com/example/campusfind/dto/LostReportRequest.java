package com.example.campusfind.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LostReportRequest(
        @NotNull(message = "Category is required") @Positive(message = "Category ID must be positive") Long categoryId,
        @NotBlank(message = "Item name is required") @Size(max = 150, message = "Item name must not exceed 150 characters") String itemName,
        @NotBlank(message = "Description is required") @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
        @NotBlank(message = "Location is required") @Size(max = 200, message = "Location must not exceed 200 characters") String location,
        @NotNull(message = "Date lost is required") @PastOrPresent(message = "Date lost cannot be in the future") LocalDate dateLost
) {}
