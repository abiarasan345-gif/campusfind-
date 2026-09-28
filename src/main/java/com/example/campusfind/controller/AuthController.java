package com.example.campusfind.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.campusfind.dto.AuthResponse;
import com.example.campusfind.dto.LoginRequest;
import com.example.campusfind.dto.RegisterRequest;
import com.example.campusfind.dto.UserResponse;
import com.example.campusfind.service.AuthService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, session));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return authService.login(request, session);
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpSession session) {
        authService.logout(session);
        return new AuthResponse(null, null, null, null, "Logged out successfully");
    }

    @GetMapping("/me")
    public UserResponse me(HttpSession session) {
        return authService.toUserResponse(authService.requireUser(session));
    }
}
