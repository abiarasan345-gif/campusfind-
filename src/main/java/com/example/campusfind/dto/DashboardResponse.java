package com.example.campusfind.dto;

public record DashboardResponse(
        long totalUsers,
        long totalLostReports,
        long totalFoundItems,
        long openLostReports,
        long availableFoundItems,
        long matchedLostReports,
        long claimedFoundItems,
        long returnedFoundItems,
        long returnedLostReports
) {}
