package nl.multicode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

class NgramClustererTest {

    private final TextClusterer clusterer = new NgramClusterer();

    @Test
    void groupsBySharedTrigram() {
        var result = clusterer.cluster(
                List.of("football", "footprint", "basketball"),
                3, 2
        );

        assertThat(result.hasGroups()).isTrue();
        assertThat(result.getGroups()).isNotEmpty();

        String key = result.getGroups().keySet().iterator().next();
        result.getGroups().get(key).values()
              .forEach(s -> assertThat(s).contains(key));
    }

    @Test
    void noGroupWhenNothingShared() {
        var result = clusterer.cluster(
                List.of("alpha", "bravo", "charlie"),
                4, 2
        );

        assertThat(result.hasGroups()).isFalse();
        assertThat(result.getUngrouped()).hasSize(3);
    }

    @Test
    void emptyInputReturnsEmptyResult() {
        var result = clusterer.cluster(List.of(), 3, 2);

        assertThat(result.getGroups()).isEmpty();
        assertThat(result.getUngrouped()).isEmpty();
    }

    @Test
    void respectsMinimumGroupSize() {
        var result = clusterer.cluster(
                List.of("abc123", "unrelated"),
                3, 3
        );

        assertThat(result.hasGroups()).isFalse();
    }
}
