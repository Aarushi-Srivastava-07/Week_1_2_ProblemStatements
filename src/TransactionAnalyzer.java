import java.util.*;
import java.time.LocalTime;
import java.time.Duration;

public class TransactionAnalyzer {
    public static class Transaction {
        int id;
        double amount;
        String merchant;
        LocalTime time;

        public Transaction(int id, double amount, String merchant, String time) {
            this.id = id;
            this.amount = amount;
            this.merchant = merchant;
            this.time = LocalTime.parse(time);
        }
    }

    private List<Transaction> transactions = new ArrayList<>();

    public void addTransaction(Transaction t) {
        transactions.add(t);
    }

    public List<int[]> findTwoSum(double target) {
        Map<Double, Integer> map = new HashMap<>();
        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                result.add(new int[]{map.get(complement), t.id});
            }
            map.put(t.amount, t.id);
        }
        return result;
    }

    public List<int[]> findTwoSumWithinTime(double target, int minutes) {
        Map<Double, Transaction> map = new HashMap<>();
        List<int[]> result = new ArrayList<>();
        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                Transaction other = map.get(complement);
                long diff = Math.abs(Duration.between(other.time, t.time).toMinutes());
                if (diff <= minutes) {
                    result.add(new int[]{other.id, t.id});
                }
            }
            map.put(t.amount, t);
        }
        return result;
    }

    public List<List<Integer>> findKSum(int k, double target) {
        List<List<Integer>> result = new ArrayList<>();
        findKSumHelper(0, new ArrayList<>(), 0.0, target, k, result);
        return result;
    }

    private void findKSumHelper(int start, List<Integer> current, double sum, double target, int k, List<List<Integer>> result) {
        if (current.size() == k) {
            if (Math.abs(sum - target) < 0.001) result.add(new ArrayList<>(current));
            return;
        }
        if (start >= transactions.size()) return;
        for (int i = start; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            current.add(t.id);
            findKSumHelper(i + 1, current, sum + t.amount, target, k, result);
            current.remove(current.size() - 1);
        }
    }

    public List<Map<String, Object>> detectDuplicates() {
        Map<String, List<Transaction>> groups = new HashMap<>();
        for (Transaction t : transactions) {
            String key = t.amount + "|" + t.merchant;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
        List<Map<String, Object>> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : groups.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> dup = new HashMap<>();
                String[] parts = entry.getKey().split("\\|");
                dup.put("amount", Double.parseDouble(parts[0]));
                dup.put("merchant", parts[1]);
                List<Integer> accounts = new ArrayList<>();
                for (Transaction t : entry.getValue()) accounts.add(t.id);
                dup.put("accounts", accounts);
                duplicates.add(dup);
            }
        }
        return duplicates;
    }

    public static void main(String[] args) {
        TransactionAnalyzer ta = new TransactionAnalyzer();
        ta.addTransaction(new Transaction(1, 500, "Store A", "10:00"));
        ta.addTransaction(new Transaction(2, 300, "Store B", "10:15"));
        ta.addTransaction(new Transaction(3, 200, "Store C", "10:30"));
        ta.addTransaction(new Transaction(4, 500, "Store A", "11:00"));
        ta.addTransaction(new Transaction(5, 100, "Store D", "10:45"));

        System.out.println("findTwoSum(target=500) -> " + formatPairs(ta.findTwoSum(500)));
        System.out.println("findTwoSumWithinTime(target=500, minutes=60) → " + formatPairs(ta.findTwoSumWithinTime(500, 60)));
        System.out.println("findTwoSumWithinTime(target=500, minutes=30) → " + formatPairs(ta.findTwoSumWithinTime(500, 30)));
        System.out.println("detectDuplicates() -> " + ta.detectDuplicates());
        System.out.println("findKSum(k=3, target=1000) -> " + ta.findKSum(3, 1000));
    }

    private static String formatPairs(List<int[]> pairs) {
        List<String> str = new ArrayList<>();
        for (int[] p : pairs) str.add("(id:" + p[0] + ", id:" + p[1] + ")");
        return str.toString();
    }
}