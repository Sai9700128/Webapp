package com.Healthcheck.HealthCheck.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Healthcheck.HealthCheck.Entities.FileMetaData;

import java.util.Optional;

@Repository
public interface MetadataRepository extends JpaRepository<FileMetaData, Long> {
    static Optional<FileMetaData> findByFileKey(String fileKey) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByFileKey'");
    }
}
