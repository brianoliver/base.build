package build.base.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParseExceptionTests {

    @Test
    void messageDescribesExpectedAndFoundWhenThereIsNoCause() {
        final var exception = new ParseException(null, "}", "<end of input>");

        assertThat(exception.getMessage())
            .isNotNull()
            .contains("}")
            .contains("<end of input>");
    }

    @Test
    void messageIncludesLocationWhenKnown() {
        final var scanner = new Scanner("a\nbcd");
        scanner.consume("a\nb");

        final var exception = new ParseException(scanner.getLocation(), "x", "c");

        assertThat(exception.getMessage()).contains("line 2").contains("column 2");
    }

    @Test
    void messageFromScannerFailureIsNeverNull() {
        assertThatThrownBy(() -> new Scanner("abc").consume("xyz"))
            .isInstanceOf(ParseException.class)
            .satisfies(e -> assertThat(e.getMessage()).isNotNull().contains("xyz"));
    }

    @Test
    void toStringIsConsistentWithMessage() {
        final var exception = new ParseException(null, "}", "<end of input>");

        assertThat(exception.toString()).contains(exception.getMessage());
    }
}
