package com.Healthcheck.HealthCheck.Servicelayer;

import com.Healthcheck.HealthCheck.Entities.HealthCheck;
import com.Healthcheck.HealthCheck.Repository.HealthCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckService {

    @Autowired
    private HealthCheckRepository healthCheckRepository;

    public boolean logHealthCheck() {
        try {
            HealthCheck healthCheck = new HealthCheck();
            healthCheckRepository.save(healthCheck);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    
}

