import java.util.*;
import java.text.NumberFormat;

public class AutocompleteSystem {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        List<QueryFreq> top = new ArrayList<>(); 
    }

    private static class QueryFreq {
        String query;
        int freq;
        QueryFreq(String q, int f) { query = q; freq = f; }
    }

    private final TrieNode root = new TrieNode();
    private final Map<String, Integer> freqMap = new HashMap<>();
    private static final int TOP_K = 10;

    public void updateFrequency(String query) {
        int newFreq = freqMap.getOrDefault(query, 0) + 1;
        freqMap.put(query, newFreq);
        TrieNode node = root;
        updateNodeTop(node, query, newFreq);
        for (char c : query.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
            updateNodeTop(node, query, newFreq);
        }
    }

    private void updateNodeTop(TrieNode node, String query, int newFreq) {
        List<QueryFreq> top = node.top;
        for (QueryFreq qf : top) {
            if (qf.query.equals(query)) {
                qf.freq = newFreq;
                top.sort((a, b) -> b.freq - a.freq);
                return;
            }
        }
        if (top.size() < TOP_K) {
            top.add(new QueryFreq(query, newFreq));
            top.sort((a, b) -> b.freq - a.freq);
        } else {
            QueryFreq last = top.get(top.size() - 1);
            if (newFreq > last.freq) {
                top.remove(top.size() - 1);
                top.add(new QueryFreq(query, newFreq));
                top.sort((a, b) -> b.freq - a.freq);
            }
        }
    }

    public List<String> search(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        NumberFormat nf = NumberFormat.getInstance();
        int rank = 1;
        for (QueryFreq qf : node.top) {
            result.add(rank++ + ". \"" + qf.query + "\" (" + nf.format(qf.freq) + " searches)");
        }
        return result;
    }

    public static void main(String[] args) {
        AutocompleteSystem ac = new AutocompleteSystem();
        String[] queries = {
            "java tutorial", "javascript", "java download", "python tutorial",
            "java 21 features", "javascript framework", "java interview questions",
            "java 8 features", "java spring boot", "java microservices",
            "java tutorial", "java tutorial", "java tutorial", 
            "javascript", "javascript", "java download", "java 21 features"
        };
        for (String q : queries) ac.updateFrequency(q);

        System.out.println("search(\"jav\") →");
        for (String s : ac.search("jav")) System.out.println(s);

        System.out.println("\nupdateFrequency(\"java 21 features\") →");
        for (int i = 0; i < 5; i++) ac.updateFrequency("java 21 features");
        for (String s : ac.search("jav")) System.out.println(s);
    }
}