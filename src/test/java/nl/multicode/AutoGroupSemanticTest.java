package nl.multicode;

import nl.multicode.model.AutoGroupResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutoGroupSemanticTest {

    private final AutoGroup autoGroup = new AutoGroup();
    @Test
    void preservesPartialOverlapsNotTokenBased() {
        List<String> input = List.of(
                "processingPayment",
                "failedPaymentProcessing"
        );

        AutoGroupResult result =
                autoGroup.autoGroupSentences(input, 6, 2);

        assertThat(result.hasGroups()).isTrue();

        String key = result.getGroups().keySet().iterator().next();

        // Algorithmic guarantee
        assertThat(input.get(0)).contains(key);
        assertThat(input.get(1)).contains(key);
    }

    @Test
    void groupsByLongestSharedStructureNotMeaning() {
        List<String> input = List.of(
                "Error 504: Gateway timeout",
                "Error 502: Gateway bad response"
        );

        AutoGroupResult result =
                autoGroup.autoGroupSentences(input, 5, 2);

        String key = result.getGroups().keySet().iterator().next();

        assertThat(input.get(0)).contains(key);
        assertThat(input.get(1)).contains(key);
    }

}
