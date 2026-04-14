package org.example.ailoldraftingcheck.api;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiny shared IP-rate limiter so we don't have to repeat the bucket4j wiring
 * in every AI controller. Identical configuration knobs as the chatgpt-jokes
 * example: capacity, refill_amount and refill_time (in minutes).
 *
 * Usage:  rateLimit.consumeOrThrow(httpRequest);
 */
@Component
public class RateLimit {

    @Value("${app.bucket_capacity}")
    private int capacity;

    @Value("${app.refill_amount}")
    private int refillAmount;

    @Value("${app.refill_time}")
    private int refillMinutes;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void consumeOrThrow(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> {
            Bandwidth limit = Bandwidth.classic(capacity,
                    Refill.greedy(refillAmount, Duration.ofMinutes(refillMinutes)));
            return Bucket.builder().addLimit(limit).build();
        });
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests from your IP. Wait a couple of minutes and try again.");
        }
    }
}
