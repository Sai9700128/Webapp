package com.Healthcheck.HealthCheck.Controller;

import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Healthcheck.HealthCheck.Servicelayer.HealthCheckService;

import java.util.Map;

@RestController
public class HealthCheckerController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckerController.class);
    private static final StatsDClient statsd = new NonBlockingStatsDClient("CSYE6225App", "localhost", 8125);
    private final HealthCheckService healthCheckerService;

    @Autowired
    public HealthCheckerController(HealthCheckService healthCheckerService) {
        this.healthCheckerService = healthCheckerService;
    }

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, String>> checkHealth(@RequestBody(required = false) String payload,
            @RequestParam(required = false) Map<String, String> queryParams) {
        long startTime = System.currentTimeMillis();
        logger.info("Received GET /healthz request");
        statsd.incrementCounter("api.healthz.count");

        // Check for payload
        if (payload != null && !payload.isEmpty()) {
            logger.warn("Invalid request: Payload provided in GET /healthz");
            statsd.incrementCounter("api.healthz.errors");
            long duration = System.currentTimeMillis() - startTime;
            statsd.recordExecutionTime("api.healthz.error.time", duration);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .headers(setHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Bad request: Payload should be empty"));
        }

        // Check for query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            logger.warn("Invalid request: Query parameters provided in GET /healthz");
            statsd.incrementCounter("api.healthz.errors");
            long duration = System.currentTimeMillis() - startTime;
            statsd.recordExecutionTime("api.healthz.error.time", duration);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .headers(setHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Bad request: Query parameters are not allowed"));
        }

        try {
            if (!healthCheckerService.isDatabaseConnected()) {
                throw new RuntimeException("Database connection unavailable");
            }
            healthCheckerService.insertHealthCheck();
            long duration = System.currentTimeMillis() - startTime;
            statsd.recordExecutionTime("api.healthz.time", duration);
            logger.info("Health check successful: System is up, completed in {} ms", duration);
            return ResponseEntity.status(HttpStatus.OK)
                    .headers(setHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "UP"));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            statsd.recordExecutionTime("api.healthz.error.time", duration);
            logger.error("Health check failed after {} ms: Database connection unavailable", duration, e);
            statsd.incrementCounter("api.healthz.errors");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(setHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Database connection unavailable"));
        }
    }

    @RequestMapping(value = "/healthz", method = { RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE,
            RequestMethod.PATCH })
    public ResponseEntity<Void> rejectNonGetRequests() {
        long startTime = System.currentTimeMillis();
        logger.warn("Received unsupported method for /healthz");
        statsd.incrementCounter("api.healthz.unsupported.count");
        long duration = System.currentTimeMillis() - startTime;
        statsd.recordExecutionTime("api.healthz.unsupported.time", duration);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    private HttpHeaders setHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Connection", "keep-alive");
        return headers;
    }
}