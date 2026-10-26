package io.keede.mombti.core.domains.report.simulation;

import io.keede.mombti.core.domains.report.model.GoalEta;
import io.keede.mombti.core.domains.report.model.GoalEtaRange;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.ProjectionPoint;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.model.ScenarioProjections;
import java.math.BigDecimal;
import java.util.OptionalInt;

/**
 * 목표 도달 주차: 기록된 주차 중 체중 ≤ 목표 + 0.05kg인 첫 주.
 * 범위는 기초대사량 × 1.1(빠른 쪽) / × 0.9(느린 쪽) 예측으로 구한다 (ADR-013).
 * 52주까지 없으면 52주 체중과 평형 체중을 담은 {@link GoalEta.Unreachable} (E-20).
 */
public class GoalEtaCalculator {

    /** 표시 단위(0.1kg)의 반올림 오차 허용. */
    static final BigDecimal REACH_TOLERANCE_KG = new BigDecimal("0.05");

    private final WeightProjectionSimulator simulator;

    public GoalEtaCalculator(WeightProjectionSimulator simulator) {
        this.simulator = simulator;
    }

    public GoalEtaRange estimate(SimulationInput input, ScenarioProjections projections, BigDecimal targetKg) {
        if (input.mode() != ReportMode.LOSS) {
            GoalEta notApplicable = new GoalEta.NotApplicable();
            return new GoalEtaRange(notApplicable, notApplicable, notApplicable);
        }
        return new GoalEtaRange(
                eta(input, projections.base(), SimulationVariant.BASE, targetKg),
                eta(input, projections.bmrHigh(), SimulationVariant.BMR_HIGH, targetKg),
                eta(input, projections.bmrLow(), SimulationVariant.BMR_LOW, targetKg));
    }

    public OptionalInt firstWeekAtOrBelow(Projection projection, BigDecimal weightKg) {
        BigDecimal threshold = weightKg.add(REACH_TOLERANCE_KG);
        return projection.points().stream()
                .filter(point -> point.weightKg().compareTo(threshold) <= 0)
                .mapToInt(ProjectionPoint::week)
                .findFirst();
    }

    private GoalEta eta(SimulationInput input, Projection projection, SimulationVariant variant,
                        BigDecimal targetKg) {
        OptionalInt week = firstWeekAtOrBelow(projection, targetKg);
        if (week.isPresent()) {
            return new GoalEta.Reached(week.getAsInt());
        }
        return new GoalEta.Unreachable(projection.points().getLast().weightKg(),
                simulator.equilibriumWeightKg(input, projection.scenario(), variant));
    }
}
