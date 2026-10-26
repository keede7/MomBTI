package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 목표 도달 예측 표의 한 행 (docs/PRD.md 리포트 구성 5).
 *
 * @param weeklyLossKg       첫 4주 평균 주당 감량
 * @param withinSafeRange    주당 감량이 현재 체중의 1% 이하 (HELMS_2014)
 */
public record ScenarioEta(Scenario scenario, GoalEtaRange eta, BigDecimal weeklyLossKg, boolean withinSafeRange) {

    public ScenarioEta {
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(eta, "eta");
        Objects.requireNonNull(weeklyLossKg, "weeklyLossKg");
    }
}
