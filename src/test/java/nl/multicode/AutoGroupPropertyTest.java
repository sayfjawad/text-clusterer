package nl.multicode;

import net.jqwik.api.*;

import java.util.*;
import net.jqwik.api.constraints.IntRange;
import nl.multicode.model.AutoGroupResult;

import static org.assertj.core.api.Assertions.assertThat;

class AutoGroupPropertyTest {

    private final AutoGroup autoGroup = new AutoGroup();

    @Property
    void groupKeysAreSubstringsOfAllGroupedSentences(
            @ForAll("sentences") List<String> sentences,
            @ForAll @IntRange(min = 2, max = 4) int minGroup
    ) {
        Assume.that(sentences.size() >= minGroup);

        final var result =
                autoGroup.autoGroupSentences(sentences, 2, minGroup);

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
