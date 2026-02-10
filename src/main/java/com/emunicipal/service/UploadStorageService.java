package com.emunicipal.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadStorageService {

    private static final Map<String, String> CONTENT_TYPE_TO_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path uploadRoot;

    public UploadStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeComplaintDonePhoto(long complaintId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Missing completion photo");
        }

        Files.createDirectories(uploadRoot);

        String ext = guessImageExtension(file);
        Path complaintDir = uploadRoot.resolve("complaints").resolve(String.valueOf(complaintId)).normalize();
        Files.createDirectories(complaintDir);

        String filename = "done-" + UUID.randomUUID() + ext;
        Path target = complaintDir.resolve(filename).normalize();
        if (!target.startsWith(complaintDir)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/complaints/" + complaintId + "/" + filename;
    }

    public String storeComplaintPhotoFromDataUrl(long complaintId, String dataUrl) throws IOException {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("Missing complaint photo");
        }
        if (!dataUrl.startsWith("data:image/")) {
            throw new IllegalArgumentException("Unsupported complaint photo format");
        }

        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Invalid data URL");
        }

        String header = dataUrl.substring(0, comma).toLowerCase(Locale.ROOT);
        String base64Part = dataUrl.substring(comma + 1);

        String contentType = null;
        // Example: data:image/jpeg;base64
        int colon = header.indexOf(':');
        int semi = header.indexOf(';');
        if (colon >= 0 && semi > colon) {
            contentType = header.substring(colon + 1, semi);
        }

        String ext = ".jpg";
        if (contentType != null) {
            String mapped = CONTENT_TYPE_TO_EXT.get(contentType);
            if (mapped != null) {
                ext = mapped;
            }
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Part);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid base64 image data");
        }

        Files.createDirectories(uploadRoot);
        Path complaintDir = uploadRoot.resolve("complaints").resolve(String.valueOf(complaintId)).normalize();
        Files.createDirectories(complaintDir);

        String filename = "before-" + UUID.randomUUID() + ext;
        Path target = complaintDir.resolve(filename).normalize();
        if (!target.startsWith(complaintDir)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        Files.write(target, bytes);
        return "/uploads/complaints/" + complaintId + "/" + filename;
    }

    private String guessImageExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null) {
            String cleaned = StringUtils.cleanPath(original);
            int dot = cleaned.lastIndexOf('.');
            if (dot >= 0 && dot < cleaned.length() - 1) {
                String ext = cleaned.substring(dot).toLowerCase(Locale.ROOT);
                if (ext.equals(".jpg") || ext.equals(".jpeg")) return ".jpg";
                if (ext.equals(".png")) return ".png";
                if (ext.equals(".webp")) return ".webp";
            }
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            String mapped = CONTENT_TYPE_TO_EXT.get(contentType.toLowerCase(Locale.ROOT));
            if (mapped != null) return mapped;
        }

        // Default to jpg to keep things simple
        return ".jpg";
    }
}
