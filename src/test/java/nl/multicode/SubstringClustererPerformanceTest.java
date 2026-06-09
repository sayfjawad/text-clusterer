package nl.multicode;

import nl.multicode.testdata.TestData;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SubstringClustererPerformanceTest {

    private final TextClusterer clusterer = new SubstringClusterer();

    @Test
    void largeInputCompletesWithinReasonableTime() {
        final var input = TestData.largeSimilarSentences(500, 80);

        assertThat(
                Duration.ofMillis(
                        measureMillis(() -> clusterer.cluster(input, 5, 3))
                )
        ).isLessThan(Duration.ofMillis(200));
    }

    private long measureMillis(Runnable r) {
        long start = System.nanoTime();
        r.run();
        return (System.nanoTime() - start) / 1_000_000;
    }
}
