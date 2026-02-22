package com.emunicipal.service;

import com.emunicipal.util.PhoneNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    public boolean sendOtp(String toPhoneNumber, String otp) {
        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(toPhoneNumber);
        if (normalizedPhone == null || otp == null || otp.isBlank()) {
            logger.warn("OTP send aborted due to invalid phone or otp");
            return false;
        }

        logger.info("OTP generated for {} in local popup mode", maskPhone(normalizedPhone));
        return true;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "******";
        }
        return "******" + phone.substring(phone.length() - 4);
    }
}
