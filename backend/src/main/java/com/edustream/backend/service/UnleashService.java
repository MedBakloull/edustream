package com.edustream.backend.service;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

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

    /**
     * Returns true when the canary enrollment flag is enabled.
     * Falls back to false (normal flow) if Unleash is unavailable.
     */
    public boolean isCanaryEnrollmentEnabled() {
        if (!unleashAvailable || unleash == null) {
            return false;
        }
        try {
            return unleash.isEnabled(FLAG_CANARY);
        } catch (Exception e) {
            log.warn("Unleash flag check failed — defaulting to OFF: {}", e.getMessage());
            return false;
        }
    }
}
