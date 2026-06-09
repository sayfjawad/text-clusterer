package nl.multicode;

import nl.multicode.model.ClusterResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubstringClustererTest {

    private final TextClusterer clusterer = new SubstringClusterer();

    @Test
    void groupsSentencesWithCommonSubstring() {
        List<String> input = List.of(
                "Order created successfully",
                "Order created with warnings",
                "Payment failed",
                "Order created and paid"
        );

        final var result = clusterer.cluster(input, 5, 2);

        assertThat(result.hasGroups()).isTrue();
        assertThat(result.getGroups()).hasSize(1);

        final var group = result.getGroups().entrySet().iterator().next();

        assertThat(group.getKey()).contains("Order created");
        assertThat(group.getValue()).hasSize(3);

        assertThat(result.getUngrouped())
                .hasSize(1)
                .containsEntry(2, "Payment failed");
    }

    @Test
    void stillGroupsOnMidWordOverlapCutsParts() {
        final var input = List.of(
                "abcDEFghi",
                "xyzDEFuvw",
                "no match here"
        );

        final var result = clusterer.cluster(input, 3, 2);

        assertThat(result.getGroups()).isNotEmpty();
        final var key = result.getGroups().keySet().iterator().next();

        assertThat(key).contains("DEF");
        assertThat(result.getGroups().values().iterator().next()).hasSize(2);

        assertThat(result.getUngrouped()).containsEntry(2, "no match here");
    }

    @Test
    void returnsAllUngroupedWhenNoCommonalityExists() {
        final var input = List.of(
                "Apple",
                "Banana",
                "Carrot"
        );

        ClusterResult result = clusterer.cluster(input, 4, 2);

        assertThat(result.hasGroups()).isFalse();
        assertThat(result.getGroups()).isEmpty();
        assertThat(result.getUngrouped()).hasSize(3);
    }

    @Test
    void emptyInputReturnsEmptyResult() {
        final var result = clusterer.cluster(List.of(), 4, 2);

        assertThat(result.getGroups()).isEmpty();
        assertThat(result.getUngrouped()).isEmpty();
    }

    @Test
    void respectsMinimumGroupSize() {
        List<String> input = List.of(
                "System error occurred",
                "System rebooted",
                "User logged in"
        );

        final var result = clusterer.cluster(input, 6, 3);

        assertThat(result.hasGroups()).isFalse();
        assertThat(result.getGroups()).isEmpty();
        assertThat(result.getUngrouped()).hasSize(3);
    }
}
