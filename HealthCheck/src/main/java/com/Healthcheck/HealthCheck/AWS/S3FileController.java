package com.Healthcheck.HealthCheck.AWS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.Healthcheck.HealthCheck.Entities.FileMetaData;
import com.Healthcheck.HealthCheck.Repository.MetadataRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class S3FileController {

    private final S3UploadService s3UploadService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3FileController(S3UploadService s3UploadService) {
        this.s3UploadService = s3UploadService;
    }

    // ✅ POST: Upload a file and store metadata
    @PostMapping("/file")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("profilePic") MultipartFile file) {
        try {
            // Call the upload method
            String fileKey = s3UploadService.uploadFile(file);

            // Retrieve stored metadata
            FileMetaData metadata = s3UploadService.getFileMetadata(fileKey);
            if (metadata == null) {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to store metadata"));
            }

            // Generate upload date
            String uploadDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Build response
            Map<String, String> response = new HashMap<>();
            response.put("file_name", metadata.getFileName());
            response.put("id", metadata.getFileKey());
            response.put("url", metadata.getFileUrl());
            response.put("upload_date", uploadDate);

            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "File upload failed: " + e.getMessage()));
        }
    }

    // ✅ GET: Retrieve file details
    @GetMapping("/file/{fileKey}")
    public ResponseEntity<Map<String, String>> getFile(@PathVariable String fileKey) {
        try {
            FileMetaData metadata = s3UploadService.getFileMetadata(fileKey);
            if (metadata == null) {
                return ResponseEntity.status(404).body(Map.of("error", "File metadata not found"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("file_name", metadata.getFileName());
            response.put("id", metadata.getFileKey());
            response.put("url", metadata.getFileUrl());
            response.put("upload_date", metadata.getUploadDate().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error retrieving file: " + e.getMessage()));
        }
    }

    // ✅ DELETE: Remove file from S3
    @DeleteMapping("/file/{fileKey}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String fileKey) {
        // Check if file exists in the database before attempting deletion
        if (MetadataRepository.findByFileKey(fileKey).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "File not found"));
        }

        try {
            boolean isDeleted = s3UploadService.deleteFile(fileKey);
            if (!isDeleted) {
                return ResponseEntity.status(404).body(Map.of("error", "File not found in S3"));
            }

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error deleting file: " + e.getMessage()));
        }
    }

    // unsupported HTTP methods for /file endpoints

    // For get and Delete requests
    @RequestMapping(value = "/file", method = { RequestMethod.GET, RequestMethod.DELETE })
    public ResponseEntity<Map<String, String>> badRequestForFile() {
        return ResponseEntity.status(400).body(Map.of("error", "Bad request for /v1/file"));
    }

    // For post, put, head, patch, options requests
    @RequestMapping(value = "/file", method = { RequestMethod.GET, RequestMethod.DELETE, RequestMethod.PUT,
            RequestMethod.HEAD, RequestMethod.PATCH,
            RequestMethod.OPTIONS })
    public ResponseEntity<Void> methodNotSupportedFile() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    // For post, put, head, patch, options requests
    @RequestMapping(value = "/file/{fileKey}", method = { RequestMethod.POST, RequestMethod.PUT, RequestMethod.HEAD,
            RequestMethod.PATCH,
            RequestMethod.OPTIONS })
    public ResponseEntity<Void> methodNotSupportedFileByKey() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

}