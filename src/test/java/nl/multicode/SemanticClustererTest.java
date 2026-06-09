package nl.multicode;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticClustererTest {

    private final TextClusterer clusterer = new SemanticClusterer();

    @Test
    void groupsBySharedWord() {
        var result = clusterer.cluster(
                List.of("Order created", "Order failed", "Payment processed"),
                3, 2
        );

        assertThat(result.hasGroups()).isTrue();
        assertThat(result.getGroups()).containsKey("order");
        assertThat(result.getGroups().get("order")).hasSize(2);
        assertThat(result.getUngrouped()).hasSize(1);
    }

    @Test
    void respectsMinWordLength() {
        var result = clusterer.cluster(
                List.of("I am ok", "I am fine"),
                4, 2
        );

        // "i", "am" are too short (< 4), no grouping key survives
        assertThat(result.hasGroups()).isFalse();
    }

    @Test
    void emptyInputReturnsEmptyResult() {
        var result = clusterer.cluster(List.of(), 3, 2);

        assertThat(result.getGroups()).isEmpty();
        assertThat(result.getUngrouped()).isEmpty();
    }

    @Test
    void caseInsensitiveTokenization() {
        var result = clusterer.cluster(
                List.of("ERROR detected", "error logged", "timeout"),
                3, 2
        );

        assertThat(result.hasGroups()).isTrue();
        assertThat(result.getGroups()).containsKey("error");
    }
}
