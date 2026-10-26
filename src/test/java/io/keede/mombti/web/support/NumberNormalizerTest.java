package io.keede.mombti.web.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NumberNormalizerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "72,5|72.5",
            "72.5|72.5",
            " 72.5 |72.5",
            "175cm|175",
            "175 cm|175",
            "175CM|175",
            "72.5kg|72.5",
            "25%|25",
            "30세|30",
            "1.75m|1.75",
            "７２|72",
    })
    void E10_쉼표_소수점_공백_단위_문자를_정규화(String raw, String expected) {
        assertThat(NumberNormalizer.normalize(raw)).hasValueSatisfying(
                value -> assertThat(value).isEqualByComparingTo(new BigDecimal(expected)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "cm"})
    void E10_비어_있으면_값_없음(String raw) {
        assertThat(NumberNormalizer.normalize(raw)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "72,5,1", "72.5.1", "1,000.5", "7 2a", "72kgcm", "1e3"})
    void E10_정규화해도_숫자가_아니면_예외_메시지에_입력값을_남기지_않음(String raw) {
        assertThatThrownBy(() -> NumberNormalizer.normalize(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(raw.strip()));
    }

    @ParameterizedTest
    @CsvSource({"30,30", "30세,30", "30.0,30"})
    void E10_나이는_정수로_정규화(String raw, int expected) {
        assertThat(NumberNormalizer.normalizeInteger(raw)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"30.5", "삼십"})
    void E10_나이가_정수가_아니면_예외(String raw) {
        assertThatThrownBy(() -> NumberNormalizer.normalizeInteger(raw)).isInstanceOf(IllegalArgumentException.class);
    }
}
