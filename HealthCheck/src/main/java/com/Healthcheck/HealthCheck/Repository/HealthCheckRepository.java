package com.Healthcheck.HealthCheck.Repository;

import com.Healthcheck.HealthCheck.Entities.HealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {
}

