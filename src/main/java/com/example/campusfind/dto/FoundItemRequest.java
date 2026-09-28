package com.example.campusfind.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FoundItemRequest(
        @NotNull(message = "Category is required") @Positive(message = "Category ID must be positive") Long categoryId,
        @NotBlank(message = "Item name is required") @Size(max = 150, message = "Item name must not exceed 150 characters") String itemName,
        @NotBlank(message = "Description is required") @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
        @NotBlank(message = "Location is required") @Size(max = 200, message = "Location must not exceed 200 characters") String location,
        @NotNull(message = "Date found is required") @PastOrPresent(message = "Date found cannot be in the future") LocalDate dateFound,
        @NotBlank(message = "Finder phone number is required")
        @Size(max = 30, message = "Finder phone number must not exceed 30 characters")
        @Pattern(regexp = "^[0-9+()\\- .]{7,30}$", message = "Finder phone number is invalid") String finderPhoneNumber
) {}
