package io.keede.mombti.core.domains.report.model;

import java.util.Objects;

/**
 * 개인차 밴드를 반영한 목표 도달 예측 (ADR-013, "18~23주").
 *
 * @param expected 기본 예측
 * @param earliest 기초대사량 × 1.1 예측 (가장 빠른 경우)
 * @param latest   기초대사량 × 0.9 예측 (가장 늦은 경우)
 */
public record GoalEtaRange(GoalEta expected, GoalEta earliest, GoalEta latest) {

    public GoalEtaRange {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(earliest, "earliest");
        Objects.requireNonNull(latest, "latest");
    }
}
