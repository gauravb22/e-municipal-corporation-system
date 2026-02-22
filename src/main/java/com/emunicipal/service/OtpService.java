package com.emunicipal.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;

@Service
public class OtpService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_UPPER_BOUND = 1_000_000;
    private static final int SALT_BYTES = 16;

    public String issueOtp(HttpSession session,
                           String contextKey,
                           Long subjectId,
                           String phone,
                           int expiryMinutes,
                           int maxAttempts) {
        clear(session, contextKey);

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(OTP_UPPER_BOUND));
        String salt = generateSalt();
        String otpHash = hashOtp(salt, otp);

        session.setAttribute(key(contextKey, "otpHash"), otpHash);
        session.setAttribute(key(contextKey, "salt"), salt);
        session.setAttribute(key(contextKey, "subjectId"), subjectId);
        session.setAttribute(key(contextKey, "phone"), phone);
        session.setAttribute(key(contextKey, "expiresAt"), LocalDateTime.now().plusMinutes(expiryMinutes));
        session.setAttribute(key(contextKey, "attemptsLeft"), Math.max(1, maxAttempts));
        return otp;
    }

    public OtpCheckResult verifyOtp(HttpSession session,
                                    String contextKey,
                                    String enteredOtp,
                                    Long expectedSubjectId,
                                    String expectedPhone) {
        if (enteredOtp == null || enteredOtp.isBlank()) {
            return new OtpCheckResult(OtpStatus.MISSING, 0);
        }

        String normalizedOtp = enteredOtp.trim();
        if (!normalizedOtp.matches("\\d{6}")) {
            return new OtpCheckResult(OtpStatus.INVALID, 0);
        }

        String storedHash = (String) session.getAttribute(key(contextKey, "otpHash"));
        String salt = (String) session.getAttribute(key(contextKey, "salt"));
        Long storedSubjectId = asLong(session.getAttribute(key(contextKey, "subjectId")));
        String storedPhone = (String) session.getAttribute(key(contextKey, "phone"));
        LocalDateTime expiresAt = asLocalDateTime(session.getAttribute(key(contextKey, "expiresAt")));
        Integer attemptsLeft = asInteger(session.getAttribute(key(contextKey, "attemptsLeft")));

        if (storedHash == null || salt == null || storedSubjectId == null || storedPhone == null
                || expiresAt == null || attemptsLeft == null) {
            return new OtpCheckResult(OtpStatus.NO_CHALLENGE, 0);
        }

        if (expectedSubjectId != null && !Objects.equals(expectedSubjectId, storedSubjectId)) {
            clear(session, contextKey);
            return new OtpCheckResult(OtpStatus.ACCOUNT_MISMATCH, 0);
        }

        if (expectedPhone != null && !Objects.equals(expectedPhone, storedPhone)) {
            clear(session, contextKey);
            return new OtpCheckResult(OtpStatus.PHONE_MISMATCH, 0);
        }

        if (LocalDateTime.now().isAfter(expiresAt)) {
            clear(session, contextKey);
            return new OtpCheckResult(OtpStatus.EXPIRED, 0);
        }

        String enteredHash = hashOtp(salt, normalizedOtp);
        boolean match = MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                enteredHash.getBytes(StandardCharsets.UTF_8));

        if (!match) {
            int remaining = Math.max(0, attemptsLeft - 1);
            if (remaining <= 0) {
                clear(session, contextKey);
                return new OtpCheckResult(OtpStatus.TOO_MANY_ATTEMPTS, 0);
            }
            session.setAttribute(key(contextKey, "attemptsLeft"), remaining);
            return new OtpCheckResult(OtpStatus.INVALID, remaining);
        }

        clear(session, contextKey);
        return new OtpCheckResult(OtpStatus.SUCCESS, 0);
    }

    public void clear(HttpSession session, String contextKey) {
        session.removeAttribute(key(contextKey, "otpHash"));
        session.removeAttribute(key(contextKey, "salt"));
        session.removeAttribute(key(contextKey, "subjectId"));
        session.removeAttribute(key(contextKey, "phone"));
        session.removeAttribute(key(contextKey, "expiresAt"));
        session.removeAttribute(key(contextKey, "attemptsLeft"));
    }

    private static String key(String contextKey, String suffix) {
        return contextKey + "." + suffix;
    }

    private static String generateSalt() {
        byte[] saltBytes = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    private static String hashOtp(String salt, String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static Long asLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return null;
    }

    public enum OtpStatus {
        SUCCESS,
        MISSING,
        NO_CHALLENGE,
        ACCOUNT_MISMATCH,
        PHONE_MISMATCH,
        EXPIRED,
        INVALID,
        TOO_MANY_ATTEMPTS
    }

    public record OtpCheckResult(OtpStatus status, int attemptsRemaining) {
        public boolean isSuccess() {
            return status == OtpStatus.SUCCESS;
        }
    }
}
