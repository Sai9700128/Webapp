package com.Healthcheck.HealthCheck.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/")
public class HealthCheckController {

    @GetMapping("/healthz")
    public ResponseEntity<Void> healthCheck(@RequestBody(required = false) String body) {
        if (body != null && !body.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();  // 400 Bad Request, no body
        }
        return ResponseEntity.ok().build();  // 200 OK, no body
    }

    @RequestMapping(value = "/healthz", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<Void> rejectNonGetRequests() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();  // 405 Method Not Allowed, no body
    }
}
