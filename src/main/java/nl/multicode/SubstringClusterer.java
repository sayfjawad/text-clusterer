package nl.multicode;

import nl.multicode.model.ClusterResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exact substring-based clustering using a Generalized Suffix Automaton (SAM).
 * Preserves the "cuts parts" power of the original PHP approach, but avoids
 * enumerating all substrings.
 */
public class SubstringClusterer implements TextClusterer {

    @Override
    public ClusterResult cluster(List<String> input,
            int minimalDenominatorLength,
            int minimumGroupSize) {

        if (input == null || input.isEmpty()) {
            return new ClusterResult(Map.of(), Map.of());
        }

        Map<Integer, String> original = index(input);
        Map<Integer, String> retry = new LinkedHashMap<>(original);

        Map<String, Map<Integer, String>> groups = new LinkedHashMap<>();
        Set<Integer> removed = new HashSet<>();

        while (!retry.isEmpty()) {

            Set<String> longestKeys = findLongestKeysExact(
                    retry,
                    minimalDenominatorLength,
                    minimumGroupSize
            );

            if (longestKeys.isEmpty()) {
                break;
            }

            Map<String, Map<Integer, String>> foundThisRound = new LinkedHashMap<>();
            for (String key : longestKeys) {
                Map<Integer, String> group = new LinkedHashMap<>();
                for (var e : retry.entrySet()) {
                    if (e.getValue().contains(key)) {
                        group.put(e.getKey(), e.getValue());
                    }
                }
                if (group.size() >= minimumGroupSize) {
                    foundThisRound.put(key, group);
                    removed.addAll(group.keySet());
                }
            }

            if (foundThisRound.isEmpty()) {
                break;
            }

            groups.putAll(foundThisRound);

            for (Integer idx : removed) {
                retry.remove(idx);
            }
        }

        Map<Integer, String> ungrouped = new LinkedHashMap<>();
        for (var e : original.entrySet()) {
            if (!removed.contains(e.getKey())) {
                ungrouped.put(e.getKey(), e.getValue());
            }
        }

        return new ClusterResult(groups, ungrouped);
    }

    private Set<String> findLongestKeysExact(Map<Integer, String> retryList,
            int minimalDenominatorLength,
            int minimumGroupSize) {

        List<String> sentences = new ArrayList<>(retryList.values());

        if (sentences.size() < minimumGroupSize) {
            return Set.of();
        }

        GeneralizedSuffixAutomaton sam = new GeneralizedSuffixAutomaton(sentences.size());
        StringBuilder global = new StringBuilder();

        for (int sid = 0; sid < sentences.size(); sid++) {
            String s = sentences.get(sid);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                global.append(c);
                sam.extend(c, sid, global.length() - 1);
            }
            char sep = (char) (0xE000 + (sid % 0x1FFF));
            global.append(sep);
            sam.resetWithSeparator(sep, global.length() - 1);
        }

        sam.propagateOccurrences();

        int bestTrimLen = 0;
        for (GeneralizedSuffixAutomaton.State st : sam.states) {
            if (st == sam.states.get(0)) continue;
            if (st.occ.cardinality() < minimumGroupSize) continue;

            String raw = substringSafe(global, st.firstPos - st.len + 1, st.firstPos + 1);
            String key = raw.trim();

            if (key.length() >= minimalDenominatorLength) {
                bestTrimLen = Math.max(bestTrimLen, key.length());
            }
        }

        if (bestTrimLen < minimalDenominatorLength) {
            return Set.of();
        }

        Set<String> keys = new LinkedHashSet<>();
        for (GeneralizedSuffixAutomaton.State st : sam.states) {
            if (st == sam.states.get(0)) continue;
            if (st.occ.cardinality() < minimumGroupSize) continue;

            String raw = substringSafe(global, st.firstPos - st.len + 1, st.firstPos + 1);
            String key = raw.trim();

            if (key.length() == bestTrimLen && !containsPrivateUseSeparator(key)) {
                keys.add(key);
            }
        }

        return keys;
    }

    private static boolean containsPrivateUseSeparator(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0xE000 && c <= 0xF8FF) return true;
        }
        return false;
    }

    private static String substringSafe(CharSequence s, int start, int endExclusive) {
        int a = Math.max(0, start);
        int b = Math.min(s.length(), endExclusive);
        if (a >= b) return "";
        return s.subSequence(a, b).toString();
    }

    private Map<Integer, String> index(List<String> list) {
        Map<Integer, String> indexed = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            indexed.put(i, list.get(i));
        }
        return indexed;
    }
}
