package com.example.campusfind.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.campusfind.dto.AuthResponse;
import com.example.campusfind.dto.LoginRequest;
import com.example.campusfind.dto.RegisterRequest;
import com.example.campusfind.dto.UserResponse;
import com.example.campusfind.entity.User;
import com.example.campusfind.entity.UserRole;
import com.example.campusfind.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class AuthService {

    public static final String SESSION_USER_ID = "CAMPUSFIND_USER_ID";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request, HttpSession session) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username is already registered");
        }

        User user = new User(
                username,
                passwordEncoder.encode(request.password()),
                UserRole.STUDENT,
                request.fullName().trim(),
                normalizeEmail(request.email()));

        user = userRepository.save(user);
        session.setAttribute(SESSION_USER_ID, user.getId());
        return toAuthResponse(user, "Registration successful");
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        Optional<User> optionalUser = userRepository.findByUsernameIgnoreCase(request.username().trim());
        if (optionalUser.isEmpty() || !passwordEncoder.matches(request.password(), optionalUser.get().getPasswordHash())) {
            throw new ForbiddenException("Invalid username or password");
        }

        User user = optionalUser.get();
        session.setAttribute(SESSION_USER_ID, user.getId());
        return toAuthResponse(user, "Login successful");
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public Optional<User> findCurrentUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long userId)) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    public User requireUser(HttpSession session) {
        return findCurrentUser(session)
                .orElseThrow(() -> new ForbiddenException("Please login to continue"));
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole().name());
    }

    private AuthResponse toAuthResponse(User user, String message) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole().name(), message);
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }
}
