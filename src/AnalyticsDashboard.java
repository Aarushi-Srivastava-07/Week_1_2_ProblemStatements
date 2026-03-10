import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class AnalyticsDashboard {
    private final ConcurrentHashMap<String, AtomicInteger> pageViews = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> sourceCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AnalyticsDashboard() {
        scheduler.scheduleAtFixedRate(this::printDashboard, 5, 5, TimeUnit.SECONDS);
    }

    public void processEvent(String url, String userId, String source) {
        pageViews.computeIfAbsent(url, k -> new AtomicInteger()).incrementAndGet();
        uniqueVisitors.computeIfAbsent(url, k -> ConcurrentHashMap.newKeySet()).add(userId);
        sourceCounts.computeIfAbsent(source, k -> new AtomicInteger()).incrementAndGet();
    }

    public void printDashboard() {
        System.out.println("\n" + getDashboard());
    }

    public String getDashboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dashboard Snapshot:\n");

        sb.append("Top Pages:\n");
        pageViews.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().get() - e1.getValue().get())
            .limit(10)
            .forEach(e -> {
                String url = e.getKey();
                int views = e.getValue().get();
                int uniques = uniqueVisitors.getOrDefault(url, ConcurrentHashMap.newKeySet()).size();
                sb.append(String.format("%d. %s - %,d views (%,d unique)\n", 
                    sb.length() == 0 ? 1 : sb.length(), url, views, uniques));
            });

        sb.append("Traffic Sources:\n");
        int totalSources = sourceCounts.values().stream().mapToInt(AtomicInteger::get).sum();
        if (totalSources > 0) {
            sourceCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().get() - e1.getValue().get())
                .forEach(e -> {
                    int count = e.getValue().get();
                    double percent = (count * 100.0) / totalSources;
                    sb.append(String.format("%s: %.1f%%, ", e.getKey(), percent));
                });
            sb.setLength(sb.length() - 2);
        } else {
            sb.append("No traffic data.");
        }
        return sb.toString();
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        AnalyticsDashboard dashboard = new AnalyticsDashboard();
        String[] sources = {"google", "facebook", "direct", "twitter", "bing"};
        String[] urls = {"/article/breaking-news", "/sports/championship", "/tech/gadgets", 
                         "/world/politics", "/entertainment/movies", "/health/wellness"};

        Random rand = new Random();
        ExecutorService eventGenerator = Executors.newSingleThreadExecutor();
        eventGenerator.submit(() -> {
            while (!Thread.interrupted()) {
                String url = urls[rand.nextInt(urls.length)];
                String userId = "user_" + (1000 + rand.nextInt(9000));
                String source = sources[rand.nextInt(sources.length)];
                dashboard.processEvent(url, userId, source);
                try { Thread.sleep(rand.nextInt(10)); } catch (InterruptedException e) { break; }
            }
        });

        Thread.sleep(20000);
        eventGenerator.shutdownNow();
        dashboard.shutdown();
    }
}