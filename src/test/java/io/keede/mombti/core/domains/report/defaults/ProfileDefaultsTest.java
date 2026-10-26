package io.keede.mombti.core.domains.report.defaults;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.calculator.BodyCompositionEstimator;
import io.keede.mombti.core.domains.report.calculator.BodyIndexCalculator;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.Goal;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProfileDefaultsTest {

    private final ProfileDefaults defaults =
            new ProfileDefaults(new BodyIndexCalculator(), new BodyCompositionEstimator());

    private static BodyProfile profile(String heightCm, String weightKg,
                                       Optional<ActivityLevel> activity,
                                       Optional<BigDecimal> target,
                                       Optional<BigDecimal> bodyFat) {
        return new BodyProfile(Gender.MALE, 30, new BigDecimal(heightCm), new BigDecimal(weightKg),
                activity, target, bodyFat);
    }

    private static BodyProfile minimal(String heightCm, String weightKg) {
        return profile(heightCm, weightKg, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void 활동량이_없으면_좌식_PAL_1_40_추정_밴드상단_1_60() {
        ResolvedProfile resolved = defaults.resolve(minimal("175.0", "80.0"));

        assertThat(resolved.activityLevel().value()).isEqualTo(ActivityLevel.SEDENTARY);
        assertThat(resolved.activityLevel().estimated()).isTrue();
        assertThat(resolved.activityLevel().evidence()).contains(Evidence.FAO_WHO_UNU_2004);
        assertThat(resolved.pal()).isEqualByComparingTo("1.40");
        assertThat(resolved.palBandUpper()).isEqualByComparingTo("1.60");
    }

    @Test
    void 활동량을_입력하면_그대로_사용하고_추정_아님() {
        ResolvedProfile resolved = defaults.resolve(
                profile("175.0", "80.0", Optional.of(ActivityLevel.ACTIVE), Optional.empty(), Optional.empty()));

        assertThat(resolved.activityLevel().value()).isEqualTo(ActivityLevel.ACTIVE);
        assertThat(resolved.activityLevel().estimated()).isFalse();
        assertThat(resolved.pal()).isEqualByComparingTo("1.85");
        assertThat(resolved.palBandUpper()).isEqualByComparingTo("1.85");
    }

    @Test
    void 목표를_입력하면_TargetWeight이고_추정_아님() {
        ResolvedProfile resolved = defaults.resolve(
                profile("175.0", "80.0", Optional.empty(), Optional.of(new BigDecimal("70.0")), Optional.empty()));

        assertThat(resolved.goal().value()).isEqualTo(new Goal.TargetWeight(new BigDecimal("70.0")));
        assertThat(resolved.goal().estimated()).isFalse();
    }

    @Test
    void 목표가_없고_BMI_23_이상이면_BMI_22_체중이_기본_목표() {
        // BMI 80 / 1.75² = 26.1 → 목표 22 × 1.75² = 67.375 → 67.4
        ResolvedProfile resolved = defaults.resolve(minimal("175.0", "80.0"));

        assertThat(resolved.goal().value()).isInstanceOfSatisfying(Goal.DefaultHealthyWeight.class, goal -> {
            assertThat(goal.weightKg()).isEqualByComparingTo("67.4");
            assertThat(goal.maintain()).isFalse();
        });
        assertThat(resolved.goal().estimated()).isTrue();
        assertThat(resolved.goal().evidence()).contains(Evidence.KSSO_2022);
    }

    @Test
    void 목표가_없고_BMI가_정확히_23이면_기본_목표_감량() {
        // 23 × 1.70² = 66.47
        ResolvedProfile resolved = defaults.resolve(minimal("170.0", "66.47"));

        assertThat(resolved.goal().value()).isInstanceOfSatisfying(Goal.DefaultHealthyWeight.class, goal -> {
            assertThat(goal.weightKg()).isEqualByComparingTo("63.6");
            assertThat(goal.maintain()).isFalse();
        });
    }

    @Test
    void 목표가_없고_BMI_18_5에서_22_9면_현재_체중_유지() {
        // BMI 65 / 1.75² = 21.2
        ResolvedProfile resolved = defaults.resolve(minimal("175.0", "65.0"));

        assertThat(resolved.goal().value()).isInstanceOfSatisfying(Goal.DefaultHealthyWeight.class, goal -> {
            assertThat(goal.weightKg()).isEqualByComparingTo("65.0");
            assertThat(goal.maintain()).isTrue();
        });
        assertThat(resolved.goal().estimated()).isTrue();
    }

    @Test
    void 목표가_없고_BMI_22_9_경계면_유지() {
        // 22.9 × 1.75² = 70.13
        ResolvedProfile resolved = defaults.resolve(minimal("175.0", "70.1"));

        assertThat(resolved.goal().value()).isInstanceOfSatisfying(Goal.DefaultHealthyWeight.class,
                goal -> assertThat(goal.maintain()).isTrue());
    }

    @Test
    void 체지방률이_없으면_Deurenberg_추정값() {
        // BMI 26.122... → 1.2 × 26.122 + 0.23 × 30 − 10.8 − 5.4 = 22.05 → 22.0 또는 22.1 (±0.05)
        ResolvedProfile resolved = defaults.resolve(minimal("175.0", "80.0"));

        assertThat(resolved.bodyFatPercent().value()).isBetween(new BigDecimal("22.0"), new BigDecimal("22.1"));
        assertThat(resolved.bodyFatPercent().estimated()).isTrue();
        assertThat(resolved.bodyFatPercent().evidence()).containsExactly(Evidence.DEURENBERG_1991);
    }

    @Test
    void 체지방률을_입력하면_그대로_사용하고_추정_아님() {
        ResolvedProfile resolved = defaults.resolve(
                profile("175.0", "80.0", Optional.empty(), Optional.empty(), Optional.of(new BigDecimal("18.5"))));

        assertThat(resolved.bodyFatPercent().value()).isEqualByComparingTo("18.5");
        assertThat(resolved.bodyFatPercent().estimated()).isFalse();
    }

    @Test
    void 선택_3개를_모두_입력하면_추정_항목이_없음() {
        ResolvedProfile resolved = defaults.resolve(profile("175.0", "80.0",
                Optional.of(ActivityLevel.LIGHT), Optional.of(new BigDecimal("72.0")), Optional.of(new BigDecimal("20.0"))));

        assertThat(resolved.activityLevel().estimated()).isFalse();
        assertThat(resolved.goal().estimated()).isFalse();
        assertThat(resolved.bodyFatPercent().estimated()).isFalse();
        assertThat(resolved.providedOptionalCount()).isEqualTo(3);
    }

    @Test
    void 필수_값은_그대로_전달() {
        ResolvedProfile resolved = defaults.resolve(minimal("175.0", "80.0"));

        assertThat(resolved.gender()).isEqualTo(Gender.MALE);
        assertThat(resolved.age()).isEqualTo(30);
        assertThat(resolved.heightCm()).isEqualByComparingTo("175.0");
        assertThat(resolved.weightKg()).isEqualByComparingTo("80.0");
        assertThat(resolved.providedOptionalCount()).isZero();
    }
}
