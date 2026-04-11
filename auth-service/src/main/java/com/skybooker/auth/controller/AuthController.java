package com.skybooker.auth.controller;

import com.skybooker.auth.dto.*;
import com.skybooker.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email={}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email={}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("Logout request received");
        return ResponseEntity.ok(authService.logout(extractToken(authHeader)));
    }

    @GetMapping("/validate")
    public ResponseEntity<MessageResponse> validate(@RequestParam String token) {
        log.debug("Token validation request received");
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request received");
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        log.info("Profile fetch request for user={}", authentication.getName());
        return ResponseEntity.ok(authService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(Authentication authentication,
                                                         @Valid @RequestBody UpdateProfileRequest request) {
        log.info("Profile update request for user={}", authentication.getName());
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ProfileResponse> patchProfile(Authentication authentication,
                                                        @RequestBody UpdateProfileRequest request) {
        log.info("Profile patch request for user={}", authentication.getName());
        return ResponseEntity.ok(authService.patchProfile(authentication.getName(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(Authentication authentication,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Password change request for user={}", authentication.getName());
        return ResponseEntity.ok(authService.changePassword(authentication.getName(), request));
    }

    @PutMapping("/deactivate")
    public ResponseEntity<MessageResponse> deactivate(Authentication authentication) {
        log.warn("Deactivate account request for user={}", authentication.getName());
        return ResponseEntity.ok(authService.deactivateAccount(authentication.getName()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request received for email={}", request.getEmail());
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received");
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    /**
     * Protected dashboard endpoint.
     * This will only work after successful login with valid Bearer token.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<MessageResponse> dashboard(Authentication authentication) {
        log.info("Dashboard accessed by user={}", authentication.getName());
        return ResponseEntity.ok(
                new MessageResponse("Welcome to the dashboard, " + authentication.getName())
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers(@RequestParam(required = false) String role) {
        log.info("Admin user list request received. role filter={}", role);
        if (role == null || role.isBlank()) {
            return ResponseEntity.ok(authService.getAllUsers());
        }
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with Bearer");
        }
        return authHeader.substring(7);
    }
}