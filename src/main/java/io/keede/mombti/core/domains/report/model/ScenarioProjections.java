package io.keede.mombti.core.domains.report.model;

import java.util.Objects;

/**
 * 시나리오 하나의 예측 묶음.
 *
 * @param base      기본 예측
 * @param bmrLow    기초대사량 × 0.9 (개인차 밴드에서 무거운 쪽, FRANKENFIELD_2005)
 * @param bmrHigh   기초대사량 × 1.1 (개인차 밴드에서 가벼운 쪽)
 * @param adherence 계획의 80%만 지킨 경우 (ADHERENCE_80, 가정)
 */
public record ScenarioProjections(
        Scenario scenario,
        Projection base,
        Projection bmrLow,
        Projection bmrHigh,
        Projection adherence
) {
    public ScenarioProjections {
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(bmrLow, "bmrLow");
        Objects.requireNonNull(bmrHigh, "bmrHigh");
        Objects.requireNonNull(adherence, "adherence");
    }
}
