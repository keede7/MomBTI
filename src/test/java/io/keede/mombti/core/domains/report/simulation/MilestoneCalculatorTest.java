package io.keede.mombti.core.domains.report.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.Milestone;
import io.keede.mombti.core.domains.report.model.MilestoneType;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.Scenario;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class MilestoneCalculatorTest {

    /** UTC로는 2026-10-25이지만 서울 기준 2026-10-26 00:30. */
    private static final Clock SEOUL_CLOCK =
            Clock.fixed(Instant.parse("2026-10-25T15:30:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    private final SimulationFixture fixture = new SimulationFixture();
    private final MilestoneCalculator calculator = new MilestoneCalculator(fixture.goalEtaCalculator, SEOUL_CLOCK);

    @Test
    void 날짜는_주입받은_서울_Clock_기준() {
        assertThat(calculator.dateAfterWeeks(0)).isEqualTo(TODAY);
        assertThat(calculator.dateAfterWeeks(3)).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void 지금_5퍼센트_10퍼센트_목표_순서로_예상_주차와_날짜() {
        // 남 30세 175cm 85kg → −5% 80.8kg, −10% 76.5kg, 기본 목표 BMI 22 = 67.4kg
        SimulationFixture.Case c = fixture.of(Gender.MALE, 30, "175.0", "85.0");
        Projection dietOnly = fixture.simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        List<Milestone> milestones = calculator.milestones(c.input(), dietOnly, fixture.goalKg(c));

        assertThat(milestones).extracting(Milestone::type).containsExactly(
                MilestoneType.CURRENT, MilestoneType.LOSS_5_PERCENT, MilestoneType.LOSS_10_PERCENT,
                MilestoneType.TARGET);
        assertThat(milestones).extracting(Milestone::weightKg).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("85.0"), new BigDecimal("80.8"), new BigDecimal("76.5"),
                        new BigDecimal("67.4"));

        assertThat(milestones.getFirst().eta()).isEqualTo(new Milestone.Eta.Reached(0, TODAY));
        Milestone.Eta.Reached loss5 = (Milestone.Eta.Reached) milestones.get(1).eta();
        Milestone.Eta.Reached loss10 = (Milestone.Eta.Reached) milestones.get(2).eta();
        assertThat(loss5.week()).isPositive().isLessThan(loss10.week());
        assertThat(loss5.date()).isEqualTo(TODAY.plusWeeks(loss5.week()));
        assertThat(dietOnly.points().get(loss5.week()).weightKg().doubleValue()).isLessThanOrEqualTo(80.85);
        assertThat(dietOnly.points().get(loss5.week() - 1).weightKg().doubleValue()).isGreaterThan(80.85);
    }

    @Test
    void 목표보다_가벼운_단계는_생략() {
        // 85kg → 목표 82kg. −5%(80.8)와 −10%(76.5)는 목표보다 가벼워 생략
        SimulationFixture.Case c = fixture.of(Gender.MALE, 30, "175.0", "85.0", null, "82.0", null);
        Projection dietOnly = fixture.simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        List<Milestone> milestones = calculator.milestones(c.input(), dietOnly, fixture.goalKg(c));

        assertThat(milestones).extracting(Milestone::type)
                .containsExactly(MilestoneType.CURRENT, MilestoneType.TARGET);
    }

    @Test
    void E20_52주_안에_도달하지_못한_단계는_NotReached() {
        // 여 70세 150cm 60kg → 기본 목표 49.5kg는 52주 미도달
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 70, "150.0", "60.0");
        Projection dietOnly = fixture.simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        List<Milestone> milestones = calculator.milestones(c.input(), dietOnly, fixture.goalKg(c));

        assertThat(milestones.getLast().type()).isEqualTo(MilestoneType.TARGET);
        assertThat(milestones.getLast().eta()).isInstanceOf(Milestone.Eta.NotReached.class);
        assertThat(milestones.get(1).eta()).isInstanceOf(Milestone.Eta.Reached.class);
    }

    @Test
    void 유지_모드는_단계별_목표가_없다() {
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 30, "165.0", "55.0");
        Projection dietOnly = fixture.simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        assertThat(calculator.milestones(c.input(), dietOnly, fixture.goalKg(c))).isEmpty();
    }
}
