package io.keede.mombti.core.domains.report.model;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.util.List;
import java.util.Objects;

/** 개선할 부분 하나. {@code priority}가 작을수록 먼저 보인다. */
public record Improvement(String title, String body, ImprovementKind kind, int priority, List<Evidence> evidence) {

    public Improvement {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(kind, "kind");
        evidence = List.copyOf(evidence);
    }
}
