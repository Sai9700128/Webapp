package com.Healthcheck.HealthCheck.AWS;

import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import com.Healthcheck.HealthCheck.Entities.FileMetaData;
import com.Healthcheck.HealthCheck.Repository.MetadataRepository;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3UploadService {

    private static final Logger logger = LoggerFactory.getLogger(S3UploadService.class);
    private static final String METRIC_UPLOAD_TIME = "s3.upload.time";
    private static final String METRIC_DELETE_TIME = "s3.delete.time";
    private static final String METRIC_GET_METADATA_TIME = "s3.metadata.get.time";

    private final S3Client s3Client;
    private final MetadataRepository fileMetadataRepository;
    private final StatsDClient statsDClient;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3UploadService(@Value("${aws.region}") String region, MetadataRepository fileMetadataRepository,
            StatsDClient statsDClient) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.statsDClient = statsDClient;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    // Method to retrieve the URL of the file from S3
    public String getFileUrl(String fileKey) {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("s3.get.url.hit");

        if (fileKey == null || fileKey.trim().isEmpty()) {
            logger.error("Invalid fileKey provided for getFileUrl: {}", fileKey);
            return null;
        }

        try {
            logger.info("Checking file existence for URL generation: {}", fileKey);
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build());

            String fileUrl = "https://" + bucketName + ".s3.amazonaws.com/" + fileKey;
            logger.info("Generated file URL: {}", fileUrl);
            return fileUrl;
        } catch (SdkException e) {
            logger.error("Failed to verify file existence for URL generation: {}", fileKey, e);
            return null;
        } finally {
            statsDClient.recordExecutionTime("s3.get.url.time", System.currentTimeMillis() - start);
        }
    }

    // Method to retrieve the upload date of the file from S3 metadata
    public String getFileUploadDate(String fileKey) {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("s3.get.uploadDate.hit");

        if (fileKey == null || fileKey.trim().isEmpty()) {
            logger.error("Invalid fileKey provided for getFileUploadDate: {}", fileKey);
            return null;
        }

        try {
            logger.info("Fetching upload date for file: {}", fileKey);
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            HeadObjectResponse metadata = s3Client.headObject(headObjectRequest);
            String uploadDate = metadata.lastModified().toString();
            logger.info("Retrieved upload date for file: {} - {}", fileKey, uploadDate);
            return uploadDate;
        } catch (S3Exception e) {
            logger.error("Failed to retrieve upload date for file: {}", fileKey, e);
            return null;
        } finally {
            statsDClient.recordExecutionTime("s3.get.uploadDate.time", System.currentTimeMillis() - start);
        }
    }

    // Method to retrieve file metadata from the database
    public FileMetaData getFileMetadata(String fileKey) {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("s3.get.metadata.hit");

        if (fileKey == null || fileKey.trim().isEmpty()) {
            logger.error("Invalid fileKey provided for getFileMetadata: {}", fileKey);
            return null;
        }

        try {
            logger.info("Retrieving metadata for fileKey: {}", fileKey);
            FileMetaData metadata = fileMetadataRepository.findByFileKey(fileKey).orElse(null);
            if (metadata == null) {
                logger.warn("No metadata found for fileKey: {}", fileKey);
            } else {
                logger.info("Retrieved metadata for fileKey: {}", fileKey);
            }
            return metadata;
        } catch (Exception e) {
            logger.error("Error retrieving metadata for fileKey: {}", fileKey, e);
            return null;
        } finally {
            statsDClient.recordExecutionTime(METRIC_GET_METADATA_TIME, System.currentTimeMillis() - start);
        }
    }

    // Updated uploadFile() method
    public String uploadFile(MultipartFile file) throws Exception {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("s3.upload.hit");

        if (file == null || file.isEmpty()) {
            logger.error("Invalid file provided for upload: file is null or empty");
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        String fileKey = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String fileUrl = "https://" + bucketName + ".s3.amazonaws.com/" + fileKey;

        try {
            logger.info("Uploading file to S3: {}", fileKey);
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .build(),
                    RequestBody.fromBytes(file.getBytes()));

            // Save metadata in RDS
            FileMetaData metadata = new FileMetaData(fileKey, file.getOriginalFilename(), fileUrl);
            fileMetadataRepository.save(metadata);

            logger.info("File uploaded to S3 and metadata saved: {}", fileKey);
            return fileKey;
        } catch (S3Exception e) {
            logger.error("S3 upload failed for file: {}", fileKey, e);
            throw new Exception("S3 upload failed: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Failed to read file bytes for upload: {}", fileKey, e);
            throw new Exception("Failed to read file content: " + e.getMessage(), e);
        } finally {
            statsDClient.recordExecutionTime(METRIC_UPLOAD_TIME, System.currentTimeMillis() - start);
        }
    }

    // Method to delete the file from S3
    public boolean deleteFile(String fileKey) {
        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("s3.delete.hit");

        if (fileKey == null || fileKey.trim().isEmpty()) {
            logger.error("Invalid fileKey provided for deletion: {}", fileKey);
            return false;
        }

        try {
            logger.info("Deleting file from S3: {}", fileKey);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            logger.info("Deleted file from S3: {}", fileKey);
            return true;
        } catch (S3Exception e) {
            logger.error("S3 deletion failed for file: {}", fileKey, e);
            return false;
        } finally {
            statsDClient.recordExecutionTime(METRIC_DELETE_TIME, System.currentTimeMillis() - start);
        }
    }
}