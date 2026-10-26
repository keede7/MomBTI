package io.keede.mombti.core.domains.report.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.keede.mombti.core.domains.report.defaults.ProfileDefaults;
import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.EnergyTarget;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnergyTargetCalculatorTest {

    private final BodyIndexCalculator bodyIndexCalculator = new BodyIndexCalculator();
    private final BodyCompositionEstimator bodyCompositionEstimator = new BodyCompositionEstimator();
    private final ProfileDefaults defaults = new ProfileDefaults(bodyIndexCalculator, bodyCompositionEstimator);
    private final EnergyTargetCalculator calculator =
            new EnergyTargetCalculator(new SafetyPolicy(bodyIndexCalculator), bodyCompositionEstimator);

    private ResolvedProfile resolved(Gender gender, int age, String heightCm, String weightKg, String bodyFat) {
        return defaults.resolve(new BodyProfile(gender, age, new BigDecimal(heightCm), new BigDecimal(weightKg),
                Optional.empty(), Optional.empty(), Optional.of(new BigDecimal(bodyFat))));
    }

    @Test
    void 에너지_밀도_남_82kg_지방량_20_5kg이면_약_6870() {
        // p = 10.4 / 30.9 = 0.33657, ρ = 0.33657 × 1816 + 0.66343 × 9440 = 6874 (허용 오차 ±5)
        BigDecimal rho = calculator.energyDensityKcalPerKg(new BigDecimal("20.5"));

        assertThat(rho.doubleValue()).isCloseTo(6870, within(5.0));
    }

    @Test
    void 주당_체중_1퍼센트_상한_남_82kg_지방량_20_5kg이면_약_805kcal() {
        // 82 × 1% × 6874 ÷ 7 = 805.3 (허용 오차 ±5)
        BigDecimal cap = calculator.weeklyLossCapKcalPerDay(new BigDecimal("82.0"), new BigDecimal("20.5"));

        assertThat(cap.doubleValue()).isCloseTo(805, within(5.0));
    }

    @Test
    void 적자는_소비량_25퍼센트와_1퍼센트_상한_중_작은_값() {
        // 남 82kg 체지방 25% → 지방량 20.5kg, 1% 상한 805.3. 소비량 2800 × 25% = 700 → 적자 700, 섭취 2100
        EnergyTarget target = calculator.calculate(
                resolved(Gender.MALE, 30, "178.0", "82.0", "25.0"), new BigDecimal("2800"), ReportMode.LOSS);

        assertThat(target.deficitKcal()).isEqualByComparingTo("700");
        assertThat(target.intakeKcal()).isEqualByComparingTo("2100");
        assertThat(target.floorApplied()).isFalse();
        assertThat(target.dietOnlyMaintains()).isFalse();
        assertThat(target.evidence()).contains(Evidence.HELMS_2014, Evidence.HALL_2007_FORBES, Evidence.HALL_2008);
    }

    @Test
    void 소비량이_크면_주당_1퍼센트_상한이_적용되고_10kcal_단위로_반올림() {
        // 소비량 3600 × 25% = 900 > 805.3 → 적자 805.3, 3600 − 805.3 = 2794.7 → 2790
        EnergyTarget target = calculator.calculate(
                resolved(Gender.MALE, 30, "178.0", "82.0", "25.0"), new BigDecimal("3600"), ReportMode.LOSS);

        assertThat(target.intakeKcal()).isEqualByComparingTo("2790");
        assertThat(target.deficitKcal()).isEqualByComparingTo("810");
    }

    @Test
    void E06_65세_이상은_적자_상한_20퍼센트() {
        // 2000 × 20% = 400 (1% 상한 805보다 작음) → 섭취 1600
        EnergyTarget target = calculator.calculate(
                resolved(Gender.MALE, 70, "178.0", "82.0", "25.0"), new BigDecimal("2000"), ReportMode.LOSS);

        assertThat(target.intakeKcal()).isEqualByComparingTo("1600");
        assertThat(target.deficitKcal()).isEqualByComparingTo("400");
    }

    @Test
    void E16_소비량_빼기_적자가_하한보다_작으면_하한으로_올림() {
        // 여 1500 × 25% = 375 → 1125 < 1200 → 1200, 실제 적자 300
        EnergyTarget target = calculator.calculate(
                resolved(Gender.FEMALE, 30, "155.0", "60.0", "35.0"), new BigDecimal("1500"), ReportMode.LOSS);

        assertThat(target.intakeKcal()).isEqualByComparingTo("1200");
        assertThat(target.deficitKcal()).isEqualByComparingTo("300");
        assertThat(target.floorApplied()).isTrue();
        assertThat(target.dietOnlyMaintains()).isFalse();
        assertThat(target.evidence()).contains(Evidence.AHA_ACC_TOS_2013);
    }

    @Test
    void E17_하한_적용_후_적자가_0_이하면_식단만_라인은_유지() {
        // 여 소비량 1180 < 하한 1200 → 섭취 1200, 적자 ≤ 0
        EnergyTarget target = calculator.calculate(
                resolved(Gender.FEMALE, 30, "150.0", "58.0", "35.0"), new BigDecimal("1180"), ReportMode.LOSS);

        assertThat(target.intakeKcal()).isEqualByComparingTo("1200");
        assertThat(target.floorApplied()).isTrue();
        assertThat(target.dietOnlyMaintains()).isTrue();
        assertThat(target.deficitKcal()).isEqualByComparingTo("0");
    }

    @Test
    void E17_하한과_소비량이_같아도_식단만_라인은_유지() {
        EnergyTarget target = calculator.calculate(
                resolved(Gender.MALE, 30, "160.0", "70.0", "30.0"), new BigDecimal("1500"), ReportMode.LOSS);

        assertThat(target.intakeKcal()).isEqualByComparingTo("1500");
        assertThat(target.dietOnlyMaintains()).isTrue();
    }

    @Test
    void 유지_모드는_소비량을_10kcal_단위로_반올림해_섭취_목표로() {
        EnergyTarget target = calculator.calculate(
                resolved(Gender.MALE, 30, "175.0", "65.0", "15.0"), new BigDecimal("2345.6"), ReportMode.MAINTAIN);

        assertThat(target.intakeKcal()).isEqualByComparingTo("2350");
        assertThat(target.deficitKcal()).isEqualByComparingTo("0");
        assertThat(target.floorApplied()).isFalse();
        assertThat(target.dietOnlyMaintains()).isTrue();
    }

    @Test
    void E11_저체중_유지_모드도_적자_없음() {
        EnergyTarget target = calculator.calculate(
                resolved(Gender.FEMALE, 25, "170.0", "50.0", "18.0"), new BigDecimal("1800"),
                ReportMode.UNDERWEIGHT_MAINTAIN);

        assertThat(target.intakeKcal()).isEqualByComparingTo("1800");
        assertThat(target.deficitKcal()).isEqualByComparingTo("0");
        assertThat(target.dietOnlyMaintains()).isTrue();
    }
}
