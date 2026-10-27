package com.edustream.backend.controller;

import com.edustream.backend.service.UnleashService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/status")
@CrossOrigin(origins = "*")
public class StatusController {

    private final UnleashService unleashService;

    public StatusController(UnleashService unleashService) {
        this.unleashService = unleashService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean canary = unleashService.isCanaryEnrollmentEnabled();

        Map<String, Object> status = new HashMap<>();
        status.put("service", "edustream-backend");
        status.put("status", "UP");
        status.put("enrollmentOpen", canary);
        status.put("canaryFlag", "canary-enrollment-v2");
        status.put("canaryActive", canary);
        status.put("mode", canary ? "CANARY v2 - Enrollment OPEN" : "STABLE v1 - Enrollment BLOCKED");
        status.put("timestamp", java.time.Instant.now().toString());

        return ResponseEntity.ok(status);
    }
}
