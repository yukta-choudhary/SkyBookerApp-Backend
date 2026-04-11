package com.skybooker.auth.service;

import com.skybooker.auth.dto.*;

import java.util.List;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    MessageResponse logout(String token);
    MessageResponse validateToken(String token);
    AuthResponse refreshToken(String refreshToken);
    ProfileResponse getProfile(String email);
    ProfileResponse updateProfile(String email, UpdateProfileRequest request);
    ProfileResponse patchProfile(String email, UpdateProfileRequest request);
    MessageResponse changePassword(String email, ChangePasswordRequest request);
    MessageResponse deactivateAccount(String email);
    List<UserSummaryResponse> getAllUsers();
    List<UserSummaryResponse> getUsersByRole(String role);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
}
