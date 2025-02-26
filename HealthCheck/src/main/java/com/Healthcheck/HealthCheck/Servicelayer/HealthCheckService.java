package com.Healthcheck.HealthCheck.Servicelayer;

import com.Healthcheck.HealthCheck.Entities.HealthCheck;
import com.Healthcheck.HealthCheck.Repository.HealthCheckRepository;

import org.apache.hadoop.hbase.TableNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


@Service
public class HealthCheckService {

    @Autowired
    private HealthCheckRepository healthCheckRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String HEALTH_CHECK_TABLE = "health_check"; // Table name to check

    public boolean isDatabaseConnected() throws TableNotFoundException {
        try {
            // Check if the table exists before attempting to connect
            if (!tableExists(HEALTH_CHECK_TABLE)) {
                throw new TableNotFoundException("Table '" + HEALTH_CHECK_TABLE + "' not found in database.");
            }
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (TableNotFoundException e) {
            throw e; // Re-throw the custom exception
        } catch (DataAccessException e) {
            return false; // Or throw a different exception if you want specific error handling
        }
    }

    private boolean tableExists(String tableName) {
        try {
            // Execute a query to check if the table exists
            jdbcTemplate.execute("SELECT 1 FROM " + tableName + " LIMIT 1");
            return true; // If the query succeeds, the table exists
        } catch (DataAccessException e) {
            return false; // If the query fails, the table doesn't exist
        }
    }

    public boolean logHealthCheck() {
        try {
            HealthCheck healthCheck = new HealthCheck();
            healthCheckRepository.save(healthCheck);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void insertHealthCheck() {
        logHealthCheck();
    }
}


