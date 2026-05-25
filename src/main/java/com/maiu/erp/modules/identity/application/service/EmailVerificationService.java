package com.maiu.erp.modules.identity.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.domain.model.User;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;
import com.maiu.erp.modules.identity.infrastructure.persistence.verification.EmailVerificationTokenEntity;
import com.maiu.erp.modules.identity.infrastructure.persistence.verification.EmailVerificationTokenJpaRepository;

import jakarta.transaction.Transactional;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenJpaRepository tokenRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String frontendBaseUrl;
    private final String fromAddress;

    public EmailVerificationService(
            EmailVerificationTokenJpaRepository tokenRepository,
            UserRepository userRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.frontend-base-url:http://localhost:3000}") String frontendBaseUrl,
            @Value("${app.mail.from:no-reply@maiu.local}") String fromAddress) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.frontendBaseUrl = frontendBaseUrl;
        this.fromAddress = fromAddress;
    }

    @Transactional
    public void createAndSendVerification(User user) {
        EmailVerificationTokenEntity tokenEntity = new EmailVerificationTokenEntity();
        tokenEntity.setToken(UUID.randomUUID().toString());
        tokenEntity.setUserId(user.getId());
        tokenEntity.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        tokenRepository.save(tokenEntity);

        String verificationUrl = frontendBaseUrl.replaceAll("/$", "")
                + "/verify-email?token=" + tokenEntity.getToken();

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail not configured. Email verification link for {}: {}", user.getEmail(), verificationUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Confirm your ERP account");
        message.setText("""
                Welcome to ERP.

                Please confirm your email address by opening the link below:
                %s

                This link will expire in 24 hours.
                """.formatted(verificationUrl));
        mailSender.send(message);
    }

    @Transactional
    public void verifyToken(String token) {
        EmailVerificationTokenEntity tokenEntity = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (tokenEntity.getVerifiedAt() != null) {
            throw new RuntimeException("Email already confirmed");
        }

        if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Verification token has expired");
        }

        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        tokenEntity.setVerifiedAt(Instant.now());
        tokenRepository.save(tokenEntity);
    }
}
