package nl.multicode;

import java.util.*;
import nl.multicode.model.AutoGroupResult;

/**
 * Type-safe Java implementation of AutoGroup
 *
 * Author: Sayf Jawad
 */
public class AutoGroupBruteForce {

    public AutoGroupResult autoGroupSentences(
            List<String> input,
            int minimalDenominatorLength,
            int minimumGroupSize
    ) {

        if (input == null || input.isEmpty()) {
            return new AutoGroupResult(Map.of(), Map.of());
        }

        Map<Integer, String> original = index(input);
        Map<Integer, String> retryList = new LinkedHashMap<>(original);

        Map<String, Map<Integer, String>> groups = new LinkedHashMap<>();
        Set<Integer> removedIndexes = new HashSet<>();

        while (!retryList.isEmpty()) {

            Set<String> possibleKeys =
                    getKeyList(retryList.values(), minimalDenominatorLength);

            Set<String> validKeys =
                    getValidKeys(possibleKeys, retryList.values(), minimumGroupSize);

            if (validKeys.isEmpty()) {
                break;
            }

            Set<String> longestKeys = getLongestKeys(validKeys);

            Map<String, Map<Integer, String>> foundGroups =
                    getGroup(longestKeys, retryList);

            if (foundGroups.isEmpty()) {
                break;
            }

            for (var entry : foundGroups.entrySet()) {
                groups.put(entry.getKey(), entry.getValue());
                removedIndexes.addAll(entry.getValue().keySet());
            }

            retryList.keySet().removeAll(removedIndexes);
        }

        Map<Integer, String> ungrouped = new LinkedHashMap<>();
        for (var entry : original.entrySet()) {
            if (!removedIndexes.contains(entry.getKey())) {
                ungrouped.put(entry.getKey(), entry.getValue());
            }
        }

        return new AutoGroupResult(groups, ungrouped);
    }

    /* ===================== Internals ===================== */

    private Set<String> getKeyList(Collection<String> sentences, int minLength) {
        int minKeyLength = Math.max(4, minLength);
        Set<String> keys = new HashSet<>();

        for (String sentence : sentences) {
            for (int start = 0; start < sentence.length(); start++) {
                for (int len = minKeyLength; start + len <= sentence.length(); len++) {
                    keys.add(sentence.substring(start, start + len).trim());
                }
            }
        }
        return keys;
    }

    private Set<String> getValidKeys(
            Set<String> keys,
            Collection<String> sentences,
            int minimumGroupSize
    ) {
        Set<String> valid = new HashSet<>();

        for (String key : keys) {
            long count = sentences.stream()
                    .filter(s -> s.contains(key))
                    .count();

            if (count >= minimumGroupSize) {
                valid.add(key);
            }
        }
        return valid;
    }

    private Set<String> getLongestKeys(Set<String> keys) {
        int maxLength = keys.stream()
                .mapToInt(String::length)
                .max()
                .orElse(0);

        Set<String> longest = new HashSet<>();
        for (String key : keys) {
            if (key.length() == maxLength) {
                longest.add(key);
            }
        }
        return longest;
    }

    private Map<String, Map<Integer, String>> getGroup(
            Set<String> keys,
            Map<Integer, String> sentences
    ) {
        Map<String, Map<Integer, String>> groups = new LinkedHashMap<>();

        for (String key : keys) {
            Map<Integer, String> group = new LinkedHashMap<>();
            for (var entry : sentences.entrySet()) {
                if (entry.getValue().contains(key)) {
                    group.put(entry.getKey(), entry.getValue());
                }
            }
            if (!group.isEmpty()) {
                groups.put(key, group);
            }
        }
        return groups;
    }

    private Map<Integer, String> index(List<String> list) {
        Map<Integer, String> indexed = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            indexed.put(i, list.get(i));
        }
        return indexed;
    }
}

