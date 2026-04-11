package com.g90.backend.modules.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g90.backend.exception.AccountLoginBlockedException;
import com.g90.backend.exception.CurrentPasswordIncorrectException;
import com.g90.backend.exception.EmailVerificationRequiredException;
import com.g90.backend.exception.EmailAlreadyExistsException;
import com.g90.backend.exception.InvalidCredentialsException;
import com.g90.backend.exception.PasswordMismatchException;
import com.g90.backend.exception.PasswordResetTokenInvalidException;
import com.g90.backend.exception.RequestValidationException;
import com.g90.backend.exception.SystemRoleNotConfiguredException;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.AuditLogEntity;
import com.g90.backend.modules.account.entity.RoleEntity;
import com.g90.backend.modules.account.entity.RoleName;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.AuditLogRepository;
import com.g90.backend.modules.account.repository.RoleRepository;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.customer.entity.CustomerStatus;
import com.g90.backend.modules.email.service.NotificationEmailService;
import com.g90.backend.modules.user.dto.ChangePasswordRequest;
import com.g90.backend.modules.user.dto.ForgotPasswordRequest;
import com.g90.backend.modules.user.dto.LoginRequest;
import com.g90.backend.modules.user.dto.LoginResponseData;
import com.g90.backend.modules.user.dto.PasswordResetTokenValidationResponseData;
import com.g90.backend.modules.user.dto.RegistrationVerificationResponseData;
import com.g90.backend.modules.user.dto.RegisterRequest;
import com.g90.backend.modules.user.dto.RegisterResponseData;
import com.g90.backend.modules.user.dto.ResendVerificationCodeRequest;
import com.g90.backend.modules.user.dto.ResendVerificationCodeResponseData;
import com.g90.backend.modules.user.dto.ResetPasswordRequest;
import com.g90.backend.modules.user.dto.UpdateProfileRequest;
import com.g90.backend.modules.user.dto.UserProfileResponse;
import com.g90.backend.modules.user.dto.VerifyRegistrationRequest;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.entity.PasswordResetTokenEntity;
import com.g90.backend.modules.user.mapper.UserManagementMapper;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.modules.user.repository.PasswordResetTokenRepository;
import com.g90.backend.security.AccessTokenService;
import com.g90.backend.security.AuthenticatedUser;
import com.g90.backend.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int PASSWORD_RESET_EXPIRE_MINUTES = 30;

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserManagementMapper userManagementMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final AccessTokenService accessTokenService;
    private final CurrentUserProvider currentUserProvider;
    private final RegistrationVerificationService registrationVerificationService;
    private final NotificationEmailService notificationEmailService;

    @Override
    @Transactional
    public RegisterResponseData register(RegisterRequest request) {
        validatePasswordConfirmation(request.getPassword(), request.getConfirmPassword(), "confirmPassword");

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userAccountRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException();
        }

        RoleEntity customerRole = roleRepository.findByNameIgnoreCase(RoleName.CUSTOMER.name())
                .orElseThrow(() -> new SystemRoleNotConfiguredException(RoleName.CUSTOMER.name()));

        UserAccountEntity user = new UserAccountEntity();
        user.setRole(customerRole);
        user.setFullName(normalize(request.getFullName()));
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(AccountStatus.PENDING_VERIFICATION.name());
        user.setEmailVerified(Boolean.FALSE);

        UserAccountEntity savedUser = userAccountRepository.save(user);

        CustomerProfileEntity customerProfile = new CustomerProfileEntity();
        customerProfile.setUser(savedUser);
        customerProfile.setContactPerson(savedUser.getFullName());
        customerProfile.setEmail(savedUser.getEmail());
        customerProfile.setCreditLimit(BigDecimal.ZERO.setScale(2));
        customerProfile.setStatus(CustomerStatus.PENDING_VERIFICATION.name());
        customerProfileRepository.save(customerProfile);

        RegistrationVerificationService.VerificationDispatch verificationDispatch =
                registrationVerificationService.issueForNewRegistration(savedUser);
        scheduleVerificationEmailDispatch(verificationDispatch);

        logAudit("REGISTER_USER", savedUser.getId(), null, userManagementMapper.toProfile(savedUser), savedUser.getId());
        return userManagementMapper.toRegisterData(savedUser, verificationDispatch.expireMinutes());
    }

    @Override
    @Transactional
    public RegistrationVerificationResponseData verifyRegistration(VerifyRegistrationRequest request) {
        RegistrationVerificationService.VerificationCompletion verificationCompletion =
                registrationVerificationService.verify(request.getEmail(), request.getVerificationCode());

        UserAccountEntity user = userAccountRepository.findWithRoleById(verificationCompletion.userId())
                .orElseThrow(InvalidCredentialsException::new);
        logAudit("VERIFY_REGISTRATION_SUCCESS", user.getId(), null, userManagementMapper.toProfile(user), user.getId());
        return userManagementMapper.toRegistrationVerificationData(user);
    }

    @Override
    @Transactional
    public ResendVerificationCodeResponseData resendVerificationCode(ResendVerificationCodeRequest request) {
        RegistrationVerificationService.VerificationDispatch verificationDispatch =
                registrationVerificationService.resend(request.getEmail());

        UserAccountEntity user = userAccountRepository.findWithRoleById(verificationDispatch.userId())
                .orElseThrow(InvalidCredentialsException::new);
        scheduleVerificationEmailDispatch(verificationDispatch);
        logAudit("RESEND_REGISTRATION_VERIFICATION", user.getId(), null, null, user.getId());
        return userManagementMapper.toResendVerificationCodeData(user, verificationDispatch.expireMinutes());
    }

    @Override
    @Transactional
    public LoginResponseData login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        UserAccountEntity user = userAccountRepository.findWithRoleByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        validateLoginStatus(user);

        String accessToken = accessTokenService.issueToken(user.getId());
        logAudit("LOGIN_SUCCESS", user.getId(), null, userManagementMapper.toAuthenticatedUser(user), user.getId());
        return userManagementMapper.toLoginData(accessToken, user);
    }

    @Override
    public void logout(String authorizationHeader) {
        String token = accessTokenService.extractBearerToken(authorizationHeader);
        if (token != null) {
            accessTokenService.invalidate(token);
        }
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        UserAccountEntity user = findCurrentUserEntity(currentUser.userId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new CurrentPasswordIncorrectException();
        }
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmNewPassword(), "confirmNewPassword");
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw RequestValidationException.singleError("newPassword", "New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);
        logAudit("CHANGE_PASSWORD", user.getId(), null, null, user.getId());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        userAccountRepository.findWithRoleByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            passwordResetTokenRepository.findByUser_IdAndUsedFalse(user.getId()).forEach(existingToken -> existingToken.setUsed(Boolean.TRUE));

            PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
            resetToken.setUser(user);
            resetToken.setToken(generateOpaqueToken());
            resetToken.setExpiredAt(LocalDateTime.now(APP_ZONE).plusMinutes(PASSWORD_RESET_EXPIRE_MINUTES));
            passwordResetTokenRepository.save(resetToken);
            executeAfterCommit(() -> notificationEmailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getFullName(),
                    new NotificationEmailService.PasswordResetEmailPayload(
                            resetToken.getToken(),
                            PASSWORD_RESET_EXPIRE_MINUTES
                    )
            ));

            logAudit(
                    "REQUEST_RESET_PASSWORD",
                    user.getId(),
                    null,
                    new ResetPasswordAuditPayload(resetToken.getId(), resetToken.getExpiredAt()),
                    user.getId()
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PasswordResetTokenValidationResponseData validateResetPasswordToken(String token) {
        String normalizedToken = normalizeNullable(token);
        if (!StringUtils.hasText(normalizedToken)) {
            throw new PasswordResetTokenInvalidException();
        }

        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(normalizedToken)
                .orElseThrow(PasswordResetTokenInvalidException::new);
        if (resetToken.getExpiredAt().isBefore(LocalDateTime.now(APP_ZONE))) {
            throw new PasswordResetTokenInvalidException();
        }
        return new PasswordResetTokenValidationResponseData(true, resetToken.getExpiredAt());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmNewPassword(), "confirmNewPassword");

        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken().trim())
                .orElseThrow(PasswordResetTokenInvalidException::new);

        if (resetToken.getExpiredAt().isBefore(LocalDateTime.now(APP_ZONE))) {
            resetToken.setUsed(Boolean.TRUE);
            throw new PasswordResetTokenInvalidException();
        }

        UserAccountEntity user = userAccountRepository.findWithRoleById(resetToken.getUser().getId())
                .orElseThrow(PasswordResetTokenInvalidException::new);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);

        resetToken.setUsed(Boolean.TRUE);
        accessTokenService.invalidateAll(user.getId());

        logAudit("RESET_PASSWORD_SUCCESS", user.getId(), null, null, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        return userManagementMapper.toProfile(findCurrentUserEntity(currentUser.userId()));
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.getCurrentUser();
        UserAccountEntity user = findCurrentUserEntity(currentUser.userId());
        UserProfileResponse oldState = userManagementMapper.toProfile(user);

        user.setFullName(normalize(request.getFullName()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setAddress(normalizeNullable(request.getAddress()));
        UserAccountEntity savedUser = userAccountRepository.save(user);

        if (RoleName.CUSTOMER.name().equalsIgnoreCase(savedUser.getRole().getName())) {
            customerProfileRepository.findByUser_Id(savedUser.getId()).ifPresent(customer -> syncCustomerProfile(customer, savedUser));
        }

        UserProfileResponse newState = userManagementMapper.toProfile(savedUser);
        logAudit("UPDATE_PROFILE", savedUser.getId(), oldState, newState, savedUser.getId());
        return newState;
    }

    private UserAccountEntity findCurrentUserEntity(String userId) {
        return userAccountRepository.findWithRoleById(userId)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private void validatePasswordConfirmation(String password, String confirmPassword, String fieldName) {
        if (!password.equals(confirmPassword)) {
            throw new PasswordMismatchException(fieldName, "Password confirmation does not match");
        }
    }

    private void validateLoginStatus(UserAccountEntity user) {
        if (Boolean.FALSE.equals(user.getEmailVerified())
                || AccountStatus.PENDING_VERIFICATION.name().equalsIgnoreCase(user.getStatus())) {
            throw new EmailVerificationRequiredException();
        }
        if (AccountStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            return;
        }
        throw new AccountLoginBlockedException(user.getStatus());
    }

    private void scheduleVerificationEmailDispatch(RegistrationVerificationService.VerificationDispatch verificationDispatch) {
        Runnable dispatchTask = () -> notificationEmailService.sendRegistrationVerificationEmail(
                verificationDispatch.email(),
                verificationDispatch.recipientName(),
                verificationDispatch.verificationCode(),
                verificationDispatch.expireMinutes()
        );

        executeAfterCommit(dispatchTask);
    }

    private void executeAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Dispatch after commit so SMTP work only starts when registration state is durable.
                // If delivery later fails asynchronously, the account remains pending and can resend safely.
                task.run();
            }
        });
    }

    private void syncCustomerProfile(CustomerProfileEntity customer, UserAccountEntity user) {
        customer.setContactPerson(user.getFullName());
        customer.setPhone(user.getPhone());
        customer.setAddress(user.getAddress());
        customer.setEmail(user.getEmail());
        customerProfileRepository.save(customer);
    }

    private void logAudit(String action, String entityId, Object oldValue, Object newValue, String userId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType("USER");
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(toJson(oldValue));
        auditLog.setNewValue(toJson(newValue));
        auditLogRepository.save(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit payload", exception);
        }
    }

    private String generateOpaqueToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record ResetPasswordAuditPayload(String tokenId, LocalDateTime expiredAt) {
    }
}
