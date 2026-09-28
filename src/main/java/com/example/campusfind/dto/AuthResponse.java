package com.example.campusfind.dto;

public record AuthResponse(Long userId, String username, String fullName, String role, String message) {}
