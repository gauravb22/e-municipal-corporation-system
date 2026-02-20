package com.emunicipal.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.emunicipal.util.ImageFormatValidator;

@Service
public class UploadStorageService {

    private final Path uploadRoot;

    public UploadStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeComplaintDonePhoto(long complaintId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Missing completion photo");
        }
        if (!ImageFormatValidator.isJpgMultipartFile(file)) {
            throw new IllegalArgumentException("Only JPG completion photos are allowed");
        }

        Files.createDirectories(uploadRoot);

        Path complaintDir = uploadRoot.resolve("complaints").resolve(String.valueOf(complaintId)).normalize();
        Files.createDirectories(complaintDir);

        String filename = "done-" + UUID.randomUUID() + ".jpg";
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
        if (!ImageFormatValidator.isJpgDataUrl(dataUrl)) {
            throw new IllegalArgumentException("Only JPG complaint photos are allowed");
        }

        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Invalid data URL");
        }

        String base64Part = dataUrl.substring(comma + 1);

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Part);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid base64 image data");
        }

        Files.createDirectories(uploadRoot);
        Path complaintDir = uploadRoot.resolve("complaints").resolve(String.valueOf(complaintId)).normalize();
        Files.createDirectories(complaintDir);

        String filename = "before-" + UUID.randomUUID() + ".jpg";
        Path target = complaintDir.resolve(filename).normalize();
        if (!target.startsWith(complaintDir)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        Files.write(target, bytes);
        return "/uploads/complaints/" + complaintId + "/" + filename;
    }
}
