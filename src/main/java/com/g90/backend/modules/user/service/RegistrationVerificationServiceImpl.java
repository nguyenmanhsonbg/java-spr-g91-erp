package com.g90.backend.modules.user.service;

import com.g90.backend.exception.RegistrationAlreadyVerifiedException;
import com.g90.backend.exception.RegistrationVerificationCodeExpiredException;
import com.g90.backend.exception.RegistrationVerificationCodeInvalidException;
import com.g90.backend.exception.RegistrationVerificationNotPendingException;
import com.g90.backend.exception.RegistrationVerificationResendCooldownException;
import com.g90.backend.modules.account.entity.AccountStatus;
import com.g90.backend.modules.account.entity.UserAccountEntity;
import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.customer.entity.CustomerStatus;
import com.g90.backend.modules.email.config.EmailProperties;
import com.g90.backend.modules.user.entity.CustomerProfileEntity;
import com.g90.backend.modules.user.entity.EmailVerificationTokenEntity;
import com.g90.backend.modules.user.repository.CustomerProfileRepository;
import com.g90.backend.modules.user.repository.EmailVerificationTokenRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegistrationVerificationServiceImpl implements RegistrationVerificationService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String VERIFICATION_CODE_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int VERIFICATION_CODE_LENGTH = 5;

    private final UserAccountRepository userAccountRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailProperties emailProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public VerificationDispatch issueForNewRegistration(UserAccountEntity user) {
        return createAndStoreVerification(user, false);
    }

    @Override
    @Transactional
    public VerificationDispatch resend(String email) {
        UserAccountEntity user = userAccountRepository.findWithRoleByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(RegistrationVerificationNotPendingException::new);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RegistrationAlreadyVerifiedException();
        }
        if (!AccountStatus.PENDING_VERIFICATION.name().equalsIgnoreCase(user.getStatus())) {
            throw new RegistrationVerificationNotPendingException();
        }

        EmailVerificationTokenEntity latestToken = emailVerificationTokenRepository.findFirstByUser_IdOrderByCreatedAtDesc(user.getId()).orElse(null);
        if (latestToken != null) {
            LocalDateTime cooldownBoundary = latestToken.getCreatedAt()
                    .plusSeconds(emailProperties.getVerification().getResendCooldownSeconds());
            if (cooldownBoundary.isAfter(LocalDateTime.now(APP_ZONE))) {
                long retryAfterSeconds = java.time.Duration.between(LocalDateTime.now(APP_ZONE), cooldownBoundary).getSeconds();
                throw new RegistrationVerificationResendCooldownException(Math.max(retryAfterSeconds, 1L));
            }
        }

        return createAndStoreVerification(user, true);
    }

    @Override
    @Transactional
    public VerificationCompletion verify(String email, String verificationCode) {
        UserAccountEntity user = userAccountRepository.findWithRoleByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(RegistrationVerificationCodeInvalidException::new);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RegistrationAlreadyVerifiedException();
        }
        if (!AccountStatus.PENDING_VERIFICATION.name().equalsIgnoreCase(user.getStatus())) {
            throw new RegistrationVerificationNotPendingException();
        }

        EmailVerificationTokenEntity token = emailVerificationTokenRepository
                .findFirstByUser_IdAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(RegistrationVerificationCodeInvalidException::new);

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        if (token.getExpiredAt().isBefore(now)) {
            token.setInvalidatedAt(now);
            throw new RegistrationVerificationCodeExpiredException();
        }

        String normalizedCode = normalizeCode(verificationCode);
        if (!passwordEncoder.matches(normalizedCode, token.getCodeHash())) {
            throw new RegistrationVerificationCodeInvalidException();
        }

        token.setConsumedAt(now);
        user.setEmailVerified(Boolean.TRUE);
        user.setStatus(AccountStatus.ACTIVE.name());
        userAccountRepository.save(user);

        customerProfileRepository.findByUser_Id(user.getId()).ifPresent(customer -> activateCustomerProfile(customer));

        return new VerificationCompletion(user.getId(), user.getEmail(), user.getStatus(), true);
    }

    private VerificationDispatch createAndStoreVerification(UserAccountEntity user, boolean invalidateExisting) {
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        if (invalidateExisting) {
            invalidateActiveTokens(user.getId(), now);
        }

        String code = generateVerificationCode();
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.setUser(user);
        token.setCodeHash(passwordEncoder.encode(code));
        token.setExpiredAt(now.plusMinutes(emailProperties.getVerification().getExpireMinutes()));
        emailVerificationTokenRepository.save(token);

        return new VerificationDispatch(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                code,
                emailProperties.getVerification().getExpireMinutes()
        );
    }

    private void invalidateActiveTokens(String userId, LocalDateTime invalidatedAt) {
        emailVerificationTokenRepository.findByUser_IdAndConsumedAtIsNullAndInvalidatedAtIsNull(userId)
                .forEach(token -> token.setInvalidatedAt(invalidatedAt));
    }

    private void activateCustomerProfile(CustomerProfileEntity customer) {
        customer.setStatus(CustomerStatus.ACTIVE.name());
        customerProfileRepository.save(customer);
    }

    private String generateVerificationCode() {
        StringBuilder builder = new StringBuilder(VERIFICATION_CODE_LENGTH);
        for (int index = 0; index < VERIFICATION_CODE_LENGTH; index++) {
            builder.append(VERIFICATION_CODE_CHARSET.charAt(secureRandom.nextInt(VERIFICATION_CODE_CHARSET.length())));
        }
        return builder.toString();
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return StringUtils.trimWhitespace(value).toUpperCase(Locale.ROOT);
    }
}
