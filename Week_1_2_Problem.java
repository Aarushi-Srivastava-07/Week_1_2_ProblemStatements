import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Week_1_2_Problem {
    private static class DNSEntry {
        final String domain;
        final String ip;
        final long creationTime;
        final long ttlMillis;
        DNSEntry(String domain, String ip, long ttlSeconds) {
            this.domain = domain;
            this.ip = ip;
            this.creationTime = System.currentTimeMillis();
            this.ttlMillis = ttlSeconds * 1000;
        }
        boolean isExpired() {
            return System.currentTimeMillis() - creationTime > ttlMillis;
        }
    }

    private final int maxSize;
    private final long defaultTTL;
    private final LinkedHashMap<String, DNSEntry> cache;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong totalLookupTimeNanos = new AtomicLong();
    private final AtomicLong lookupCount = new AtomicLong();

    public Week_1_2_Problem(int maxSize, long defaultTTLSeconds) {
        this.maxSize = maxSize;
        this.defaultTTL = defaultTTLSeconds;
        this.cache = new LinkedHashMap<String, DNSEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > Week_1_2_Problem.this.maxSize;
            }
        };
        cleaner.scheduleAtFixedRate(this::cleanExpired, 5, 5, TimeUnit.SECONDS);
    }

    public String resolve(String domain) {
        long start = System.nanoTime();
        DNSEntry entry;
        boolean hit = false;
        String ip = null;
        synchronized (cache) {
            entry = cache.get(domain);
            if (entry != null && !entry.isExpired()) {
                hit = true;
                ip = entry.ip;
            } else {
                if (entry != null) cache.remove(domain);
            }
        }
        if (hit) {
            hitCount.incrementAndGet();
        } else {
            missCount.incrementAndGet();
            ip = queryUpstream(domain);
            DNSEntry newEntry = new DNSEntry(domain, ip, defaultTTL);
            synchronized (cache) {
                cache.put(domain, newEntry);
            }
        }
        long elapsed = System.nanoTime() - start;
        totalLookupTimeNanos.addAndGet(elapsed);
        lookupCount.incrementAndGet();
        return ip;
    }

    private String queryUpstream(String domain) {
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        int hash = domain.hashCode() & 0xFF;
        return "192.168." + (hash / 16) + "." + (hash % 16 + 1);
    }

    private void cleanExpired() {
        synchronized (cache) {
            Iterator<Map.Entry<String, DNSEntry>> it = cache.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue().isExpired()) it.remove();
            }
        }
    }

    public String getCacheStats() {
        long hits = hitCount.get(), misses = missCount.get(), total = hits + misses;
        double hitRate = total == 0 ? 0 : (double) hits / total * 100;
        long lookups = lookupCount.get();
        double avgTime = lookups == 0 ? 0 : (double) totalLookupTimeNanos.get() / lookups / 1_000_000;
        return String.format("Hit Rate: %.2f%%, Avg Lookup Time: %.3f ms (hits=%d, misses=%d)",
                hitRate, avgTime, hits, misses);
    }

    public void shutdown() { cleaner.shutdown(); }

    public static void main(String[] args) throws InterruptedException {
        Week_1_2_Problem dns = new Week_1_2_Problem(3, 5);
        System.out.println("resolve(\"google.com\") → " + dns.resolve("google.com"));
        System.out.println("resolve(\"google.com\") → " + dns.resolve("google.com"));
        System.out.println("resolve(\"yahoo.com\")  → " + dns.resolve("yahoo.com"));
        System.out.println("resolve(\"bing.com\")   → " + dns.resolve("bing.com"));
        System.out.println("resolve(\"yahoo.com\")  → " + dns.resolve("yahoo.com"));
        System.out.println("resolve(\"google.com\") → " + dns.resolve("google.com"));
        System.out.println("resolve(\"amazon.com\") → " + dns.resolve("amazon.com"));
        System.out.println("resolve(\"bing.com\")   → " + dns.resolve("bing.com"));
        System.out.println(dns.getCacheStats());

        System.out.println("Waiting for TTL expiration (6 seconds)...");
        Thread.sleep(6000);
        System.out.println("resolve(\"google.com\") → " + dns.resolve("google.com"));
        System.out.println("resolve(\"yahoo.com\")  → " + dns.resolve("yahoo.com"));
        System.out.println(dns.getCacheStats());

        dns.shutdown();
    }
}