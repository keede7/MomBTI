package io.keede.mombti.web.support;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 숫자 입력 정규화 (E-10). 전각 숫자, 공백, 쉼표 소수점("72,5"), 단위 문자("175cm", "30세")를 허용한다.
 * 실패하면 {@link IllegalArgumentException}을 던져 바인딩 오류(typeMismatch)가 된다.
 * 예외 메시지에는 입력값을 담지 않는다.
 */
public final class NumberNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0]+");
    private static final Pattern UNIT_SUFFIX = Pattern.compile("(?i)(cm|kg|m|%|세|살)$");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private NumberNormalizer() {
    }

    public static Optional<BigDecimal> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = WHITESPACE.matcher(Normalizer.normalize(raw, Normalizer.Form.NFKC)).replaceAll("");
        text = UNIT_SUFFIX.matcher(text).replaceFirst("");
        if (text.isEmpty()) {
            return Optional.empty();
        }
        if (text.indexOf(',') >= 0) {
            if (text.indexOf('.') >= 0 || text.indexOf(',') != text.lastIndexOf(',')) {
                throw invalid();
            }
            text = text.replace(',', '.');
        }
        if (!NUMBER.matcher(text).matches()) {
            throw invalid();
        }
        return Optional.of(new BigDecimal(text));
    }

    public static Optional<Integer> normalizeInteger(String raw) {
        return normalize(raw).map(value -> {
            try {
                return value.stripTrailingZeros().intValueExact();
            } catch (ArithmeticException e) {
                throw invalid();
            }
        });
    }

    public static PropertyEditor bigDecimalEditor() {
        return new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(normalize(text).orElse(null));
            }

            @Override
            public String getAsText() {
                return getValue() instanceof BigDecimal value ? value.toPlainString() : "";
            }
        };
    }

    public static PropertyEditor integerEditor() {
        return new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(normalizeInteger(text).orElse(null));
            }

            @Override
            public String getAsText() {
                return getValue() instanceof Integer value ? value.toString() : "";
            }
        };
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("숫자 형식이 아님");
    }
}
