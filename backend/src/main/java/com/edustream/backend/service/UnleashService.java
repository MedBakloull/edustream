package com.edustream.backend.service;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.util.UnleashConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UnleashService {

    private static final Logger log = LoggerFactory.getLogger(UnleashService.class);
    private static final String FLAG_CANARY = "canary-enrollment-v2";

    @Value("${unleash.api.url:}")
    private String unleashApiUrl;

    @Value("${unleash.api.token:}")
    private String unleashApiToken;

    private Unleash unleash;
    private boolean unleashAvailable = false;

    // Live distribution telemetry
    private final Set<String> v1Users = ConcurrentHashMap.newKeySet();
    private final Set<String> v2Users = ConcurrentHashMap.newKeySet();
    private final AtomicInteger totalEvaluations = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        if (unleashApiUrl == null || unleashApiUrl.isBlank() ||
            unleashApiToken == null || unleashApiToken.isBlank()) {
            log.warn("Unleash not configured — feature flags disabled (all flags return OFF).");
            return;
        }
        try {
            UnleashConfig config = UnleashConfig.builder()
                    .appName("edustream-backend")
                    .instanceId("edustream-production")
                    .unleashAPI(unleashApiUrl)
                    .apiKey(unleashApiToken)
                    .build();
            this.unleash = new DefaultUnleash(config);
            this.unleashAvailable = true;
            log.info("Unleash connected to {}", unleashApiUrl);
        } catch (Exception e) {
            log.error("Failed to connect to Unleash — feature flags disabled: {}", e.getMessage());
        }
    }

    public boolean isCanaryEnrollmentEnabled() {
        return isCanaryEnrollmentEnabled(null);
    }

    public boolean isCanaryEnrollmentEnabled(String userId) {
        totalEvaluations.incrementAndGet();

        if (!unleashAvailable || unleash == null) {
            recordEvaluation(userId, false);
            return false;
        }

        try {
            boolean enabled;
            if (userId != null && !userId.isBlank()) {
                UnleashContext context = UnleashContext.builder()
                        .userId(userId)
                        .build();
                enabled = unleash.isEnabled(FLAG_CANARY, context);
            } else {
                enabled = unleash.isEnabled(FLAG_CANARY);
            }
            recordEvaluation(userId, enabled);
            return enabled;
        } catch (Exception e) {
            log.warn("Unleash flag check failed — defaulting to OFF: {}", e.getMessage());
            recordEvaluation(userId, false);
            return false;
        }
    }

    private void recordEvaluation(String userId, boolean isCanary) {
        if (userId == null || userId.isBlank()) return;
        if (isCanary) {
            v2Users.add(userId);
            v1Users.remove(userId);
        } else {
            v1Users.add(userId);
            v2Users.remove(userId);
        }
    }

    public Map<String, Object> getMetrics() {
        int v1 = v1Users.size();
        int v2 = v2Users.size();
        int total = v1 + v2;
        int v1Percent = total > 0 ? (v1 * 100) / total : 0;
        int v2Percent = total > 0 ? (v2 * 100) / total : 0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("v1UsersCount", v1);
        metrics.put("v2UsersCount", v2);
        metrics.put("totalUsersCount", total);
        metrics.put("v1Percentage", v1Percent);
        metrics.put("v2Percentage", v2Percent);
        metrics.put("totalEvaluations", totalEvaluations.get());
        return metrics;
    }
}
