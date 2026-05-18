package com.skybooker.auth;

import com.skybooker.auth.config.AppProperties;
import com.skybooker.auth.dto.*;
import com.skybooker.auth.entity.PasswordResetToken;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.enums.AuthProvider;
import com.skybooker.auth.enums.Role;
import com.skybooker.auth.exception.BadRequestException;
import com.skybooker.auth.exception.ResourceNotFoundException;
import com.skybooker.auth.exception.UnauthorizedException;
import com.skybooker.auth.repository.PasswordResetTokenRepository;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.service.EmailService;
import com.skybooker.auth.service.JwtService;
import com.skybooker.auth.service.TokenBlacklistService;
import com.skybooker.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private AppProperties appProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private AppProperties.Jwt jwtProps;
    private AppProperties.PasswordReset resetProps;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("user-123")
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .phone("1234567890")
                .role(Role.PASSENGER)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        jwtProps = new AppProperties.Jwt();
        jwtProps.setSecret("testSecretKey12345678901234567890");
        jwtProps.setAccessTokenExpirationMs(3600000);
        jwtProps.setRefreshTokenExpirationMs(86400000);

        resetProps = new AppProperties.PasswordReset();
        resetProps.setExpirationMinutes(15);
    }

    // ─── Register ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("Should register a new passenger successfully")
        void register_success() {
            RegisterRequest request = new RegisterRequest();
            request.setFullName("John Doe");
            request.setEmail("john@example.com");
            request.setPassword("password123");
            request.setPhone("1234567890");
            request.setRole(Role.PASSENGER);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getFullName()).isEqualTo("John Doe");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getRole()).isEqualTo(Role.PASSENGER);
            assertThat(response.getMessage()).contains("Registration successful");
            assertThat(response.getRedirectUrl()).isEqualTo("/login");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_duplicateEmail() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setPhone("1234567890");
            request.setRole(Role.PASSENGER);

            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email already exists");
        }

        @Test
        @DisplayName("Should throw exception when phone already exists")
        void register_duplicatePhone() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPhone("1234567890");
            request.setRole(Role.PASSENGER);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Phone already exists");
        }

        @Test
        @DisplayName("Should throw exception when admin self-registration attempted")
        void register_adminNotAllowed() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("admin@example.com");
            request.setPhone("1234567890");
            request.setRole(Role.ADMIN);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Admin self-registration is not allowed");
        }

        @Test
        @DisplayName("Should throw exception when passport number already exists")
        void register_duplicatePassport() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPhone("9999999999");
            request.setPassportNumber("AB123456");
            request.setRole(Role.PASSENGER);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(userRepository.existsByPassportNumber("AB123456")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Passport number already exists");
        }
    }

    // ─── Login ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_success() {
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(anyString(), anyString(), anyString(), eq("ACCESS")))
                    .thenReturn("access-token");
            when(jwtService.generateToken(anyString(), anyString(), anyString(), eq("REFRESH")))
                    .thenReturn("refresh-token");
            when(appProperties.getJwt()).thenReturn(jwtProps);

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getMessage()).isEqualTo("Login successful");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found during login")
        void login_userNotFound() {
            LoginRequest request = new LoginRequest();
            request.setEmail("unknown@example.com");
            request.setPassword("password123");

            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when account is inactive")
        void login_inactiveAccount() {
            testUser.setActive(false);

            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Account is inactive");
        }
    }

    // ─── Logout ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should logout successfully by blacklisting token")
    void logout_success() {
        MessageResponse response = authService.logout("some-token");

        assertThat(response.getMessage()).isEqualTo("Logged out successfully");
        verify(tokenBlacklistService).blacklist("some-token");
    }

    // ─── Validate Token ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should return valid message for a valid token")
    void validateToken_valid() {
        when(jwtService.isTokenValid("valid-token", "ACCESS")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);

        MessageResponse response = authService.validateToken("valid-token");

        assertThat(response.getMessage()).isEqualTo("Token is valid");
    }

    @Test
    @DisplayName("Should return invalid message for blacklisted token")
    void validateToken_blacklisted() {
        when(jwtService.isTokenValid("token", "ACCESS")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("token")).thenReturn(true);

        MessageResponse response = authService.validateToken("token");

        assertThat(response.getMessage()).isEqualTo("Token is invalid");
    }

    // ─── Refresh Token ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void refreshToken_success() {
            when(jwtService.isTokenValid("refresh-token", "REFRESH")).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted("refresh-token")).thenReturn(false);
            when(jwtService.extractUsername("refresh-token")).thenReturn("john@example.com");
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(anyString(), anyString(), anyString(), eq("ACCESS")))
                    .thenReturn("new-access-token");
            when(jwtService.generateToken(anyString(), anyString(), anyString(), eq("REFRESH")))
                    .thenReturn("new-refresh-token");
            when(appProperties.getJwt()).thenReturn(jwtProps);

            AuthResponse response = authService.refreshToken("refresh-token");

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.getMessage()).isEqualTo("Token refreshed successfully");
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void refreshToken_invalid() {
            when(jwtService.isTokenValid("bad-token", "REFRESH")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid refresh token");
        }
    }

    // ─── Profile ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Profile")
    class ProfileTests {

        @Test
        @DisplayName("Should get profile successfully")
        void getProfile_success() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            ProfileResponse response = authService.getProfile("john@example.com");

            assertThat(response.getUserId()).isEqualTo("user-123");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should throw exception when user not found for profile")
        void getProfile_notFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getProfile("unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should update profile successfully")
        void updateProfile_success() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("John Updated");
            request.setPhone("1234567890");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileResponse response = authService.updateProfile("john@example.com", request);

            assertThat(response.getFullName()).isEqualTo("John Updated");
        }

        @Test
        @DisplayName("Should throw when updating profile with duplicate phone")
        void updateProfile_duplicatePhone() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("John Updated");
            request.setPhone("9999999999");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.existsByPhone("9999999999")).thenReturn(true);

            assertThatThrownBy(() -> authService.updateProfile("john@example.com", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Phone already exists");
        }

        @Test
        @DisplayName("Should patch profile with partial fields")
        void patchProfile_partialUpdate() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("Patched Name");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileResponse response = authService.patchProfile("john@example.com", request);

            assertThat(response.getFullName()).isEqualTo("Patched Name");
        }
    }

    // ─── Change Password ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Change Password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void changePassword_success() {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("oldPass");
            request.setNewPassword("newPass123");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPass", "hashedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPass123")).thenReturn("newHashedPassword");

            MessageResponse response = authService.changePassword("john@example.com", request);

            assertThat(response.getMessage()).isEqualTo("Password updated successfully");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw when old password is incorrect")
        void changePassword_wrongOldPassword() {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("wrongOldPass");
            request.setNewPassword("newPass123");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongOldPass", "hashedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword("john@example.com", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Old password is incorrect");
        }

        @Test
        @DisplayName("Should throw when OAuth user tries to change password")
        void changePassword_oauthUser() {
            testUser.setProvider(AuthProvider.GOOGLE);
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("old");
            request.setNewPassword("new12345");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.changePassword("john@example.com", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Password change is not supported for OAuth users");
        }
    }

    // ─── Deactivate Account ──────────────────────────────────────────────

    @Test
    @DisplayName("Should deactivate account successfully")
    void deactivateAccount_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.deactivateAccount("john@example.com");

        assertThat(response.getMessage()).isEqualTo("Account deactivated successfully");
        assertThat(testUser.getActive()).isFalse();
    }

    // ─── Get All Users ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should get all users")
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserSummaryResponse> users = authService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("john@example.com");
    }

    // ─── Get Users By Role ───────────────────────────────────────────────

    @Test
    @DisplayName("Should get users by role")
    void getUsersByRole_success() {
        when(userRepository.findAllByRole(Role.PASSENGER)).thenReturn(List.of(testUser));

        List<UserSummaryResponse> users = authService.getUsersByRole("PASSENGER");

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getRole()).isEqualTo(Role.PASSENGER);
    }

    // ─── Forgot Password ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should send OTP for forgot password")
    void forgotPassword_success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("john@example.com");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(appProperties.getPasswordReset()).thenReturn(resetProps);

        MessageResponse response = authService.forgotPassword(request);

        assertThat(response.getMessage()).contains("OTP has been sent");
        verify(passwordResetTokenRepository).deleteByUser(testUser);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetOtp(eq("john@example.com"), eq("John Doe"), anyString(), eq(15L));
    }

    @Test
    @DisplayName("Should return same message even for non-existing email")
    void forgotPassword_unknownEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(request);

        assertThat(response.getMessage()).contains("OTP has been sent");
        verify(passwordResetTokenRepository, never()).save(any());
    }

    // ─── Verify OTP ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should verify OTP successfully")
    void verifyOtp_success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("123456")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsedFalse("123456")).thenReturn(Optional.of(token));

        AuthResponse response = authService.verifyOtp("john@example.com", "123456");

        assertThat(response.getMessage()).isEqualTo("OTP verified successfully");
        assertThat(response.getAccessToken()).isNotNull(); // reset UUID token
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    @DisplayName("Should throw when OTP is expired")
    void verifyOtp_expired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("123456")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsedFalse("123456")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyOtp("john@example.com", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("OTP has expired");
    }

    @Test
    @DisplayName("Should throw when OTP email mismatch")
    void verifyOtp_emailMismatch() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("123456")
                .user(testUser) // email is john@example.com
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsedFalse("123456")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyOtp("other@example.com", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid OTP for this email");
    }

    // ─── Reset Password ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should reset password successfully")
    void resetPassword_success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("reset-uuid")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-uuid");
        request.setNewPassword("newSecurePass");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse("reset-uuid")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newSecurePass")).thenReturn("newHash");

        MessageResponse response = authService.resetPassword(request);

        assertThat(response.getMessage()).isEqualTo("Password reset successful");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw when reset token expired")
    void resetPassword_expiredToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("expired-uuid")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-uuid");
        request.setNewPassword("newPass");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse("expired-uuid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Password reset token expired");
    }

    @Test
    @DisplayName("Should throw when OAuth user tries to reset password")
    void resetPassword_oauthUser() {
        testUser.setProvider(AuthProvider.GOOGLE);
        PasswordResetToken token = PasswordResetToken.builder()
                .token("reset-uuid")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-uuid");
        request.setNewPassword("newPass");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse("reset-uuid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Password reset is not supported for OAuth users");
    }
}
