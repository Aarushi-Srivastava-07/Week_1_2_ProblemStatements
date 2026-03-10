import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class RateLimiter {
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final double refillRatePerSecond;

    public RateLimiter(int capacity, double refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    private static class TokenBucket {
        private final int capacity;
        private final double refillRate;
        private double tokens;
        private long lastRefill;

        TokenBucket(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefill = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        synchronized int getAvailableTokens() {
            refill();
            return (int) tokens;
        }

        synchronized long getResetTimeSeconds() {
            refill();
            double deficit = capacity - tokens;
            long seconds = (long) Math.ceil(deficit / refillRate);
            return seconds;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefill) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillRate);
            lastRefill = now;
        }
    }

    public String checkRateLimit(String clientId) {
        TokenBucket bucket = buckets.computeIfAbsent(clientId, k -> new TokenBucket(capacity, refillRatePerSecond));
        boolean allowed = bucket.tryConsume();
        if (allowed) {
            int remaining = bucket.getAvailableTokens();
            return "Allowed (" + remaining + " requests remaining)";
        } else {
            int remaining = bucket.getAvailableTokens();
            long retryAfter = bucket.getResetTimeSeconds();
            return "Denied (" + remaining + " requests remaining, retry after " + retryAfter + "s)";
        }
    }

    public String getRateLimitStatus(String clientId) {
        TokenBucket bucket = buckets.get(clientId);
        if (bucket == null) return "No data for client";
        int used = capacity - bucket.getAvailableTokens();
        long reset = System.currentTimeMillis() / 1000 + bucket.getResetTimeSeconds();
        return "{used: " + used + ", limit: " + capacity + ", reset: " + reset + "}";
    }

    public static void main(String[] args) throws InterruptedException {
        RateLimiter limiter = new RateLimiter(1000, 1000 / 3600.0); // 1000 per hour
        String client = "abc123";

        for (int i = 0; i < 1005; i++) {
            System.out.println("checkRateLimit(clientId=\"" + client + "\") → " + limiter.checkRateLimit(client));
        }

        System.out.println("getRateLimitStatus(\"" + client + "\") → " + limiter.getRateLimitStatus(client));

        // Simulate time passing to allow some tokens to refill
        Thread.sleep(3600 * 1000 / 4); // 15 minutes
        System.out.println("After 15 minutes: " + limiter.checkRateLimit(client));
        System.out.println("getRateLimitStatus(\"" + client + "\") → " + limiter.getRateLimitStatus(client));
    }
}