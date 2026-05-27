package nl.multicode;

import nl.multicode.model.AutoGroupResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AutoGroupParameterizedTest {

    private final AutoGroup autoGroup = new AutoGroup();

    static Stream<TestCase> groupingCases() {
        return Stream.of(
                new TestCase(
                        List.of("foo bar baz", "foo bar qux"),
                        3, 2,
                        true
                ),
                new TestCase(
                        List.of("alpha", "beta", "gamma"),
                        3, 2,
                        false
                ),
                new TestCase(
                        List.of("abcDEFghi", "xyzDEFuvw"),
                        3, 2,
                        true
                )
        );
    }

    @ParameterizedTest
    @MethodSource("groupingCases")
    void groupingScenarios(TestCase tc) {
        final var result =
                autoGroup.autoGroupSentences(tc.input, tc.minLen, tc.minGroup);

        assertThat(result.hasGroups()).isEqualTo(tc.expectGroups);
    }

    record TestCase(
            List<String> input,
            int minLen,
            int minGroup,
            boolean expectGroups
    ) {}
}
