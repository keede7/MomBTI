package io.keede.mombti.core.domains.report.model;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.util.List;
import java.util.Objects;

public record Faq(String question, String answer, List<Evidence> evidence) {

    public Faq {
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(answer, "answer");
        evidence = List.copyOf(evidence);
    }
}
