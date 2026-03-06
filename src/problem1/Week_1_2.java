package problem1;

import java.util.*;

public class Week_1_2 {
    private Map<String, Integer> userDatabase = new HashMap<>();
    private Map<String, Integer> attemptFrequency = new HashMap<>();

    public Week_1_2() {
        userDatabase.put("john_doe", 101);
        userDatabase.put("jane_smith", 102);
        userDatabase.put("admin", 103);
    }

    public boolean checkAvailability(String username) {
        attemptFrequency.put(username, attemptFrequency.getOrDefault(username, 0) + 1);
        return !userDatabase.containsKey(username);
    }

    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            suggestions.add(username + i);
        }
        suggestions.add(username.replace("_", "."));
        suggestions.add(username + "_");
        suggestions.removeIf(userDatabase::containsKey);
        return suggestions;
    }

    public String getMostAttempted() {
        return attemptFrequency.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No attempts yet");
    }

    public static void main(String[] args) {
        Week_1_2 checker = new Week_1_2();

        System.out.println("checkAvailability(\"john_doe\"): " + checker.checkAvailability("john_doe"));
        System.out.println("checkAvailability(\"new_user\"): " + checker.checkAvailability("new_user"));
        System.out.println("checkAvailability(\"john_doe\"): " + checker.checkAvailability("john_doe"));
        System.out.println("checkAvailability(\"admin\"): " + checker.checkAvailability("admin"));

        System.out.println("Suggestions for 'john_doe': " + checker.suggestAlternatives("john_doe"));
        System.out.println("Most attempted: " + checker.getMostAttempted());
    }
}