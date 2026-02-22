package com.emunicipal.util;

public final class PhoneNumberUtil {
    private static final String INDIA_COUNTRY_CODE = "91";
    private static final int LOCAL_PHONE_LENGTH = 10;

    private PhoneNumberUtil() {
    }

    public static String normalizeIndianPhone(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }

        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.length() == LOCAL_PHONE_LENGTH) {
            return digits;
        }

        if (digits.length() == LOCAL_PHONE_LENGTH + INDIA_COUNTRY_CODE.length()
                && digits.startsWith(INDIA_COUNTRY_CODE)) {
            return digits.substring(INDIA_COUNTRY_CODE.length());
        }

        if (digits.length() == LOCAL_PHONE_LENGTH + 1 && digits.startsWith("0")) {
            return digits.substring(1);
        }

        return null;
    }

    public static String formatWithDefaultCountryCode(String rawPhone) {
        String normalizedPhone = normalizeIndianPhone(rawPhone);
        if (normalizedPhone == null) {
            return null;
        }
        return "+91 " + normalizedPhone;
    }
}
