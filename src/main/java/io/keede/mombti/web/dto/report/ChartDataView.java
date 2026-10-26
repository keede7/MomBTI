package io.keede.mombti.web.dto.report;

import io.keede.mombti.core.domains.report.model.ChartWindow;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.ProjectionPoint;
import io.keede.mombti.core.domains.report.model.Report;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.model.Scenario;
import io.keede.mombti.core.domains.report.model.ScenarioProjections;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 차트 데이터 (0~26주, 2주 간격). 서버가 만든 {@code Report.chartProjections()}를 표시 단위로 옮기기만 한다.
 * 같은 데이터로 표도 렌더한다 (ADR-014). JSON에 null을 넣지 않는다.
 *
 * @param bandLowerKg 개인차 밴드 하단 = 식단만 기초대사량 × 1.1 예측
 * @param bandUpperKg 개인차 밴드 상단 = 식단만 기초대사량 × 0.9 예측
 * @param window      GOAL_IN_VIEW | GOAL_WITHIN_SIX_MONTHS | GOAL_BEYOND_SIX_MONTHS | NO_GOAL
 * @param goalWeek    도달 주차 (GOAL_IN_VIEW, GOAL_WITHIN_SIX_MONTHS). 그 밖에는 0
 */
public record ChartDataView(
        List<Integer> weeks,
        List<String> dates,
        List<Series> series,
        List<BigDecimal> bandLowerKg,
        List<BigDecimal> bandUpperKg,
        BigDecimal targetKg,
        boolean showTarget,
        String window,
        int goalWeek,
        int defaultWeeks,
        int extendedWeeks,
        boolean sixMonthToggle
) {

    /** 기본 표시는 식단만 + 주 4일 (docs/PRD.md 리포트 구성 3). */
    private static final List<Scenario> VISIBLE_BY_DEFAULT = List.of(Scenario.DIET_ONLY, Scenario.EX4);

    /**
     * @param adherenceWeightsKg 현실 보정(계획의 80%만 지킨 경우, 가정) 라인
     */
    public record Series(String key, String label, int sessionsPerWeek, boolean visibleByDefault,
                         List<BigDecimal> weightsKg, List<BigDecimal> adherenceWeightsKg) {
    }

    public static ChartDataView from(Report report) {
        ScenarioProjections dietOnly = report.chartProjections().getFirst();
        List<Integer> weeks = dietOnly.base().points().stream().map(ProjectionPoint::week).toList();
        ChartWindow window = report.chartWindow();
        return new ChartDataView(
                weeks,
                weeks.stream().map(week -> ReportView.formatWeekOfMonth(report.dateAfterWeeks(week))).toList(),
                report.chartProjections().stream().map(ChartDataView::series).toList(),
                weights(dietOnly.bmrHigh()),
                weights(dietOnly.bmrLow()),
                kg(report.profile().goal().value().weightKg()),
                report.mode() == ReportMode.LOSS,
                windowName(window),
                switch (window) {
                    case ChartWindow.GoalInView inView -> inView.week();
                    case ChartWindow.GoalWithinSixMonths withinSixMonths -> withinSixMonths.week();
                    case ChartWindow.GoalBeyondSixMonths beyond -> 0;
                    case ChartWindow.NoGoal noGoal -> 0;
                },
                window.defaultWeeks(),
                ChartWindow.EXTENDED_WEEKS,
                window.sixMonthToggle());
    }

    private static Series series(ScenarioProjections projections) {
        Scenario scenario = projections.scenario();
        return new Series(scenario.name(), ReportView.scenarioLabel(scenario), scenario.sessionsPerWeek(),
                VISIBLE_BY_DEFAULT.contains(scenario), weights(projections.base()), weights(projections.adherence()));
    }

    private static String windowName(ChartWindow window) {
        return switch (window) {
            case ChartWindow.GoalInView inView -> "GOAL_IN_VIEW";
            case ChartWindow.GoalWithinSixMonths withinSixMonths -> "GOAL_WITHIN_SIX_MONTHS";
            case ChartWindow.GoalBeyondSixMonths beyond -> "GOAL_BEYOND_SIX_MONTHS";
            case ChartWindow.NoGoal noGoal -> "NO_GOAL";
        };
    }

    private static List<BigDecimal> weights(Projection projection) {
        return projection.points().stream().map(point -> kg(point.weightKg())).toList();
    }

    private static BigDecimal kg(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }
}
