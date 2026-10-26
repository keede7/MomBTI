package io.keede.mombti.core.domains.report.model;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.util.List;
import java.util.Objects;

/** 화면에 나가는 값 + 기본값으로 계산했는지 여부 + 출처. {@code estimated}면 화면에 "추정" 태그. */
public record Estimated<T>(T value, boolean estimated, List<Evidence> evidence) {

    public Estimated {
        Objects.requireNonNull(value, "value");
        evidence = List.copyOf(evidence);
    }

    public static <T> Estimated<T> provided(T value, Evidence... evidence) {
        return new Estimated<>(value, false, List.of(evidence));
    }

    public static <T> Estimated<T> estimated(T value, Evidence... evidence) {
        return new Estimated<>(value, true, List.of(evidence));
    }
}
