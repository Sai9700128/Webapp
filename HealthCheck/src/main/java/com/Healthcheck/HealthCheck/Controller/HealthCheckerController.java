package com.Healthcheck.HealthCheck.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Healthcheck.HealthCheck.Servicelayer.HealthCheckService;

import java.util.Map;

@RestController
public class HealthCheckerController {
    private final HealthCheckService healthCheckerService;

    @Autowired
    public HealthCheckerController(HealthCheckService healthCheckerService) {
        this.healthCheckerService = healthCheckerService;
    }



    @GetMapping("/healthz")
    public ResponseEntity<Void> checkHealth(@RequestBody(required = false) String payload,
                                            @RequestParam(required = false) Map<String, String> queryParams) {
        if (payload != null && !payload.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .headers(setHeaders())
                    .build();
        }

        // Reject requests with query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .headers(setHeaders())
                    .build();
        }

        try {
            if (!healthCheckerService.isDatabaseConnected()) {
                throw new RuntimeException("Database connection unavailable");
            }
            healthCheckerService.insertHealthCheck();
            return ResponseEntity.status(HttpStatus.OK)
                    .headers(setHeaders())
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(setHeaders())
                    .build();
        }
    }


    @RequestMapping(value = "/healthz", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<Void> rejectNonGetRequests() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();  // 405 Method Not Allowed, no body
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
