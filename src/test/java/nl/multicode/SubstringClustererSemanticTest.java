package nl.multicode;

import nl.multicode.model.ClusterResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubstringClustererSemanticTest {

    private final TextClusterer clusterer = new SubstringClusterer();

    @Test
    void preservesPartialOverlapsNotTokenBased() {
        List<String> input = List.of(
                "processingPayment",
                "failedPaymentProcessing"
        );

        ClusterResult result = clusterer.cluster(input, 6, 2);

        assertThat(result.hasGroups()).isTrue();

        String key = result.getGroups().keySet().iterator().next();

        assertThat(input.get(0)).contains(key);
        assertThat(input.get(1)).contains(key);
    }

    @Test
    void groupsByLongestSharedStructureNotMeaning() {
        List<String> input = List.of(
                "Error 504: Gateway timeout",
                "Error 502: Gateway bad response"
        );

        ClusterResult result = clusterer.cluster(input, 5, 2);

        String key = result.getGroups().keySet().iterator().next();

        assertThat(input.get(0)).contains(key);
        assertThat(input.get(1)).contains(key);
    }
}
