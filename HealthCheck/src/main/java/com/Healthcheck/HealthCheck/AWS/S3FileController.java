package com.Healthcheck.HealthCheck.AWS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.timgroup.statsd.StatsDClient;

import com.Healthcheck.HealthCheck.Entities.FileMetaData;
import com.Healthcheck.HealthCheck.Repository.MetadataRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1")
public class S3FileController {

    private static final Logger logger = LoggerFactory.getLogger(S3FileController.class);
    private final S3UploadService s3UploadService;
    private final MetadataRepository metadataRepository; // Added for direct DB access
    private final StatsDClient statsDClient; // Added for metrics tracking

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    // Updated constructor to include all dependencies
    public S3FileController(S3UploadService s3UploadService, MetadataRepository metadataRepository,
            StatsDClient statsDClient) {
        this.s3UploadService = s3UploadService;
        this.metadataRepository = metadataRepository;
        this.statsDClient = statsDClient;
    }

    // ✅ POST: Upload a file and store metadata
    @PostMapping("/file")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("profilePic") MultipartFile file) {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("api.file.upload.hit");
        logger.info("Received file upload request for: {}", file.getOriginalFilename());

        try {
            // Call the upload method
            String fileKey = s3UploadService.uploadFile(file);

            // Retrieve stored metadata
            FileMetaData metadata = s3UploadService.getFileMetadata(fileKey);
            if (metadata == null) {
                logger.error("Failed to store metadata for fileKey: {}", fileKey);
                return ResponseEntity.status(500).body(Map.of("error", "Failed to store metadata"));
            }

            // Generate upload date
            String uploadDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Save metadata to the database
            long dbStart = System.currentTimeMillis();
            metadataRepository.save(metadata);
            statsDClient.recordExecutionTime("db.file.save", System.currentTimeMillis() - dbStart);
            logger.info("Saved file metadata to DB for fileKey: {}", fileKey);

            // Build response
            Map<String, String> response = new HashMap<>();
            response.put("file_name", metadata.getFileName());
            response.put("id", metadata.getFileKey());
            response.put("url", metadata.getFileUrl());
            response.put("upload_date", uploadDate);

            logger.info("File uploaded successfully for fileKey: {}", fileKey);
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            logger.error("File upload failed for file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(400).body(Map.of("error", "File upload failed: " + e.getMessage()));
        } finally {
            statsDClient.recordExecutionTime("api.file.upload.time", System.currentTimeMillis() - start);
        }
    }

    // ✅ GET: Retrieve file details
    @GetMapping("/file/{fileKey}")
    public ResponseEntity<Map<String, String>> getFile(@PathVariable String fileKey) {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("api.file.get.hit");
        logger.info("Fetching file metadata for fileKey: {}", fileKey);

        try {
            long dbStart = System.currentTimeMillis();
            Optional<FileMetaData> metadataOpt = metadataRepository.findByFileKey(fileKey);
            statsDClient.recordExecutionTime("db.file.get", System.currentTimeMillis() - dbStart);

            if (metadataOpt.isEmpty()) {
                logger.warn("File with fileKey {} not found", fileKey);
                return ResponseEntity.status(404).body(Map.of("error", "File metadata not found"));
            }

            FileMetaData metadata = metadataOpt.get();
            Map<String, String> response = new HashMap<>();
            response.put("file_name", metadata.getFileName());
            response.put("id", metadata.getFileKey());
            response.put("url", metadata.getFileUrl());
            response.put("upload_date", metadata.getUploadDate().toString());

            logger.info("Returning metadata for fileKey: {}", fileKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving file for fileKey: {}", fileKey, e);
            return ResponseEntity.status(500).body(Map.of("error", "Error retrieving file: " + e.getMessage()));
        } finally {
            statsDClient.recordExecutionTime("api.file.get.time", System.currentTimeMillis() - start);
        }
    }

    @DeleteMapping("/file/{fileKey}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String fileKey) {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("api.file.delete.hit");
        logger.info("Received delete request for fileKey: {}", fileKey);

        try {
            long dbStart = System.currentTimeMillis();
            Optional<FileMetaData> metadataOpt = metadataRepository.findByFileKey(fileKey);
            statsDClient.recordExecutionTime("db.file.get", System.currentTimeMillis() - dbStart);

            if (metadataOpt.isEmpty()) {
                logger.warn("File with fileKey {} not found for deletion", fileKey);
                return ResponseEntity.status(404).body(Map.of("error", "File not found"));
            }

            FileMetaData metadata = metadataOpt.get();

            // Delete from S3
            boolean isDeleted = s3UploadService.deleteFile(fileKey);
            if (!isDeleted) {
                logger.error("Failed to delete file from S3 for fileKey: {}", fileKey);
                return ResponseEntity.status(500).body(Map.of("error", "Error deleting file from S3"));
            }

            // Delete metadata from DB using the entity
            dbStart = System.currentTimeMillis();
            metadataRepository.delete(metadata); // Changed from deleteById(fileKey)
            statsDClient.recordExecutionTime("db.file.delete", System.currentTimeMillis() - dbStart);
            logger.info("Deleted file metadata from DB for fileKey: {}", fileKey);

            logger.info("Deleted file from S3 for fileKey: {}", fileKey);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting file for fileKey: {}", fileKey, e);
            return ResponseEntity.status(500).body(Map.of("error", "Error deleting file: " + e.getMessage()));
        } finally {
            statsDClient.recordExecutionTime("api.file.delete.time", System.currentTimeMillis() - start);
        }
    }

    // Unsupported HTTP methods for /file endpoints
    @RequestMapping(value = "/file", method = { RequestMethod.GET, RequestMethod.DELETE })
    public ResponseEntity<Map<String, String>> badRequestForFile() {
        logger.warn("Bad request received for /v1/file");
        return ResponseEntity.status(400).body(Map.of("error", "Bad request for /v1/file"));
    }

    @RequestMapping(value = "/file", method = { RequestMethod.PUT, RequestMethod.HEAD, RequestMethod.PATCH,
            RequestMethod.OPTIONS })
    public ResponseEntity<Void> methodNotSupportedFile() {
        logger.warn("Method not allowed for /v1/file");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @RequestMapping(value = "/file/{fileKey}", method = { RequestMethod.POST, RequestMethod.PUT, RequestMethod.HEAD,
            RequestMethod.PATCH, RequestMethod.OPTIONS })
    public ResponseEntity<Void> methodNotSupportedFileByKey() {
        logger.warn("Method not allowed for /v1/file/{}");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}