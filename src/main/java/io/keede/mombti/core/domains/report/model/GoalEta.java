package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;

/** 목표 도달 예측. */
public sealed interface GoalEta {

    record Reached(int week) implements GoalEta {
    }

    record Unreachable(BigDecimal week52WeightKg, BigDecimal equilibriumWeightKg) implements GoalEta {
    }

    record NotApplicable() implements GoalEta {
    }
}
