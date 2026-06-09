package nl.multicode;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubstringClustererPropertyTest {

    private final TextClusterer clusterer = new SubstringClusterer();

    @Property
    void groupKeysAreSubstringsOfAllGroupedSentences(
            @ForAll("sentences") List<String> sentences,
            @ForAll @IntRange(min = 2, max = 4) int minGroup
    ) {
        Assume.that(sentences.size() >= minGroup);

        final var result = clusterer.cluster(sentences, 2, minGroup);

        result.getGroups().forEach((key, group) -> {
            group.values().forEach(sentence ->
                    assertThat(sentence).contains(key)
            );
            assertThat(group).hasSizeGreaterThanOrEqualTo(minGroup);
        });
    }

    @Provide
    final Arbitrary<List<String>> sentences() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20)
                .list()
                .ofMinSize(2)
                .ofMaxSize(10);
    }
}
