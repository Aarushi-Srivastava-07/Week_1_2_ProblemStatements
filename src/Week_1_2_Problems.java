import java.util.*;
import java.util.concurrent.*;

public class Week_1_2_Problems {

    private final ConcurrentHashMap<String, Product> products = new ConcurrentHashMap<>();

    private static class Product {
        private final String id;
        private int stock;
        private final Queue<String> waitingList;

        Product(String id, int initialStock) {
            this.id = id;
            this.stock = initialStock;
            this.waitingList = new LinkedList<>();
        }

        synchronized int getStock() {
            return stock;
        }

        synchronized int purchase(String userId) {
            if (stock > 0) {
                stock--;
                return stock;
            } else {
                waitingList.add(userId);
                return -waitingList.size();
            }
        }
    }

    public void addProduct(String productId, int initialStock) {
        products.putIfAbsent(productId, new Product(productId, initialStock));
    }

    public int checkStock(String productId) {
        Product p = products.get(productId);
        if (p == null) throw new IllegalArgumentException("Product not found: " + productId);
        return p.getStock();
    }

    public String purchaseItem(String productId, String userId) {
        Product p = products.get(productId);
        if (p == null) return "Error: Product " + productId + " not found.";
        int result = p.purchase(userId);
        if (result >= 0) return "Success, " + result + " units remaining";
        else return "Added to waiting list, position #" + (-result);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Week_1_2_Problems manager = new Week_1_2_Problems();
        String productId = "IPHONE15_256GB";
        manager.addProduct(productId, 100);

        int totalUsers = 50_000;
        ExecutorService executor = Executors.newFixedThreadPool(200);
        List<Future<String>> results = new ArrayList<>();

        for (int i = 1; i <= totalUsers; i++) {
            String userId = "user" + i;
            results.add(executor.submit(() -> manager.purchaseItem(productId, userId)));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        int success = 0, waiting = 0, maxPos = 0;
        for (Future<String> f : results) {
            String msg = f.get();
            if (msg.startsWith("Success")) success++;
            else {
                waiting++;
                int pos = Integer.parseInt(msg.replaceAll("\\D+", ""));
                if (pos > maxPos) maxPos = pos;
            }
        }

        System.out.println("Purchases: " + success + ", Waiting: " + waiting + ", Final stock: " + manager.checkStock(productId) + ", Max waiting pos: " + maxPos);
    }
}