package nl.multicode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Generalized Suffix Automaton (GSA), a data structure for
 * efficiently handling and processing multiple sequences (e.g., strings or sentences)
 * in parallel. It supports operations such as string extension, resetting with separators,
 * and propagating occurrence information across its states.
 *
 * This implementation can be utilized in text clustering, pattern matching, or other
 * text analysis scenarios that require managing multiple strings in an efficient manner.
 */
final class GeneralizedSuffixAutomaton {

    static final class State {
        int len;
        int link;
        Map<Character, Integer> next = new HashMap<>();
        BitSet occ;
        int firstPos;

        State(int sentenceCount) {
            this.occ = new BitSet(sentenceCount);
        }
    }

    final List<State> states = new ArrayList<>();
    private final int sentenceCount;
    private int last;

    /**
     * Constructs a Generalized Suffix Automaton (GSA) for use with multiple sequences.
     * The automaton is initialized with a specified capacity, which determines its
     * ability to process and track multiple sequences in parallel.
     *
     * @param sentenceCount the number of sequences or sentences that the automaton will handle.
     */
    GeneralizedSuffixAutomaton(int sentenceCount) {
        this.sentenceCount = sentenceCount;
        State init = new State(sentenceCount);
        init.len = 0;
        init.link = -1;
        init.firstPos = -1;
        states.add(init);
        last = 0;
    }

    /**
     * Extends the Generalized Suffix Automaton (GSA) by adding a new transition based on the
     * provided character and updates the automaton's internal states accordingly. This method
     * ensures correct state linkage and handles necessary state cloning if required to maintain
     * the automaton's structure.
     *
     * @param c          the character to add to the automaton, representing the next extension.
     * @param sentenceId the identifier for the specific sequence or sentence associated with the
     *                   character being added.
     * @param globalPos  the global position of the character in the input across all sequences.
     */
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
     * Resets the state of the Generalized Suffix Automaton (GSA) with a specified separator
     * character. This method integrates the separator into the automaton structure and resets
     * the active state to the initial state for further processing.
     *
     * @param sep       the separator character to insert into the automaton.
     * @param globalPos the global position of the separator character in the input across all sequences.
     */
    void resetWithSeparator(char sep, int globalPos) {
        extendSeparator(sep, globalPos);
        last = 0;
    }

    /**
     * Extends the Generalized Suffix Automaton (GSA) by incorporating a transition with a separator
     * character. This method handles the creation of new states and updates the automaton's structure,
     * ensuring correct linkage and performing state cloning if necessary. The separator character is used
     * to signify boundaries between sequences or segments.
     *
     * @param c          the separator character to be integrated into the automaton structure.
     * @param globalPos  the global position of the separator character in the input across all sequences.
     */
    private void extendSeparator(char c, int globalPos) {
        int cur = states.size();
        State curSt = new State(sentenceCount);
        curSt.len = states.get(last).len + 1;
        curSt.firstPos = globalPos;
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

        for (int i = order.length - 1; i > 0; i--) {
            int v = order[i];
            int link = states.get(v).link;
            if (link >= 0) {
                states.get(link).occ.or(states.get(v).occ);
            }
        }
    }
}
