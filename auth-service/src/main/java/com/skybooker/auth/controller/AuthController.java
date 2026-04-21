package com.skybooker.auth.controller;

import com.skybooker.auth.dto.*;
import com.skybooker.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, JWT management, profile, and password reset APIs")
public class AuthController {

    private final AuthService authService;

    // ===== Public Endpoints =====

    @Operation(summary = "Register User", description = "Register a new PASSENGER or AIRLINE_STAFF account. Admin self-registration is blocked.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Email/phone already exists or validation failed")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Login", description = "Authenticate with email and password. Returns JWT access and refresh tokens.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — returns access + refresh tokens"),
        @ApiResponse(responseCode = "401", description = "Invalid email or password"),
        @ApiResponse(responseCode = "403", description = "Account inactive")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Logout", description = "Blacklists the current JWT token, invalidating the user's session.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return ResponseEntity.ok(authService.logout(token));
    }

    @Operation(summary = "Validate Token", description = "Check if a given JWT access token is valid and not blacklisted.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Returns 'Token is valid' or 'Token is invalid'")
    })
    @GetMapping("/validate")
    public ResponseEntity<MessageResponse> validateToken(@RequestParam String token) {
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @Operation(summary = "Refresh Token", description = "Generate a new access token using a valid refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New tokens issued"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    // ===== Authenticated User Profile =====

    @Operation(summary = "Get My Profile", description = "Returns the authenticated user's full profile.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getProfile(userDetails.getUsername()));
    }

    @Operation(summary = "Update My Profile (Full)", description = "Replace all updatable profile fields (fullName, phone, passportNumber, nationality).")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userDetails.getUsername(), request));
    }

    @Operation(summary = "Update My Profile (Partial)", description = "Update only the provided profile fields — omitted fields are left unchanged.")
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/me")
    public ResponseEntity<ProfileResponse> patchProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.patchProfile(userDetails.getUsername(), request));
    }

    @Operation(summary = "Change Password", description = "Change password for LOCAL (non-OAuth) accounts. Requires the old password for verification.")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password updated"),
        @ApiResponse(responseCode = "400", description = "Old password incorrect or OAuth user")
    })
    @PutMapping("/me/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(userDetails.getUsername(), request));
    }

    @Operation(summary = "Deactivate Account", description = "Soft-delete the authenticated user's account (sets active=false).")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deactivateAccount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.deactivateAccount(userDetails.getUsername()));
    }

    // ===== Password Reset (Public) =====

    @Operation(
        summary = "Forgot Password",
        description = "Sends a password reset link to the email if it exists. Always returns 200 to prevent email enumeration."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @Operation(summary = "Verify OTP", description = "Verify the 6-digit OTP sent to the user's email. Returns a reset token for the final password reset step.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP verified — returns reset token"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request.getEmail(), request.getOtp()));
    }

    @Operation(summary = "Reset Password", description = "Reset password using a valid, unexpired reset token from the OTP verification step.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successful"),
        @ApiResponse(responseCode = "400", description = "Invalid, used, or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    // ===== Admin Endpoints =====

    @Operation(summary = "Get All Users (Admin)", description = "Returns a summary list of all registered users. Requires ADMIN role.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @Operation(summary = "Get Users by Role (Admin)", description = "Filter users by role: PASSENGER, AIRLINE_STAFF, or ADMIN. Requires ADMIN role.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/admin/users/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }
}