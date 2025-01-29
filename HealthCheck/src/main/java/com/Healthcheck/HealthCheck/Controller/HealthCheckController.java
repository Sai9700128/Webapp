package com.Healthcheck.HealthCheck.Controller;


import com.Healthcheck.HealthCheck.Servicelayer.HealthCheckService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HealthCheckController {

    @Autowired
    private HealthCheckService healthcheckservice;

    @GetMapping("/healthz")
    public ResponseEntity<Void> healthCheck(HttpServletRequest request){
        if("GET".equalsIgnoreCase(request.getMethod())) {
            boolean success = healthcheckservice.logHealthCheck();
//            return success ? ResponseEntity.status(200).header("Cache-Control", "no-cache").build() : ResponseEntity.status(503).header("Cache-Control", "no-cache").build();
            if (success) {
                return ResponseEntity
                        .status(200)
                        .header("Cache-Control", "no-cache, no-store, must-revalidate;")
                        .header("Pragma", "no-cache")
                        .header("X-Content-Type-Options", "nosniff")
                        .build();
            } else {
                return ResponseEntity
                        .status(503)
                        .header("Cache-Control", "no-cache, no-store, must-revalidate;")
                        .header("Pragma", "no-cache")
                        .header("X-Content-Type-Options", "nosniff")
                        .build();
            }
        }
        return ResponseEntity
                .status(405)
                .header("Cache-Control", "no-cache, no-store, must-revalidate;")
                .header("Pragma", "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }
}