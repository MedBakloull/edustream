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
    public ResponseEntity<Map<String, Object>> getStatus(@RequestParam(required = false) String userId) {
        boolean canary = (userId != null && !userId.isBlank())
                ? unleashService.isCanaryEnrollmentEnabled(userId)
                : unleashService.isCanaryEnrollmentEnabled();

        Map<String, Object> status = new HashMap<>();
        status.put("service", "edustream-backend");
        status.put("status", "UP");
        status.put("canaryFlag", "canary-enrollment-v2");
        status.put("canaryActive", canary);
        status.put("userId", userId != null ? userId : "anonymous");
        status.put("assignedVersion", canary ? "v2-canary" : "v1-stable");
        status.put("mode", canary ? "CANARY v2 (Testing)" : "STABLE v1 (Production Safe)");
        status.put("metrics", unleashService.getMetrics());
        status.put("timestamp", java.time.Instant.now().toString());

        return ResponseEntity.ok(status);
    }
}
