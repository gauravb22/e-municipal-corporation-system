package com.emunicipal.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {

    public boolean sendOtp(String toPhoneNumber, String otp) {
        // For now, just log OTP - Twilio integration disabled
        System.out.println("================== OTP GENERATED ==================");
        System.out.println("Phone: " + toPhoneNumber);
        System.out.println("OTP: " + otp);
        System.out.println("====================================================");
        return true;
    }
}
