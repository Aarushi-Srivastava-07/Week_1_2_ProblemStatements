import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiLevelCache {
    private static class Video {
        String id;
        String data;
        int accessCount;
        Video(String id, String data) {
            this.id = id;
            this.data = data;
            this.accessCount = 0;
        }
    }

    private final int L1_CAPACITY = 10000;
    private final int L2_CAPACITY = 100000;
    private final int PROMOTION_THRESHOLD = 5;

    private final Map<String, Video> database = new ConcurrentHashMap<>();

    private final LinkedHashMap<String, Video> l1Cache = new LinkedHashMap<String, Video>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Video> eldest) {
            return size() > L1_CAPACITY;
        }
    };

    private final LinkedHashMap<String, Video> l2Cache = new LinkedHashMap<String, Video>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Video> eldest) {
            return size() > L2_CAPACITY;
        }
    };

    private final Map<String, Integer> accessCountMap = new ConcurrentHashMap<>();

    private long l1Hits = 0, l1Misses = 0, l1Time = 0;
    private long l2Hits = 0, l2Misses = 0, l2Time = 0;
    private long l3Hits = 0, l3Misses = 0, l3Time = 0;

    public void addToDatabase(String videoId, String data) {
        database.put(videoId, new Video(videoId, data));
    }

    public String getVideo(String videoId) {
        long start = System.nanoTime();
        Video v;

        synchronized (l1Cache) {
            v = l1Cache.get(videoId);
        }
        if (v != null) {
            l1Hits++;
            l1Time += (System.nanoTime() - start);
            simulateLatency(0.5);
            return "L1 Cache HIT (" + v.data + ")";
        }
        l1Misses++;
        l1Time += (System.nanoTime() - start);

        start = System.nanoTime();
        synchronized (l2Cache) {
            v = l2Cache.get(videoId);
        }
        if (v != null) {
            l2Hits++;
            l2Time += (System.nanoTime() - start);
            simulateLatency(5);
            int count = accessCountMap.merge(videoId, 1, Integer::sum);
            if (count >= PROMOTION_THRESHOLD) {
                promoteToL1(v);
            }
            return "L2 Cache HIT (" + v.data + ")";
        }
        l2Misses++;
        l2Time += (System.nanoTime() - start);

        start = System.nanoTime();
        v = database.get(videoId);
        if (v != null) {
            l3Hits++;
            l3Time += (System.nanoTime() - start);
            simulateLatency(150);
            addToL2(v);
            return "L3 Database HIT (" + v.data + ")";
        } else {
            l3Misses++;
            l3Time += (System.nanoTime() - start);
            return "Video not found";
        }
    }

    private void promoteToL1(Video v) {
        synchronized (l1Cache) {
            l1Cache.put(v.id, v);
        }
        synchronized (l2Cache) {
            l2Cache.remove(v.id);
        }
    }

    private void addToL2(Video v) {
        synchronized (l2Cache) {
            l2Cache.put(v.id, v);
        }
        accessCountMap.put(v.id, 1);
    }

    private void simulateLatency(double ms) {
        try {
            Thread.sleep((long) ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String getStatistics() {
        long l1Total = l1Hits + l1Misses;
        long l2Total = l2Hits + l2Misses;
        long l3Total = l3Hits + l3Misses;
        long overallHits = l1Hits + l2Hits + l3Hits;
        long overallTotal = overallHits + l1Misses + l2Misses + l3Misses;
        double l1HitRate = l1Total == 0 ? 0 : (double) l1Hits / l1Total * 100;
        double l2HitRate = l2Total == 0 ? 0 : (double) l2Hits / l2Total * 100;
        double l3HitRate = l3Total == 0 ? 0 : (double) l3Hits / l3Total * 100;
        double overallHitRate = overallTotal == 0 ? 0 : (double) overallHits / overallTotal * 100;
        double l1Avg = l1Total == 0 ? 0 : l1Time / 1_000_000.0 / l1Total;
        double l2Avg = l2Total == 0 ? 0 : l2Time / 1_000_000.0 / l2Total;
        double l3Avg = l3Total == 0 ? 0 : l3Time / 1_000_000.0 / l3Total;
        double overallAvg = overallTotal == 0 ? 0 : (l1Time + l2Time + l3Time) / 1_000_000.0 / overallTotal;

        return String.format("L1: Hit Rate %.1f%%, Avg Time: %.1fms\n", l1HitRate, l1Avg) +
               String.format("L2: Hit Rate %.1f%%, Avg Time: %.1fms\n", l2HitRate, l2Avg) +
               String.format("L3: Hit Rate %.1f%%, Avg Time: %.1fms\n", l3HitRate, l3Avg) +
               String.format("Overall: Hit Rate %.1f%%, Avg Time: %.1fms", overallHitRate, overallAvg);
    }

    public static void main(String[] args) {
        MultiLevelCache cache = new MultiLevelCache();

        for (int i = 1; i <= 1000; i++) {
            cache.addToDatabase("video_" + i, "Data for video " + i);
        }

        System.out.println(cache.getVideo("video_123"));
        System.out.println(cache.getVideo("video_123"));
        System.out.println(cache.getVideo("video_999"));

        for (int i = 0; i < 5; i++) {
            cache.getVideo("video_123");
        }

        System.out.println(cache.getVideo("video_123"));
        System.out.println(cache.getStatistics());
    }
}