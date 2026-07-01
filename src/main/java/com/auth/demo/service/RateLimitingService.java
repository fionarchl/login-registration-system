package com.auth.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for login attempts.
 *
 * <p>Tracks timestamps of login requests per IP address. If the number of
 * requests within the configured window exceeds the threshold, subsequent
 * requests are rejected until the window slides past older entries.</p>
 *
 * <p>Stale entries are cleaned up every 5 minutes via a {@code @Scheduled} task.</p>
 */
@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

    @Value("${app.rate-limit.login.max-attempts}")
    private int maxAttempts;

    @Value("${app.rate-limit.login.window-seconds}")
    private int windowSeconds;

    private final Map<String, List<Instant>> requestLog = new ConcurrentHashMap<>();

    /**
     * Records a login attempt from the given IP and returns {@code true}
     * if the rate limit has been exceeded.
     */
    public boolean isRateLimited(String ipAddress) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        requestLog.compute(ipAddress, (key, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayList<>();
            }
            // Remove entries outside the sliding window
            timestamps.removeIf(t -> t.isBefore(windowStart));
            timestamps.add(now);
            return timestamps;
        });

        List<Instant> timestamps = requestLog.get(ipAddress);
        boolean limited = timestamps != null && timestamps.size() > maxAttempts;

        if (limited) {
            log.warn("Rate limit exceeded for IP: {}", ipAddress);
        }

        return limited;
    }

    /**
     * Cleans up stale entries every 5 minutes to prevent memory leaks.
     */
    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds * 2L);
        requestLog.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(t -> t.isBefore(cutoff));
            return entry.getValue().isEmpty();
        });
    }
}
