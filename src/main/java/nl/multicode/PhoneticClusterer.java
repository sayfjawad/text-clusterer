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
 * Clusters strings by phonetic similarity using the Soundex algorithm.
 * The group key is the Soundex code shared by the most strings in each round.
 * {@code minimalDenominatorLength} is the minimum word length to phonetically encode.
 */
public class PhoneticClusterer implements TextClusterer {

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
            Map<String, Set<Integer>> codeToIndices = new HashMap<>();

            for (var entry : remaining.entrySet()) {
                for (String code : soundexCodes(entry.getValue(), minWordLength)) {
                    codeToIndices.computeIfAbsent(code, k -> new LinkedHashSet<>())
                                 .add(entry.getKey());
                }
            }

            String bestCode = null;
            int bestCount = 0;
            for (var entry : codeToIndices.entrySet()) {
                int count = entry.getValue().size();
                if (count >= minimumGroupSize && count > bestCount) {
                    bestCount = count;
                    bestCode = entry.getKey();
                }
            }

            if (bestCode == null) break;

            Map<Integer, String> group = new LinkedHashMap<>();
            for (Integer idx : codeToIndices.get(bestCode)) {
                group.put(idx, remaining.get(idx));
            }

            groups.put(bestCode, group);
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

    private Set<String> soundexCodes(String s, int minWordLength) {
        Set<String> codes = new LinkedHashSet<>();
        for (String word : s.split("[^a-zA-Z]+")) {
            if (word.length() >= Math.max(1, minWordLength)) {
                codes.add(soundex(word));
            }
        }
        return codes;
    }

    // Soundex: keep first letter, encode consonants as digits, collapse adjacent,
    // strip vowels/H/W/Y, pad/truncate to length 4.
    String soundex(String word) {
        if (word == null || word.isEmpty()) return "";

        String upper = word.toUpperCase();
        char first = upper.charAt(0);
        StringBuilder code = new StringBuilder().append(first);
        char prev = digitFor(first);

        for (int i = 1; i < upper.length() && code.length() < 4; i++) {
            char c = upper.charAt(i);
            char digit = digitFor(c);
            if (digit == '0') {
                prev = '0';
                continue;
            }
            if (digit != prev) {
                code.append(digit);
                prev = digit;
            }
        }

        while (code.length() < 4) code.append('0');
        return code.toString();
    }

    private char digitFor(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'B', 'F', 'P', 'V'             -> '1';
            case 'C', 'G', 'J', 'K', 'Q', 'S',
                 'X', 'Z'                        -> '2';
            case 'D', 'T'                        -> '3';
            case 'L'                             -> '4';
            case 'M', 'N'                        -> '5';
            case 'R'                             -> '6';
            default                              -> '0';
        };
    }

    private Map<Integer, String> index(List<String> list) {
        Map<Integer, String> indexed = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            indexed.put(i, list.get(i));
        }
        return indexed;
    }
}
