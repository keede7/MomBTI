package io.keede.mombti.core.domains.report.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.GoalEta;
import io.keede.mombti.core.domains.report.model.GoalEtaRange;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.ProjectionPoint;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.model.Scenario;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoalEtaCalculatorTest {

    private final SimulationFixture fixture = new SimulationFixture();
    private final GoalEtaCalculator calculator = fixture.goalEtaCalculator;

    private static Projection projection(String... weights) {
        List<ProjectionPoint> points = new java.util.ArrayList<>();
        for (int week = 0; week < weights.length; week++) {
            BigDecimal weight = new BigDecimal(weights[week]);
            points.add(new ProjectionPoint(week, weight, BigDecimal.TEN, weight.subtract(BigDecimal.TEN)));
        }
        return new Projection(Scenario.DIET_ONLY, points);
    }

    @Test
    void 도달_주차는_체중이_목표_더하기_0_05kg_이하인_첫_주() {
        Projection projection = projection("70.0", "69.5", "69.06", "69.05", "68.9");

        assertThat(calculator.firstWeekAtOrBelow(projection, new BigDecimal("69.0"))).hasValue(3);
        assertThat(calculator.firstWeekAtOrBelow(projection, new BigDecimal("68.0"))).isEmpty();
    }

    @Test
    void 도달하면_Reached와_밴드_범위() {
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 28, "162.0", "68.0", ActivityLevel.LIGHT, "62.0", null);

        GoalEtaRange range = calculator.estimate(c.input(),
                fixture.simulator.simulate(c.input(), Scenario.DIET_ONLY), fixture.goalKg(c));

        assertThat(range.expected()).isInstanceOf(GoalEta.Reached.class);
        assertThat(range.earliest()).isInstanceOf(GoalEta.Reached.class);
        int expected = ((GoalEta.Reached) range.expected()).week();
        int earliest = ((GoalEta.Reached) range.earliest()).week();
        assertThat(earliest).isLessThanOrEqualTo(expected);
        assertThat(range.latest()).satisfiesAnyOf(
                latest -> assertThat(latest).isInstanceOf(GoalEta.Unreachable.class),
                latest -> assertThat(((GoalEta.Reached) latest).week()).isGreaterThanOrEqualTo(expected));
    }

    @Test
    void E20_52주_안에_도달하지_못하면_52주_체중과_평형_체중() {
        // 여 70세 150cm 60kg → 기본 목표 49.5kg, 섭취 하한 1200. 평형 체중 43.06kg (Mifflin, 허용 오차 ±0.1)
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 70, "150.0", "60.0");
        var projections = fixture.simulator.simulate(c.input(), Scenario.DIET_ONLY);

        GoalEtaRange range = calculator.estimate(c.input(), projections, fixture.goalKg(c));

        assertThat(range.expected()).isInstanceOf(GoalEta.Unreachable.class);
        GoalEta.Unreachable unreachable = (GoalEta.Unreachable) range.expected();
        assertThat(unreachable.week52WeightKg())
                .isEqualByComparingTo(projections.base().points().getLast().weightKg());
        assertThat(unreachable.week52WeightKg()).isGreaterThan(fixture.goalKg(c));
        assertThat(unreachable.equilibriumWeightKg().doubleValue()).isCloseTo(43.06, within(0.1));
    }

    @Test
    void 유지_모드는_NotApplicable() {
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 30, "165.0", "55.0");
        assertThat(c.mode()).isEqualTo(ReportMode.MAINTAIN);

        GoalEtaRange range = calculator.estimate(c.input(),
                fixture.simulator.simulate(c.input(), Scenario.DIET_ONLY), fixture.goalKg(c));

        assertThat(range.expected()).isInstanceOf(GoalEta.NotApplicable.class);
        assertThat(range.earliest()).isInstanceOf(GoalEta.NotApplicable.class);
        assertThat(range.latest()).isInstanceOf(GoalEta.NotApplicable.class);
    }

    @Test
    void 운동_일수가_많을수록_도달이_빠르거나_같다() {
        SimulationFixture.Case c = fixture.of(Gender.MALE, 30, "175.0", "85.0");

        List<Integer> weeks = List.of(Scenario.DIET_ONLY, Scenario.EX3, Scenario.EX4, Scenario.EX5).stream()
                .map(s -> calculator.estimate(c.input(), fixture.simulator.simulate(c.input(), s), fixture.goalKg(c)))
                .map(range -> range.expected() instanceof GoalEta.Reached reached ? reached.week() : 53)
                .toList();

        assertThat(weeks).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(weeks.getLast()).isLessThan(weeks.getFirst());
    }
}
