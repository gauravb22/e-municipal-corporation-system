package com.emunicipal.util;

import java.util.Locale;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public final class ImageFormatValidator {

    private ImageFormatValidator() {
    }

    public static boolean isJpgDataUrl(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return false;
        }

        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex <= 0) {
            return false;
        }

        String header = dataUrl.substring(0, commaIndex).toLowerCase(Locale.ROOT);
        if (!header.startsWith("data:image/")) {
            return false;
        }

        int colonIndex = header.indexOf(':');
        int semicolonIndex = header.indexOf(';');
        if (colonIndex < 0 || semicolonIndex <= colonIndex) {
            return false;
        }

        String mimeType = header.substring(colonIndex + 1, semicolonIndex).trim();
        return "image/jpeg".equals(mimeType) || "image/jpg".equals(mimeType);
    }

    public static boolean isJpgMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            String normalized = contentType.trim().toLowerCase(Locale.ROOT);
            if ("image/jpeg".equals(normalized) || "image/jpg".equals(normalized)) {
                return true;
            }
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return false;
        }
        String cleaned = StringUtils.cleanPath(originalName).toLowerCase(Locale.ROOT);
        return cleaned.endsWith(".jpg") || cleaned.endsWith(".jpeg");
    }
}
