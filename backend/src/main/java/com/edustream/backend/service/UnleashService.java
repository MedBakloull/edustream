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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UnleashService {

    private static final Logger log = LoggerFactory.getLogger(UnleashService.class);
    private static final String FLAG_CANARY = "canary-enrollment-v2";

    // Sessions that have not sent a heartbeat in 6 seconds are considered offline
    private static final long SESSION_HEARTBEAT_TTL_MS = 6000L;

    @Value("${unleash.api.url:}")
    private String unleashApiUrl;

    @Value("${unleash.api.token:}")
    private String unleashApiToken;

    private Unleash unleash;
    private boolean unleashAvailable = false;

    // Track active online sessions with timestamps (Heartbeat TTL)
    private static class ActiveSession {
        final boolean isCanary;
        final long lastSeen;

        ActiveSession(boolean isCanary, long lastSeen) {
            this.isCanary = isCanary;
            this.lastSeen = lastSeen;
        }
    }

    private final ConcurrentHashMap<String, ActiveSession> activeSessions = new ConcurrentHashMap<>();
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
        activeSessions.put(userId, new ActiveSession(isCanary, System.currentTimeMillis()));
    }

    private void cleanupInactiveSessions() {
        long now = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry -> (now - entry.getValue().lastSeen) > SESSION_HEARTBEAT_TTL_MS);
    }

    public Map<String, Object> getMetrics() {
        cleanupInactiveSessions();

        int v1 = 0;
        int v2 = 0;
        for (ActiveSession session : activeSessions.values()) {
            if (session.isCanary) {
                v2++;
            } else {
                v1++;
            }
        }
        int total = v1 + v2;
        int v1Percent = total > 0 ? (v1 * 100) / total : 50;
        int v2Percent = total > 0 ? (v2 * 100) / total : 50;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("v1UsersCount", v1);
        metrics.put("v2UsersCount", v2);
        metrics.put("totalUsersCount", total);
        metrics.put("v1Percentage", v1Percent);
        metrics.put("v2Percentage", v2Percent);
        metrics.put("totalEvaluations", totalEvaluations.get());
        return metrics;
    }

    public void resetAllSessions() {
        activeSessions.clear();
        totalEvaluations.set(0);
    }
}
