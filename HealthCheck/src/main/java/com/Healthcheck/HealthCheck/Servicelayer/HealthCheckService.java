package com.Healthcheck.HealthCheck.Servicelayer;

import com.Healthcheck.HealthCheck.Entities.HealthCheck;
import com.Healthcheck.HealthCheck.Repository.HealthCheckRepository;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    private static final StatsDClient statsd = new NonBlockingStatsDClient("CSYE6225App", "localhost", 8125);
    private final String HEALTH_CHECK_TABLE = "health_check"; // Table name to check

    @Autowired
    private HealthCheckRepository healthCheckRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean isDatabaseConnected() throws TableNotFoundException {
        long startTime = System.currentTimeMillis();
        logger.info("Checking database connection and table existence for '{}'", HEALTH_CHECK_TABLE);

        try {
            // Check if the table exists before attempting to connect
            if (!tableExists(HEALTH_CHECK_TABLE)) {
                long duration = System.currentTimeMillis() - startTime;
                statsd.recordExecutionTime("db.query.error.time", duration);
                logger.error("Table '{}' not found in database after {} ms", HEALTH_CHECK_TABLE, duration);
                throw new TableNotFoundException("Table '" + HEALTH_CHECK_TABLE + "' not found in database.");
            }

            // Test database connection with a simple query
            long queryStart = System.currentTimeMillis();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long queryDuration = System.currentTimeMillis() - queryStart;
            statsd.recordExecutionTime("db.query.time", queryDuration);
            long totalDuration = System.currentTimeMillis() - startTime;
            logger.info("Database connection confirmed in {} ms (query took {} ms)", totalDuration, queryDuration);
            return true;
        } catch (TableNotFoundException e) {
            throw e; // Re-throw the custom exception
        } catch (DataAccessException e) {
            long duration = System.currentTimeMillis() - startTime;
            statsd.recordExecutionTime("db.query.error.time", duration);
            logger.error("Database connection failed after {} ms: {}", duration, e.getMessage(), e);
            return false;
        }
    }

    private boolean tableExists(String tableName) {
        long startTime = System.currentTimeMillis();
        logger.debug("Checking if table '{}' exists", tableName);

        try {
            jdbcTemplate.execute("SELECT 1 FROM " + tableName + " LIMIT 1");
            long duration = System.currentTimeMillis() - startTime;
            statsd.recordExecutionTime("db.query.time", duration);
            logger.debug("Table '{}' exists, check completed in {} ms", tableName, duration);
            return true;
        } catch (DataAccessException e) {
            long duration = System.currentTimeMillis() - startTime;
            statsd.recordExecutionTime("db.query.error.time", duration);
            logger.warn("Table '{}' does not exist, check failed after {} ms: {}", tableName, duration, e.getMessage());
            return false;
        }
    }

    public boolean logHealthCheck() {
        long startTime = System.currentTimeMillis();
        logger.info("Logging health check to database");

        try {
            HealthCheck healthCheck = new HealthCheck();
            long queryStart = System.currentTimeMillis();
            healthCheckRepository.save(healthCheck);
            long queryDuration = System.currentTimeMillis() - queryStart;
            statsd.recordExecutionTime("db.query.time", queryDuration);
            long totalDuration = System.currentTimeMillis() - startTime;
            logger.info("Health check logged successfully in {} ms (query took {} ms)", totalDuration, queryDuration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            statsd.recordExecutionTime("db.query.error.time", duration);
            logger.error("Failed to log health check after {} ms: {}", duration, e.getMessage(), e);
            return false;
        }
    }

    public void insertHealthCheck() {
        logger.debug("Inserting health check via logHealthCheck");
        logHealthCheck();
    }
}