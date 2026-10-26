package io.keede.mombti.core.domains.report.simulation;

import io.keede.mombti.core.domains.report.model.ChartWindow;
import io.keede.mombti.core.domains.report.model.GoalEta;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.ProjectionPoint;
import io.keede.mombti.core.domains.report.model.ScenarioProjections;
import java.util.List;

/** 그래프 기간 규칙 (ADR-021). 계산은 52주, 표시는 최대 26주를 2주 간격으로. */
public class ChartWindowPolicy {

    static final int CHART_STEP_WEEKS = 2;

    /** 식단만 기본 예측의 도달 시점으로 정한다 (E-18, E-19, E-20). */
    public ChartWindow window(GoalEta dietOnlyEta) {
        return switch (dietOnlyEta) {
            case GoalEta.Reached reached when reached.week() <= ChartWindow.DEFAULT_WEEKS ->
                    new ChartWindow.GoalInView(reached.week());
            case GoalEta.Reached reached when reached.week() <= ChartWindow.EXTENDED_WEEKS ->
                    new ChartWindow.GoalWithinSixMonths(reached.week());
            case GoalEta.Reached reached -> new ChartWindow.GoalBeyondSixMonths();
            case GoalEta.Unreachable unreachable -> new ChartWindow.GoalBeyondSixMonths();
            case GoalEta.NotApplicable notApplicable -> new ChartWindow.NoGoal();
        };
    }

    /** 차트와 표 데이터: 0~26주, 2주 간격. */
    public List<ProjectionPoint> chartPoints(Projection projection) {
        return projection.points().stream()
                .filter(point -> point.week() <= ChartWindow.EXTENDED_WEEKS)
                .filter(point -> point.week() % CHART_STEP_WEEKS == 0)
                .toList();
    }

    /** 시나리오 하나의 기본·개인차 밴드·현실 보정 예측을 모두 차트 데이터로 줄인다. */
    public ScenarioProjections chartProjections(ScenarioProjections projections) {
        return new ScenarioProjections(projections.scenario(),
                chart(projections.base()),
                chart(projections.bmrLow()),
                chart(projections.bmrHigh()),
                chart(projections.adherence()));
    }

    private Projection chart(Projection projection) {
        return new Projection(projection.scenario(), chartPoints(projection));
    }
}
