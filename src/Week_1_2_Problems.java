import java.util.*;

public class Week_1_2_Problems {
    private static final int N = 5;
    private final Map<String, Set<Integer>> index = new HashMap<>();
    private final Map<Integer, List<String>> docNGrams = new HashMap<>();

    public void addDocument(int docId, String text) {
        List<String> ngrams = extractNGrams(text);
        docNGrams.put(docId, ngrams);
        for (String ng : ngrams) {
            index.computeIfAbsent(ng, k -> new HashSet<>()).add(docId);
        }
    }

    private List<String> extractNGrams(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        List<String> ngrams = new ArrayList<>();
        if (words.length < N) return ngrams;
        for (int i = 0; i <= words.length - N; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < N; j++) {
                if (j > 0) sb.append(' ');
                sb.append(words[i + j]);
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }

    public Map<Integer, Double> analyzeDocument(String text) {
        List<String> queryNGrams = extractNGrams(text);
        int total = queryNGrams.size();
        if (total == 0) return Collections.emptyMap();

        Map<Integer, Integer> matchCount = new HashMap<>();
        for (String ng : queryNGrams) {
            Set<Integer> docs = index.get(ng);
            if (docs != null) {
                for (int docId : docs) {
                    matchCount.merge(docId, 1, Integer::sum);
                }
            }
        }

        Map<Integer, Double> similarity = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : matchCount.entrySet()) {
            similarity.put(e.getKey(), e.getValue() * 100.0 / total);
        }
        return similarity;
    }

    public void printAnalysis(String documentName, String text) {
        System.out.println("analyzeDocument(\"" + documentName + "\")");
        int total = extractNGrams(text).size();
        System.out.println("-> Extracted " + total + " n-grams");
        Map<Integer, Double> results = analyzeDocument(text);
        if (results.isEmpty()) {
            System.out.println("-> No matches found.");
            return;
        }
        results.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .forEach(e -> {
                    int docId = e.getKey();
                    double sim = e.getValue();
                    int matches = (int) Math.round(sim * total / 100);
                    String tag = sim > 50 ? "PLAGIARISM DETECTED" : (sim > 20 ? "suspicious" : "low");
                    System.out.printf("-> Found %d matching n-grams with \"essay_%03d.txt\"\n", matches, docId);
                    System.out.printf("-> Similarity: %.1f%% (%s)\n", sim, tag);
                });
    }

    public static void main(String[] args) {
        Week_1_2_Problems detector = new Week_1_2_Problems();

        detector.addDocument(89, "The quick brown fox jumps over the lazy dog. The quick brown fox is quick.");
        detector.addDocument(92, "Plagiarism is the act of using someone else's work without permission. It is unethical and often illegal. Students should avoid plagiarism at all costs.");
        detector.addDocument(45, "This is a unique essay about Java programming. Java is a popular language for enterprise applications.");

        String newEssay = "The quick brown fox jumps over the lazy dog. The quick brown fox is indeed quick. Plagiarism is wrong and should be avoided.";
        detector.printAnalysis("essay_123.txt", newEssay);

        System.out.println("\n--- Additional check ---");
        String anotherEssay = "Java is used in many enterprise applications. It is a popular language.";
        detector.printAnalysis("essay_456.txt", anotherEssay);
    }
}