package nl.multicode;

import java.util.*;
import nl.multicode.model.AutoGroupResult;

/**
 * Exact substring-based grouping using a Generalized Suffix Automaton (SAM).
 * Preserves the "cuts parts" power of the original PHP approach, but avoids
 * enumerating all substrings.
 */
public class AutoGroup {

    /**
     * Groups sentences based on the longest common substring(s) that occur
     * in at least {@code minimumGroupSize} sentences.
     *
     * @param input list of sentences
     * @param minimalDenominatorLength minimal denominator length (post-trim)
     * @param minimumGroupSize minimum number of sentences for a valid group
     */
    public AutoGroupResult autoGroupSentences(List<String> input,
            int minimalDenominatorLength,
            int minimumGroupSize) {

        if (input == null || input.isEmpty()) {
            return new AutoGroupResult(Map.of(), Map.of());
        }

        Map<Integer, String> original = index(input);
        Map<Integer, String> retry = new LinkedHashMap<>(original);

        Map<String, Map<Integer, String>> groups = new LinkedHashMap<>();
        Set<Integer> removed = new HashSet<>();

        while (!retry.isEmpty()) {

            // Find the longest denominator keys (exact, substring-based)
            Set<String> longestKeys = findLongestKeysExact(
                    retry,
                    minimalDenominatorLength,
                    minimumGroupSize
            );

            if (longestKeys.isEmpty()) {
                break;
            }

            // Create groups for each longest key, like PHP
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
                // No usable groups from these keys; stop.
                break;
            }

            groups.putAll(foundThisRound);

            // Remove grouped sentences and continue searching with the rest
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

        return new AutoGroupResult(groups, ungrouped);
    }

    /* ========================= Exact key search (SAM) ========================= */

    private Set<String> findLongestKeysExact(Map<Integer, String> retryList,
            int minimalDenominatorLength,
            int minimumGroupSize) {

        // Map original sentence indices -> compact [0..n-1] ids for BitSet
        List<Integer> ids = new ArrayList<>(retryList.keySet());
        List<String> sentences = new ArrayList<>(retryList.values());

        if (sentences.size() < minimumGroupSize) {
            return Set.of();
        }

        // Build a generalized suffix automaton over all sentences with separators
        GeneralizedSuffixAutomaton sam = new GeneralizedSuffixAutomaton(sentences.size());

        // Build a global text for substring reconstruction
        StringBuilder global = new StringBuilder();

        for (int sid = 0; sid < sentences.size(); sid++) {
            String s = sentences.get(sid);

            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                global.append(c);
                sam.extend(c, sid, global.length() - 1);
            }

            // Add a unique separator char to prevent cross-sentence substrings
            // Use Private Use Area characters.
            char sep = (char) (0xE000 + (sid % 0x1FFF));
            global.append(sep);
            sam.resetWithSeparator(sep, global.length() - 1);
        }

        sam.propagateOccurrences();

        // Find best (max) trimmed length among substrings occurring in >= minimumGroupSize sentences
        int bestTrimLen = 0;
        for (GeneralizedSuffixAutomaton.State st : sam.states) {
            if (st == sam.states.get(0)) continue; // skip initial
            if (st.occ.cardinality() < minimumGroupSize) continue;

            // representative longest substring for this state is length=st.len ending at st.firstPos
            String raw = substringSafe(global, st.firstPos - st.len + 1, st.firstPos + 1);
            String key = raw.trim();

            if (key.length() >= minimalDenominatorLength) {
                bestTrimLen = Math.max(bestTrimLen, key.length());
            }
        }

        if (bestTrimLen < minimalDenominatorLength) {
            return Set.of();
        }

        // Collect all distinct keys whose trimmed length == bestTrimLen and occur in >= minimumGroupSize sentences
        Set<String> keys = new LinkedHashSet<>();
        for (GeneralizedSuffixAutomaton.State st : sam.states) {
            if (st == sam.states.get(0)) continue;
            if (st.occ.cardinality() < minimumGroupSize) continue;

            String raw = substringSafe(global, st.firstPos - st.len + 1, st.firstPos + 1);
            String key = raw.trim();

            if (key.length() == bestTrimLen) {
                // Defensive: exclude keys that might contain separator (should not happen)
                if (!containsPrivateUseSeparator(key)) {
                    keys.add(key);
                }
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

    /* ========================= Generalized Suffix Automaton ========================= */

    static final class GeneralizedSuffixAutomaton {

        static final class State {
            int len;
            int link;
            Map<Character, Integer> next = new HashMap<>();
            BitSet occ;          // which sentence-ids contain substrings of this state
            int firstPos;        // a position of an occurrence end in the global text

            State(int sentenceCount) {
                this.occ = new BitSet(sentenceCount);
            }
        }

        final List<State> states = new ArrayList<>();
        private final int sentenceCount;
        private int last;

        GeneralizedSuffixAutomaton(int sentenceCount) {
            this.sentenceCount = sentenceCount;
            State init = new State(sentenceCount);
            init.len = 0;
            init.link = -1;
            init.firstPos = -1;
            states.add(init);
            last = 0;
        }

        void extend(char c, int sentenceId, int globalPos) {
            int cur = states.size();
            State curSt = new State(sentenceCount);
            curSt.len = states.get(last).len + 1;
            curSt.firstPos = globalPos;
            curSt.occ.set(sentenceId);
            states.add(curSt);

            int p = last;
            while (p != -1 && !states.get(p).next.containsKey(c)) {
                states.get(p).next.put(c, cur);
                p = states.get(p).link;
            }

            if (p == -1) {
                curSt.link = 0;
            } else {
                int q = states.get(p).next.get(c);
                State qSt = states.get(q);

                if (states.get(p).len + 1 == qSt.len) {
                    curSt.link = q;
                } else {
                    int clone = states.size();
                    State cloneSt = new State(sentenceCount);
                    cloneSt.len = states.get(p).len + 1;
                    cloneSt.next = new HashMap<>(qSt.next);
                    cloneSt.link = qSt.link;
                    cloneSt.firstPos = qSt.firstPos;
                    // IMPORTANT: clone does not get direct occ marks here; it will receive them via propagation
                    states.add(cloneSt);

                    while (p != -1 && states.get(p).next.getOrDefault(c, -1) == q) {
                        states.get(p).next.put(c, clone);
                        p = states.get(p).link;
                    }

                    qSt.link = clone;
                    curSt.link = clone;
                }
            }

            last = cur;
        }

        /**
         * Adds a separator to break substrings across sentences, then resets "last"
         * so next sentence starts fresh.
         */
        void resetWithSeparator(char sep, int globalPos) {
            // Add the separator as a character transition, but do NOT mark occurrences for any sentence.
            // This avoids cross-sentence substrings.
            extendSeparator(sep, globalPos);
            last = 0;
        }

        private void extendSeparator(char c, int globalPos) {
            int cur = states.size();
            State curSt = new State(sentenceCount);
            curSt.len = states.get(last).len + 1;
            curSt.firstPos = globalPos;
            // no occ set for separators
            states.add(curSt);

            int p = last;
            while (p != -1 && !states.get(p).next.containsKey(c)) {
                states.get(p).next.put(c, cur);
                p = states.get(p).link;
            }

            if (p == -1) {
                curSt.link = 0;
            } else {
                int q = states.get(p).next.get(c);
                State qSt = states.get(q);

                if (states.get(p).len + 1 == qSt.len) {
                    curSt.link = q;
                } else {
                    int clone = states.size();
                    State cloneSt = new State(sentenceCount);
                    cloneSt.len = states.get(p).len + 1;
                    cloneSt.next = new HashMap<>(qSt.next);
                    cloneSt.link = qSt.link;
                    cloneSt.firstPos = qSt.firstPos;
                    states.add(cloneSt);

                    while (p != -1 && states.get(p).next.getOrDefault(c, -1) == q) {
                        states.get(p).next.put(c, clone);
                        p = states.get(p).link;
                    }

                    qSt.link = clone;
                    curSt.link = clone;
                }
            }

            last = cur;
        }

        /**
         * Propagate occurrence BitSets from longer states to their suffix links.
         * This yields exact per-state sentence coverage: if a state represents substrings
         * occurring in certain sentences, occ has those sentence IDs.
         */
        void propagateOccurrences() {
            int maxLen = 0;
            for (State st : states) maxLen = Math.max(maxLen, st.len);

            int[] cnt = new int[maxLen + 1];
            for (State st : states) cnt[st.len]++;

            for (int i = 1; i <= maxLen; i++) cnt[i] += cnt[i - 1];

            int[] order = new int[states.size()];
            for (int i = states.size() - 1; i >= 0; i--) {
                State st = states.get(i);
                order[--cnt[st.len]] = i;
            }

            // process in decreasing length
            for (int i = order.length - 1; i > 0; i--) {
                int v = order[i];
                int link = states.get(v).link;
                if (link >= 0) {
                    states.get(link).occ.or(states.get(v).occ);
                }
            }
        }
    }
}
