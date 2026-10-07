package build.base.parsing;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class FilterTests {

    @Test
    void shouldSkipTerminatedMultilineComment() {
        final var input = new StringInput("/* hi */rest");

        Filter.JAVA_MULTILINE_COMMENT.accept(input);

        assertThat(input.peekMaximum().toString()).isEqualTo("rest");
    }

    @Test
    void shouldRejectUnterminatedMultilineCommentInsteadOfHanging() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
            assertThatThrownBy(() -> Filter.JAVA_MULTILINE_COMMENT.accept(new StringInput("/* never closed")))
                .isInstanceOf(ParseException.class));
    }
}
