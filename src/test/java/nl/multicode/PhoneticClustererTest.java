package nl.multicode;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneticClustererTest {

    private final PhoneticClusterer clusterer = new PhoneticClusterer();

    @Test
    void groupsPhoneticallySimilarNames() {
        // Robert → R163, Rupert → R163
        var result = clusterer.cluster(
                List.of("Robert", "Rupert", "Alice"),
                3, 2
        );

        assertThat(result.hasGroups()).isTrue();
        assertThat(result.getGroups()).containsKey("R163");
        assertThat(result.getGroups().get("R163")).hasSize(2);
        assertThat(result.getUngrouped()).hasSize(1);
    }

    @Test
    void groupsAlternativeSpellings() {
        // Smith → S530, Smyth → S530
        var result = clusterer.cluster(
                List.of("Smith", "Smyth", "Jones"),
                3, 2
        );

        assertThat(result.hasGroups()).isTrue();
        assertThat(result.getGroups()).containsKey("S530");
    }

    @Test
    void soundexKnownValues() {
        assertThat(clusterer.soundex("Robert")).isEqualTo("R163");
        assertThat(clusterer.soundex("Rupert")).isEqualTo("R163");
        assertThat(clusterer.soundex("Smith")).isEqualTo("S530");
        assertThat(clusterer.soundex("Smyth")).isEqualTo("S530");
        assertThat(clusterer.soundex("Euler")).isEqualTo("E460");
        assertThat(clusterer.soundex("Ellery")).isEqualTo("E460");
    }

    @Test
    void emptyInputReturnsEmptyResult() {
        var result = clusterer.cluster(List.of(), 3, 2);

        assertThat(result.getGroups()).isEmpty();
        assertThat(result.getUngrouped()).isEmpty();
    }

    @Test
    void noGroupWhenNoPhoneticOverlap() {
        var result = clusterer.cluster(
                List.of("Alice", "Bob", "Charlie"),
                3, 2
        );

        assertThat(result.hasGroups()).isFalse();
        assertThat(result.getUngrouped()).hasSize(3);
    }
}
