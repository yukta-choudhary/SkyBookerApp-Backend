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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Covers registration, login, logout, profile management, and password flows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private AppProperties appProperties;
    @Mock private AppProperties.Jwt jwtProps;
    @Mock private AppProperties.PasswordReset passwordResetProps;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .fullName("Test User")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .phone("9876543210")
                .role(Role.PASSENGER)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("SecurePass123");
        registerRequest.setPhone("9876543210");
        registerRequest.setRole(Role.PASSENGER);
    }

    // ===== REGISTRATION TESTS =====

    @Test
    @DisplayName("Register: should register new passenger successfully")
    void register_shouldSucceed_whenValidRequest() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo(Role.PASSENGER);
        assertThat(response.getMessage()).contains("successful");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register: should throw BadRequestException when email already exists")
    void register_shouldThrow_whenEmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register: should throw BadRequestException when phone already exists")
    void register_shouldThrow_whenPhoneExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Phone already exists");
    }

    @Test
    @DisplayName("Register: should reject admin self-registration")
    void register_shouldThrow_whenRoleIsAdmin() {
        registerRequest.setRole(Role.ADMIN);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Admin self-registration");
    }

    @Test
    @DisplayName("Register: should register airline staff successfully")
    void register_shouldSucceed_forAirlineStaff() {
        registerRequest.setRole(Role.AIRLINE_STAFF);
        User staffUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .email("staff@airline.com")
                .role(Role.AIRLINE_STAFF)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(staffUser);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getRole()).isEqualTo(Role.AIRLINE_STAFF);
    }

    // ===== LOGIN TESTS =====

    @Test
    @DisplayName("Login: should return auth tokens on valid credentials")
    void login_shouldReturnTokens_whenCredentialsValid() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("SecurePass123");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("test@example.com", "SecurePass123"));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(anyString(), anyString(), anyString(), eq("ACCESS"))).thenReturn("access-token");
        when(jwtService.generateToken(anyString(), anyString(), anyString(), eq("REFRESH"))).thenReturn("refresh-token");
        when(appProperties.getJwt()).thenReturn(jwtProps);
        when(jwtProps.getAccessTokenExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Login: should throw UnauthorizedException for inactive account")
    void login_shouldThrow_whenAccountInactive() {
        testUser.setActive(false);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("SecurePass123");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("test@example.com", "SecurePass123"));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("Login: should throw when authentication fails (wrong password)")
    void login_shouldThrow_whenBadCredentials() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ===== LOGOUT TEST =====

    @Test
    @DisplayName("Logout: should blacklist token successfully")
    void logout_shouldBlacklistToken() {
        doNothing().when(tokenBlacklistService).blacklist(anyString());

        MessageResponse response = authService.logout("valid-jwt-token");

        assertThat(response.getMessage()).contains("Logged out");
        verify(tokenBlacklistService).blacklist("valid-jwt-token");
    }

    // ===== PROFILE TESTS =====

    @Test
    @DisplayName("GetProfile: should return profile for active user")
    void getProfile_shouldReturnProfile_forActiveUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        ProfileResponse profile = authService.getProfile("test@example.com");

        assertThat(profile.getEmail()).isEqualTo("test@example.com");
        assertThat(profile.getFullName()).isEqualTo("Test User");
        assertThat(profile.getRole()).isEqualTo(Role.PASSENGER);
    }

    @Test
    @DisplayName("GetProfile: should throw ResourceNotFoundException for unknown email")
    void getProfile_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getProfile("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== CHANGE PASSWORD TESTS =====

    @Test
    @DisplayName("ChangePassword: should change password for local user")
    void changePassword_shouldSucceed_forLocalUser() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("OldPass123");
        req.setNewPassword("NewPass456");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newhashedpassword");
        when(userRepository.save(any())).thenReturn(testUser);

        MessageResponse response = authService.changePassword("test@example.com", req);

        assertThat(response.getMessage()).contains("updated");
        verify(passwordEncoder).encode("NewPass456");
    }

    @Test
    @DisplayName("ChangePassword: should throw if old password is wrong")
    void changePassword_shouldThrow_whenOldPasswordIncorrect() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("WrongOldPass");
        req.setNewPassword("NewPass456");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("test@example.com", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    @DisplayName("ChangePassword: should throw for OAuth users")
    void changePassword_shouldThrow_forOAuthUser() {
        testUser.setProvider(AuthProvider.GOOGLE);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("any");
        req.setNewPassword("any");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.changePassword("test@example.com", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OAuth");
    }

    // ===== GET ALL USERS TEST =====

    @Test
    @DisplayName("GetAllUsers: should return list of user summaries")
    void getAllUsers_shouldReturnUserList() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserSummaryResponse> users = authService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("test@example.com");
    }

    // ===== DEACTIVATE ACCOUNT TEST =====

    @Test
    @DisplayName("DeactivateAccount: should deactivate active user")
    void deactivateAccount_shouldDeactivate() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        MessageResponse response = authService.deactivateAccount("test@example.com");

        assertThat(testUser.getActive()).isFalse();
        assertThat(response.getMessage()).contains("deactivated");
    }

    // ===== FORGOT PASSWORD TEST =====

    @Test
    @DisplayName("ForgotPassword: should send reset email for local user")
    void forgotPassword_shouldSendEmail_forLocalUser() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("test@example.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.save(any())).thenReturn(null);
        when(appProperties.getPasswordReset()).thenReturn(passwordResetProps);
        when(passwordResetProps.getExpirationMinutes()).thenReturn(30L);
        when(appProperties.getFrontendBaseUrl()).thenReturn("http://localhost:4200");
        doNothing().when(passwordResetTokenRepository).deleteByUser(any());

        MessageResponse response = authService.forgotPassword(req);

        assertThat(response.getMessage()).contains("password reset link");
        verify(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    // ===== RESET PASSWORD TESTS =====

    @Test
    @DisplayName("ResetPassword: should reset password for valid token")
    void resetPassword_shouldSucceed_forValidToken() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("valid-reset-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("valid-reset-token");
        req.setNewPassword("NewSecurePass789");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(anyString())).thenReturn("newhashedpassword");
        when(userRepository.save(any())).thenReturn(testUser);
        when(passwordResetTokenRepository.save(any())).thenReturn(resetToken);

        MessageResponse response = authService.resetPassword(req);

        assertThat(response.getMessage()).contains("successful");
        assertThat(resetToken.isUsed()).isTrue();
    }

    @Test
    @DisplayName("ResetPassword: should throw for expired token")
    void resetPassword_shouldThrow_forExpiredToken() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .token("expired-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // already expired
                .used(false)
                .build();

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("expired-token");
        req.setNewPassword("NewPass");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("ResetPassword: should throw for invalid/used token")
    void resetPassword_shouldThrow_forInvalidToken() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("invalid-token");
        req.setNewPassword("NewPass");

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BadRequestException.class);
    }

    // ===== VALIDATE TOKEN TEST =====

    @Test
    @DisplayName("ValidateToken: should return valid message for non-blacklisted valid token")
    void validateToken_shouldReturnValid_forGoodToken() {
        when(jwtService.isTokenValid(anyString(), anyString())).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);

        MessageResponse response = authService.validateToken("good-token");

        assertThat(response.getMessage()).contains("valid");
    }
}
