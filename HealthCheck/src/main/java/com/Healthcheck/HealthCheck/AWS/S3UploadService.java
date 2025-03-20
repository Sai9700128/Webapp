package com.Healthcheck.HealthCheck.AWS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.Healthcheck.HealthCheck.Entities.FileMetaData;
import com.Healthcheck.HealthCheck.Repository.MetadataRepository;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.UUID;

@Service
public class S3UploadService {

    private final S3Client s3Client;
    private final MetadataRepository fileMetadataRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3UploadService(@Value("${aws.region}") String region, MetadataRepository fileMetadataRepository) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.fileMetadataRepository = fileMetadataRepository;
    }

    // Method to retrieve the URL of the file from S3
    public String getFileUrl(String fileKey) {
        try {
            // Check if the object exists in S3
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build());

            // Generate the file URL
            return bucketName + fileKey;
        } catch (SdkException e) {
            return null;
        }
    }

    // Method to retrieve the upload date of the file from S3 metadata
    public String getFileUploadDate(String fileKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            HeadObjectResponse metadata = s3Client.headObject(headObjectRequest);

            return metadata.lastModified().toString();
        } catch (S3Exception e) {
            return null;
        }
    }

    public FileMetaData getFileMetadata(String fileKey) {
        return MetadataRepository.findByFileKey(fileKey).orElse(null);
    }

    // Updated uploadFile() method (Removed userId)
    public String uploadFile(MultipartFile file) throws Exception {
        // Generate unique file name
        String fileKey = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String fileUrl = "https://" + bucketName + ".s3.amazonaws.com/" + fileKey;

        // Upload to S3
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(file.getContentType())
                .build(),
                RequestBody.fromBytes(file.getBytes()));

        // Save metadata in RDS
        FileMetaData metadata = new FileMetaData(fileKey, file.getOriginalFilename(), fileUrl);
        fileMetadataRepository.save(metadata);

        return fileKey;
    }

    // Method to delete the file from S3
    public boolean deleteFile(String fileKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            return true;

        } catch (S3Exception e) {
            return false;
        }
    }
}
