package nl.multicode;

import nl.multicode.model.ClusterResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clusters strings by shared words (lexical overlap).
 * The group key is the word shared by the most strings in each round.
 * {@code minimalDenominatorLength} is the minimum word length to consider as a token.
 *
 * Note: this is structural word-overlap clustering, not embedding-based semantic similarity.
 */
public class SemanticClusterer implements TextClusterer {

    @Override
    public ClusterResult cluster(List<String> input, int minWordLength, int minimumGroupSize) {
        if (input == null || input.isEmpty()) {
            return new ClusterResult(Map.of(), Map.of());
        }

        Map<Integer, String> original = index(input);
        Map<Integer, String> remaining = new LinkedHashMap<>(original);
        Map<String, Map<Integer, String>> groups = new LinkedHashMap<>();
        Set<Integer> removed = new HashSet<>();

        while (!remaining.isEmpty()) {
            Map<String, Set<Integer>> wordToIndices = new HashMap<>();

            for (var entry : remaining.entrySet()) {
                for (String word : tokenize(entry.getValue(), minWordLength)) {
                    wordToIndices.computeIfAbsent(word, k -> new LinkedHashSet<>())
                                 .add(entry.getKey());
                }
            }

            String bestWord = null;
            int bestCount = 0;
            for (var entry : wordToIndices.entrySet()) {
                int count = entry.getValue().size();
                if (count >= minimumGroupSize && count > bestCount) {
                    bestCount = count;
                    bestWord = entry.getKey();
                }
            }

            if (bestWord == null) break;

            Map<Integer, String> group = new LinkedHashMap<>();
            for (Integer idx : wordToIndices.get(bestWord)) {
                group.put(idx, remaining.get(idx));
            }

            groups.put(bestWord, group);
            removed.addAll(group.keySet());
            remaining.keySet().removeAll(group.keySet());
        }

        Map<Integer, String> ungrouped = new LinkedHashMap<>();
        for (var e : original.entrySet()) {
            if (!removed.contains(e.getKey())) {
                ungrouped.put(e.getKey(), e.getValue());
            }
        }

        return new ClusterResult(groups, ungrouped);
    }

    private Set<String> tokenize(String s, int minLength) {
        Set<String> words = new LinkedHashSet<>();
        for (String token : s.split("[^a-zA-Z0-9]+")) {
            String word = token.toLowerCase();
            if (word.length() >= Math.max(1, minLength)) {
                words.add(word);
            }
        }
        return words;
    }

    private Map<Integer, String> index(List<String> list) {
        Map<Integer, String> indexed = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            indexed.put(i, list.get(i));
        }
        return indexed;
    }
}
