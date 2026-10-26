package io.keede.mombti.core.domains.report.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import io.keede.mombti.core.domains.report.exception.SimulationInvariantException;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.ProjectionPoint;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.model.Scenario;
import io.keede.mombti.core.domains.report.model.ScenarioProjections;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WeightProjectionSimulatorTest {

    private final SimulationFixture fixture = new SimulationFixture();
    private final WeightProjectionSimulator simulator = fixture.simulator;

    /** 감량 모드 프로필: Mifflin(체지방률 미입력) / Cunningham(입력), 남·여, 65세 이상, 하한 적용 포함. */
    static Stream<Arguments> lossProfiles() {
        return Stream.of(
                Arguments.of(Gender.MALE, 30, "175.0", "85.0", null, null, null),
                Arguments.of(Gender.FEMALE, 28, "162.0", "68.0", ActivityLevel.LIGHT, "58.0", null),
                Arguments.of(Gender.MALE, 35, "178.0", "82.0", null, null, "25.0"),
                Arguments.of(Gender.FEMALE, 45, "158.0", "95.0", ActivityLevel.MODERATE, null, "45.0"),
                Arguments.of(Gender.MALE, 70, "170.0", "80.0", null, null, null),
                Arguments.of(Gender.FEMALE, 60, "150.0", "60.0", null, null, null),
                Arguments.of(Gender.MALE, 40, "190.0", "180.0", ActivityLevel.VERY_ACTIVE, null, null));
    }

    @Test
    void 기록은_0주부터_52주까지_7일마다_53개() {
        SimulationFixture.Case c = fixture.of(Gender.MALE, 30, "175.0", "85.0");

        Projection projection = simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        assertThat(projection.scenario()).isEqualTo(Scenario.DIET_ONLY);
        assertThat(projection.points()).hasSize(53);
        assertThat(projection.points()).extracting(ProjectionPoint::week)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(0, 52).boxed().toList());
        assertThat(projection.points().getFirst().weightKg()).isEqualByComparingTo("85.0");
    }

    @Test
    void 첫_주_감량은_수기_계산과_같다() {
        // 남 35세 178cm 82kg 체지방 25% → F 20.5, L 61.5, Cunningham BMR 1698.4, × PAL 1.40 = 2377.76
        // 적자 min(594.44, 805.3) → 섭취 1783.3 → 1780. 1일차 적자 597.76, 적응 59.78, ΔE −537.98
        // p = 10.4 / 30.9 = 0.33657, ρ = 6874.0 → ΔW −0.07826kg/일 → 7일 후 약 81.45kg (허용 오차 ±0.01)
        SimulationFixture.Case c = fixture.of(Gender.MALE, 35, "178.0", "82.0", null, null, "25.0");

        Projection projection = simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        assertThat(c.target().intakeKcal()).isEqualByComparingTo("1780");
        assertThat(projection.points().get(1).weightKg().doubleValue()).isCloseTo(81.45, within(0.01));
    }

    @ParameterizedTest
    @MethodSource("lossProfiles")
    void 감량_모드에서_체중은_단조_비증가하고_불변식을_지킨다(Gender gender, int age, String height, String weight,
                                         ActivityLevel activity, String goal, String bodyFat) {
        SimulationFixture.Case c = fixture.of(gender, age, height, weight, activity, goal, bodyFat);
        assertThat(c.mode()).isEqualTo(ReportMode.LOSS);

        for (Scenario scenario : Scenario.values()) {
            ScenarioProjections projections = simulator.simulate(c.input(), scenario);
            for (Projection projection : List.of(projections.base(), projections.bmrHigh(), projections.adherence())) {
                assertMonotonicNonIncreasing(projection);
            }
            for (Projection projection : List.of(projections.base(), projections.bmrLow(),
                    projections.bmrHigh(), projections.adherence())) {
                assertThat(projection.points()).allSatisfy(point -> {
                    assertThat(point.fatMassKg()).isGreaterThan(new BigDecimal("0.5"));
                    assertThat(point.leanMassKg()).isPositive();
                    assertThat(point.weightKg().doubleValue())
                            .isCloseTo(point.fatMassKg().add(point.leanMassKg()).doubleValue(), within(0.002));
                });
            }
        }
    }

    @ParameterizedTest
    @MethodSource("lossProfiles")
    void 같은_적자에서_7700kcal_정적_규칙보다_52주_감량이_적다(Gender gender, int age, String height, String weight,
                                               ActivityLevel activity, String goal, String bodyFat) {
        SimulationFixture.Case c = fixture.of(gender, age, height, weight, activity, goal, bodyFat);
        BigDecimal startDeficit = c.target().tdeeKcal().subtract(c.input().intakeKcal());

        Projection projection = simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        double dynamicLoss = projection.points().getFirst().weightKg()
                .subtract(projection.points().getLast().weightKg()).doubleValue();
        double staticLoss = startDeficit.doubleValue() * 364 / 7700;
        assertThat(dynamicLoss).isLessThan(staticLoss);
    }

    @ParameterizedTest
    @MethodSource("lossProfiles")
    void 개인차_밴드는_BMR_110퍼센트_이하_기본_이하_BMR_90퍼센트_순서(Gender gender, int age, String height, String weight,
                                                  ActivityLevel activity, String goal, String bodyFat) {
        SimulationFixture.Case c = fixture.of(gender, age, height, weight, activity, goal, bodyFat);

        for (Scenario scenario : Scenario.values()) {
            ScenarioProjections projections = simulator.simulate(c.input(), scenario);
            for (int week = 0; week <= 52; week++) {
                BigDecimal base = projections.base().points().get(week).weightKg();
                assertThat(projections.bmrHigh().points().get(week).weightKg()).isLessThanOrEqualTo(base);
                assertThat(projections.bmrLow().points().get(week).weightKg()).isGreaterThanOrEqualTo(base);
                assertThat(projections.adherence().points().get(week).weightKg()).isGreaterThanOrEqualTo(base);
            }
        }
    }

    @Test
    void 운동_일수가_많을수록_52주_체중이_가볍거나_같다() {
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 28, "162.0", "68.0", ActivityLevel.LIGHT, "58.0", null);

        List<BigDecimal> week52 = Stream.of(Scenario.DIET_ONLY, Scenario.EX3, Scenario.EX4, Scenario.EX5)
                .map(s -> simulator.project(c.input(), s, SimulationVariant.BASE).points().getLast().weightKg())
                .toList();

        assertThat(week52).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(week52.get(0)).isGreaterThan(week52.get(3));
    }

    @Test
    void E15_유지_모드의_식단만_라인은_체중이_그대로다() {
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 30, "165.0", "55.0");
        assertThat(c.mode()).isEqualTo(ReportMode.MAINTAIN);

        Projection projection = simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        assertThat(projection.points()).allSatisfy(point ->
                assertThat(point.weightKg().doubleValue()).isCloseTo(55.0, within(0.001)));
    }

    @Test
    void E17_하한_적용_후_적자가_없으면_식단만은_유지선이고_운동_라인만_감량() {
        // 여 80세 140cm 46kg: Mifflin 774 × 1.40 = 1083.6 < 하한 1200 → 식단만 유지
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 80, "140.0", "46.0");
        assertThat(c.mode()).isEqualTo(ReportMode.LOSS);
        assertThat(c.target().dietOnlyMaintains()).isTrue();

        Projection dietOnly = simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);
        Projection ex4 = simulator.project(c.input(), Scenario.EX4, SimulationVariant.BASE);

        assertThat(dietOnly.points()).allSatisfy(point ->
                assertThat(point.weightKg().doubleValue()).isCloseTo(46.0, within(0.001)));
        assertThat(ex4.points().getLast().weightKg()).isLessThan(new BigDecimal("46.0"));
        assertMonotonicNonIncreasing(ex4);
    }

    @Test
    void 현실_보정_섭취는_목표에_소비량과의_차이_20퍼센트를_더한다() {
        // 섭취 1780, 소비량 2377.76 → 1780 + 0.2 × 597.76 = 1899.55
        SimulationFixture.Case c = fixture.of(Gender.MALE, 35, "178.0", "82.0", null, null, "25.0");

        assertThat(simulator.intakeKcal(c.input(), SimulationVariant.ADHERENCE_80).doubleValue())
                .isCloseTo(1899.55, within(0.01));
        assertThat(simulator.intakeKcal(c.input(), SimulationVariant.BASE)).isEqualByComparingTo("1780");
    }

    @Test
    void 평형_체중은_Mifflin_소비량이_섭취와_같아지는_체중() {
        // 여 70세 150cm 60kg: 섭취 하한 1200. 1.40 × (10W + 937.5 − 350 − 161) = 1200 → W = 43.06 (허용 오차 ±0.1)
        SimulationFixture.Case c = fixture.of(Gender.FEMALE, 70, "150.0", "60.0");
        assertThat(c.target().intakeKcal()).isEqualByComparingTo("1200");

        BigDecimal equilibrium = simulator.equilibriumWeightKg(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        assertThat(equilibrium.doubleValue()).isCloseTo(43.06, within(0.1));
    }

    @Test
    void E29_시뮬레이션_불변식_위반이면_SimulationInvariantException() {
        // 지방량 0.9kg에서 큰 적자 → 며칠 안에 0.5kg 아래로 내려감
        SimulationInput input = new SimulationInput(Gender.MALE, 30, new BigDecimal("175.0"), new BigDecimal("1.40"),
                true, new BigDecimal("0.9"), new BigDecimal("60.0"), new BigDecimal("500"),
                new BigDecimal("2500"), ReportMode.LOSS);

        assertThatThrownBy(() -> simulator.project(input, Scenario.DIET_ONLY, SimulationVariant.BASE))
                .isInstanceOf(SimulationInvariantException.class)
                .hasMessageContaining("FAT_MASS")
                .hasMessageNotContaining("60.0")
                .hasMessageNotContaining("175");
    }

    @Test
    void E29_감량_모드에서_체중이_늘면_SimulationInvariantException() {
        // 섭취가 소비량보다 많은 비정상 입력 (서비스 경로에서는 유지선으로 막힘)
        SimulationInput input = new SimulationInput(Gender.MALE, 30, new BigDecimal("175.0"), new BigDecimal("1.40"),
                false, new BigDecimal("20.0"), new BigDecimal("60.0"), new BigDecimal("4000"),
                new BigDecimal("2500"), ReportMode.LOSS);

        assertThatThrownBy(() -> simulator.project(input, Scenario.DIET_ONLY, SimulationVariant.BASE))
                .isInstanceOf(SimulationInvariantException.class)
                .hasMessageContaining("MONOTONIC");
    }

    private static void assertMonotonicNonIncreasing(Projection projection) {
        List<ProjectionPoint> points = projection.points();
        for (int i = 1; i < points.size(); i++) {
            assertThat(points.get(i).weightKg()).isLessThanOrEqualTo(points.get(i - 1).weightKg());
        }
    }
}
