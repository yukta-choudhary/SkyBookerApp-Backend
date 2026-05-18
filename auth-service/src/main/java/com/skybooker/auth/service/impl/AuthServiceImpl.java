package com.skybooker.auth.service.impl;

import com.skybooker.auth.config.AppProperties;
import com.skybooker.auth.dto.*;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.enums.AuthProvider;
import com.skybooker.auth.enums.Role;
import com.skybooker.auth.exception.BadRequestException;
import com.skybooker.auth.exception.ResourceNotFoundException;
import com.skybooker.auth.exception.UnauthorizedException;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.service.AuthService;
import com.skybooker.auth.service.EmailService;
import com.skybooker.auth.service.JwtService;
import com.skybooker.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String OTP_PREFIX = "otp:";
    private static final String RESET_PREFIX = "reset:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AppProperties appProperties;
    private final StringRedisTemplate redisTemplate;

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Starting registration for email={}", request.getEmail());

        validateRegistration(request);

        if (request.getRole() == Role.ADMIN) {
            log.warn("Attempted admin self-registration for email={}", request.getEmail());
            throw new BadRequestException("Admin self-registration is not allowed");
        }

        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .fullName(request.getFullName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // BCrypt encryption
                .phone(request.getPhone().trim())
                .role(request.getRole())
                .provider(AuthProvider.LOCAL)
                .active(true)
                .passportNumber(blankToNull(request.getPassportNumber()))
                .nationality(blankToNull(request.getNationality()))
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);

        log.info("Registration successful for userId={} email={}", saved.getUserId(), saved.getEmail());

        // Do NOT auto-login after register.
        // Frontend should redirect to login page using redirectUrl.
        return AuthResponse.builder()
                .userId(saved.getUserId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .provider(saved.getProvider())
                .message("Registration successful. Please login to continue.")
                .redirectUrl("/login")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Login attempt for email={}", email);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            log.warn("Inactive account login attempt for email={}", email);
            throw new UnauthorizedException("Account is inactive");
        }

        log.info("Login successful for userId={} email={}", user.getUserId(), user.getEmail());
        return buildLoginResponse(user);
    }

    @Override
    public MessageResponse logout(String token) {
        log.info("Logout request received");
        tokenBlacklistService.blacklist(token);
        log.info("Token blacklisted successfully");
        return new MessageResponse("Logged out successfully");
    }

    @Override
    public MessageResponse validateToken(String token) {
        boolean valid = jwtService.isTokenValid(token, "ACCESS") && !tokenBlacklistService.isBlacklisted(token);
        log.debug("Token validation result={}", valid);
        return new MessageResponse(valid ? "Token is valid" : "Token is invalid");
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refresh token request received");

        if (!jwtService.isTokenValid(refreshToken, "REFRESH") || tokenBlacklistService.isBlacklisted(refreshToken)) {
            log.warn("Invalid refresh token used");
            throw new UnauthorizedException("Invalid refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getUserId(), "ACCESS");
        String newRefreshToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getUserId(), "REFRESH");

        log.info("Refresh token generated successfully for email={}", email);

        return AuthResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getAccessTokenExpirationMs() / 1000)
                .message("Token refreshed successfully")
                .redirectUrl("/dashboard")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        log.info("Fetching profile for email={}", email);
        return mapProfile(fetchActiveUserByEmail(email));
    }

    @Override
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        log.info("Updating profile for email={}", email);
        User user = fetchActiveUserByEmail(email);

        if (user.getPhone() != null && !user.getPhone().equals(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone already exists");
        }

        String passportNumber = blankToNull(request.getPassportNumber());
        if (passportNumber != null
                && !passportNumber.equals(user.getPassportNumber())
                && userRepository.existsByPassportNumber(passportNumber)) {
            throw new BadRequestException("Passport number already exists");
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPassportNumber(passportNumber);
        user.setNationality(blankToNull(request.getNationality()));

        log.info("Profile updated successfully for email={}", email);
        return mapProfile(userRepository.save(user));
    }

    @Override
    public ProfileResponse patchProfile(String email, UpdateProfileRequest request) {
        log.info("Patching profile for email={}", email);
        User user = fetchActiveUserByEmail(email);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String phone = request.getPhone().trim();
            if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
                throw new BadRequestException("Phone already exists");
            }
            user.setPhone(phone);
        }

        if (request.getPassportNumber() != null) {
            String passportNumber = blankToNull(request.getPassportNumber());
            if (passportNumber != null
                    && !passportNumber.equals(user.getPassportNumber())
                    && userRepository.existsByPassportNumber(passportNumber)) {
                throw new BadRequestException("Passport number already exists");
            }
            user.setPassportNumber(passportNumber);
        }

        if (request.getNationality() != null) {
            user.setNationality(blankToNull(request.getNationality()));
        }

        log.info("Profile patched successfully for email={}", email);
        return mapProfile(userRepository.save(user));
    }

    @Override
    public MessageResponse changePassword(String email, ChangePasswordRequest request) {
        log.info("Change password request for email={}", email);
        User user = fetchActiveUserByEmail(email);

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password change is not supported for OAuth users");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            log.warn("Incorrect old password provided for email={}", email);
            throw new BadRequestException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword())); // BCrypt encryption
        userRepository.save(user);

        log.info("Password changed successfully for email={}", email);
        return new MessageResponse("Password updated successfully");
    }

    @Override
    public MessageResponse deactivateAccount(String email) {
        log.warn("Deactivate account request for email={}", email);
        User user = fetchActiveUserByEmail(email);
        user.setActive(false);
        userRepository.save(user);
        log.warn("Account deactivated for email={}", email);
        return new MessageResponse("Account deactivated successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream().map(this::mapUserSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUsersByRole(String role) {
        log.info("Fetching users by role={}", role);
        Role parsedRole = Role.valueOf(role.toUpperCase());
        return userRepository.findAllByRole(parsedRole).stream().map(this::mapUserSummary).toList();
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password (OTP) request for email={}", request.getEmail());

        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {
            if (user.getProvider() == AuthProvider.LOCAL) {
                // Generate 6-digit OTP and store in Redis with TTL
                String otp = String.format("%06d", new Random().nextInt(999999));
                long expirationMinutes = appProperties.getPasswordReset().getExpirationMinutes();

                redisTemplate.opsForValue().set(
                        OTP_PREFIX + user.getEmail().toLowerCase(), otp,
                        Duration.ofMinutes(expirationMinutes)
                );

                log.info("OTP stored in Redis for email={}", user.getEmail());
                emailService.sendPasswordResetOtp(user.getEmail(), user.getFullName(), otp,
                        expirationMinutes);
            }
        });

        return new MessageResponse("If the email exists, an OTP has been sent");
    }

    @Override
    public AuthResponse verifyOtp(String email, String otp) {
        log.info("Verify OTP request for email={}", email);

        String redisKey = OTP_PREFIX + email.toLowerCase();
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        if (!storedOtp.equals(otp)) {
            throw new BadRequestException("Invalid OTP for this email");
        }

        // OTP is valid — delete it and generate a one-time reset token
        redisTemplate.delete(redisKey);

        String resetUuid = UUID.randomUUID().toString();
        long expirationMinutes = appProperties.getPasswordReset().getExpirationMinutes();
        redisTemplate.opsForValue().set(
                RESET_PREFIX + resetUuid, email.toLowerCase(),
                Duration.ofMinutes(expirationMinutes)
        );

        log.info("OTP verified for email={}, reset token stored in Redis", email);
        return AuthResponse.builder()
                .message("OTP verified successfully")
                .accessToken(resetUuid)  // Reuse accessToken field to carry the reset token
                .build();
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request received");

        String redisKey = RESET_PREFIX + request.getToken();
        String email = redisTemplate.opsForValue().get(redisKey);

        if (email == null) {
            throw new BadRequestException("Invalid or used password reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password reset is not supported for OAuth users");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword())); // BCrypt encryption
        userRepository.save(user);

        // Delete the reset token from Redis (one-time use)
        redisTemplate.delete(redisKey);

        log.info("Password reset successful for email={}", user.getEmail());
        return new MessageResponse("Password reset successful");
    }

    private void validateRegistration(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone().trim())) {
            throw new BadRequestException("Phone already exists");
        }
        String passportNumber = blankToNull(request.getPassportNumber());
        if (passportNumber != null && userRepository.existsByPassportNumber(passportNumber)) {
            throw new BadRequestException("Passport number already exists");
        }
    }

    private AuthResponse buildLoginResponse(User user) {
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getUserId(), "ACCESS");
        String refreshToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getUserId(), "REFRESH");

        return AuthResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getAccessTokenExpirationMs() / 1000)
                .message("Login successful")
                .redirectUrl("/dashboard")
                .build();
    }

    private User fetchActiveUserByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("User account is inactive");
        }

        return user;
    }

    private ProfileResponse mapProfile(User user) {
        return ProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .provider(user.getProvider())
                .active(user.getActive())
                .passportNumber(user.getPassportNumber())
                .nationality(user.getNationality())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserSummaryResponse mapUserSummary(User user) {
        return UserSummaryResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .provider(user.getProvider())
                .active(user.getActive())
                .passportNumber(user.getPassportNumber())
                .nationality(user.getNationality())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}