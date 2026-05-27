package nl.multicode;

import nl.multicode.testdata.TestData;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AutoGroupPerformanceTest is a test class designed to validate the performance
 * of the AutoGroup functionality. It includes tests to ensure that the
 * processing of large input data completes within a reasonable time frame.
 */
class AutoGroupPerformanceTest {

    private final AutoGroup autoGroup = new AutoGroup();

    @Test
    void largeInputCompletesWithinReasonableTime() {
        final var input = TestData.largeSimilarSentences(500, 80);

        assertThat(
                Duration.ofMillis(
                        measureMillis(() ->
                                autoGroup.autoGroupSentences(input, 5, 3)
                        )
                )
        ).isLessThan(Duration.ofMillis(200));
    }

    private long measureMillis(Runnable r) {
        long start = System.nanoTime();
        r.run();
        return (System.nanoTime() - start) / 1_000_000;
    }
}
