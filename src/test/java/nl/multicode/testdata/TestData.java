package nl.multicode.testdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TestData {

    private static final Random RANDOM = new Random(42); // deterministic

    private TestData() {
        // utility class
    }

    /**
     * Generates a list of sentences that all share a long common substring,
     * plus some small variation to avoid trivial equality.
     *
     * @param count         number of sentences
     * @param sentenceSize  approximate length of each sentence
     */
    public static List<String> largeSimilarSentences(int count, int sentenceSize) {
        final var common = randomAlpha(sentenceSize / 2);

        final var result = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            result.add(common + "-" + randomAlpha(sentenceSize / 2));
        }
        return result;
    }

    /**
     * Generates completely random sentences (low chance of grouping).
     */
    public static List<String> randomSentences(int count, int minLen, int maxLen) {
        final var result = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            int len = minLen + RANDOM.nextInt(maxLen - minLen + 1);
            result.add(randomAlpha(len));
        }
        return result;
    }

    private static String randomAlpha(int len) {
        final var sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + RANDOM.nextInt(26)));
        }
        return sb.toString();
    }
}
