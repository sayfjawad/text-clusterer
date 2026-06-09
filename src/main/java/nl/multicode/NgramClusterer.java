package nl.multicode;

import nl.multicode.model.ClusterResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clusters strings by shared character n-grams.
 * The group key is the n-gram shared by the most strings in each round.
 * {@code minimalDenominatorLength} controls n (the gram size).
 */
public class NgramClusterer implements TextClusterer {

    @Override
    public ClusterResult cluster(List<String> input, int n, int minimumGroupSize) {
        if (input == null || input.isEmpty()) {
            return new ClusterResult(Map.of(), Map.of());
        }

        int gramSize = Math.max(1, n);
        Map<Integer, String> original = index(input);
        Map<Integer, String> remaining = new LinkedHashMap<>(original);
        Map<String, Map<Integer, String>> groups = new LinkedHashMap<>();
        Set<Integer> removed = new HashSet<>();

        while (!remaining.isEmpty()) {
            Map<String, Set<Integer>> ngramToIndices = new HashMap<>();

            for (var entry : remaining.entrySet()) {
                for (String gram : ngrams(entry.getValue(), gramSize)) {
                    ngramToIndices.computeIfAbsent(gram, k -> new LinkedHashSet<>())
                                  .add(entry.getKey());
                }
            }

            String bestGram = null;
            int bestCount = 0;
            for (var entry : ngramToIndices.entrySet()) {
                int count = entry.getValue().size();
                if (count >= minimumGroupSize && count > bestCount) {
                    bestCount = count;
                    bestGram = entry.getKey();
                }
            }

            if (bestGram == null) break;

            Map<Integer, String> group = new LinkedHashMap<>();
            for (Integer idx : ngramToIndices.get(bestGram)) {
                group.put(idx, remaining.get(idx));
            }

            groups.put(bestGram, group);
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

    private Set<String> ngrams(String s, int n) {
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i <= s.length() - n; i++) {
            result.add(s.substring(i, i + n));
        }
        return result;
    }

    private Map<Integer, String> index(List<String> list) {
        Map<Integer, String> indexed = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            indexed.put(i, list.get(i));
        }
        return indexed;
    }
}
