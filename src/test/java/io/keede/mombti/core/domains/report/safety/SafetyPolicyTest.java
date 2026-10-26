package io.keede.mombti.core.domains.report.safety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.keede.mombti.core.domains.report.calculator.BodyCompositionEstimator;
import io.keede.mombti.core.domains.report.calculator.BodyIndexCalculator;
import io.keede.mombti.core.domains.report.defaults.ProfileDefaults;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.exception.DomainValidationException;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SafetyPolicyTest {

    private final BodyIndexCalculator bodyIndexCalculator = new BodyIndexCalculator();
    private final SafetyPolicy policy = new SafetyPolicy(bodyIndexCalculator);
    private final ProfileDefaults defaults =
            new ProfileDefaults(bodyIndexCalculator, new BodyCompositionEstimator());

    private static BodyProfile profile(Gender gender, int age, String heightCm, String weightKg, String target) {
        return new BodyProfile(gender, age, new BigDecimal(heightCm), new BigDecimal(weightKg),
                Optional.empty(), Optional.ofNullable(target).map(BigDecimal::new), Optional.empty());
    }

    private ReportMode mode(BodyProfile profile) {
        return policy.decideMode(defaults.resolve(profile));
    }

    @Test
    void E04_19세_미만은_MINOR_BLOCKED() {
        assertThat(policy.evaluate(profile(Gender.MALE, 18, "175.0", "80.0", null)))
                .contains(ReportMode.MINOR_BLOCKED);
    }

    @Test
    void E04_19세는_차단하지_않음() {
        assertThat(policy.evaluate(profile(Gender.MALE, 19, "175.0", "80.0", null))).isEmpty();
    }

    @Test
    void E04_미성년자는_목표_BMI_검증보다_안내_화면이_우선() {
        assertThat(policy.evaluate(profile(Gender.FEMALE, 17, "160.0", "55.0", "40.0")))
                .contains(ReportMode.MINOR_BLOCKED);
    }

    @Test
    void E05_80세_초과는_나이_필드_도메인_검증_오류() {
        assertThatThrownBy(() -> policy.evaluate(profile(Gender.MALE, 81, "175.0", "80.0", null)))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> {
                    assertThat(e.code()).isEqualTo("E-05");
                    assertThat(e.field()).isEqualTo("age");
                    assertThat(e.getMessage()).doesNotContain("81");
                });
    }

    @Test
    void E05_80세는_허용() {
        assertThat(policy.evaluate(profile(Gender.MALE, 80, "175.0", "80.0", null))).isEmpty();
    }

    @Test
    void E06_65세_이상_적자_상한_20퍼센트() {
        assertThat(policy.maxDeficitRatio(65)).isEqualByComparingTo("0.20");
        assertThat(policy.maxDeficitRatio(80)).isEqualByComparingTo("0.20");
        assertThat(policy.maxDeficitRatio(64)).isEqualByComparingTo("0.25");
    }

    @Test
    void E06_65세_이상_단백질_하한_12g_per_kg() {
        assertThat(policy.minProteinGPerKg(65)).isEqualByComparingTo("1.2");
        assertThat(policy.minProteinGPerKg(64)).isEqualByComparingTo("0");
        assertThat(policy.proteinEvidence(70)).contains(Evidence.PROT_AGE_2013);
        assertThat(policy.proteinEvidence(30)).isEmpty();
    }

    @Test
    void E06_65세_이상은_근감소_주의_대상() {
        assertThat(policy.isSenior(65)).isTrue();
        assertThat(policy.isSenior(64)).isFalse();
    }

    @Test
    void E11_BMI_18_5_미만이면_UNDERWEIGHT_MAINTAIN() {
        // 50 / 1.7² = 17.3
        assertThat(mode(profile(Gender.FEMALE, 25, "170.0", "50.0", null)))
                .isEqualTo(ReportMode.UNDERWEIGHT_MAINTAIN);
    }

    @Test
    void E11_저체중이면_목표를_입력해도_감량하지_않음() {
        // 53 / 1.7² = 18.34, 목표 BMI 18.5 이상이 되려면 53.5kg 이상 → 목표 54.0은 유효하지만 증량 목표
        assertThat(mode(profile(Gender.FEMALE, 25, "170.0", "53.0", "54.0")))
                .isEqualTo(ReportMode.UNDERWEIGHT_MAINTAIN);
    }

    @Test
    void E12_목표가_현재_체중_1kg_이내면_MAINTAIN() {
        assertThat(mode(profile(Gender.MALE, 30, "175.0", "80.0", "79.0"))).isEqualTo(ReportMode.MAINTAIN);
        assertThat(mode(profile(Gender.MALE, 30, "175.0", "80.0", "85.0"))).isEqualTo(ReportMode.MAINTAIN);
    }

    @Test
    void E12_목표가_현재_체중보다_1kg_넘게_가벼우면_LOSS() {
        assertThat(mode(profile(Gender.MALE, 30, "175.0", "80.0", "78.9"))).isEqualTo(ReportMode.LOSS);
    }

    @Test
    void E13_목표_BMI_18_5_미만은_목표_필드_오류와_최저_체중_안내값() {
        // 170cm: 18.5 × 1.7² = 53.465 → 안내값은 BMI 18.5 이상이 되도록 올림 53.5kg
        assertThatThrownBy(() -> policy.evaluate(profile(Gender.FEMALE, 25, "170.0", "60.0", "53.4")))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> {
                    assertThat(e.code()).isEqualTo("E-13");
                    assertThat(e.field()).isEqualTo("targetWeightKg");
                    assertThat(e.arguments()).hasSize(1);
                    assertThat((BigDecimal) e.arguments().getFirst()).isEqualByComparingTo("53.5");
                    assertThat(e.getMessage()).doesNotContain("53");
                });
    }

    @Test
    void E13_목표_BMI_18_5_이상은_통과() {
        assertThat(policy.evaluate(profile(Gender.FEMALE, 25, "170.0", "60.0", "53.5"))).isEmpty();
    }

    @Test
    void E14_목표_없음_BMI_23_이상이면_BMI_22_기본_목표로_LOSS() {
        // 80 / 1.75² = 26.1, 기본 목표 22 × 1.75² = 67.4kg
        assertThat(mode(profile(Gender.MALE, 30, "175.0", "80.0", null))).isEqualTo(ReportMode.LOSS);
    }

    @Test
    void E15_목표_없음_BMI_18_5에서_22_9면_MAINTAIN() {
        // 65 / 1.75² = 21.2
        assertThat(mode(profile(Gender.MALE, 30, "175.0", "65.0", null))).isEqualTo(ReportMode.MAINTAIN);
    }

    @Test
    void E16_섭취_하한_여_1200_남_1500() {
        assertThat(policy.calorieFloorKcal(Gender.FEMALE)).isEqualByComparingTo("1200");
        assertThat(policy.calorieFloorKcal(Gender.MALE)).isEqualByComparingTo("1500");
    }

    @Test
    void 주당_감량_상한은_체중의_1퍼센트() {
        assertThat(policy.maxWeeklyLossRatio()).isEqualByComparingTo("0.01");
    }

    @Test
    void 첫_4주_주당_감량이_체중_1퍼센트_이하면_안전_범위() {
        // 80kg × 1% = 0.8kg/주 (HELMS_2014)
        assertThat(policy.isWithinSafeWeeklyLoss(new BigDecimal("80.0"), new BigDecimal("0.800"))).isTrue();
        assertThat(policy.isWithinSafeWeeklyLoss(new BigDecimal("80.0"), new BigDecimal("0.801"))).isFalse();
        assertThat(policy.isWithinSafeWeeklyLoss(new BigDecimal("80.0"), BigDecimal.ZERO)).isTrue();
    }

    private static BodyProfile profileWithBodyFat(Gender gender, String bodyFat) {
        return new BodyProfile(gender, 30, new BigDecimal("170.0"), new BigDecimal("70.0"),
                Optional.empty(), Optional.empty(), Optional.of(new BigDecimal(bodyFat)));
    }

    @Test
    void E09_BMI_12_미만이면_키_필드_단위_확인_도메인_검증_오류() {
        // 230cm 60kg → BMI 11.34
        assertThatThrownBy(() -> policy.evaluate(profile(Gender.MALE, 30, "230.0", "60.0", null)))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> {
                    assertThat(e.code()).isEqualTo("E-09");
                    assertThat(e.field()).isEqualTo("heightCm");
                    assertThat(e.getMessage()).doesNotContain("230").doesNotContain("60");
                });
    }

    @Test
    void E09_BMI_70_초과면_단위_확인_도메인_검증_오류() {
        // 120cm 101kg → BMI 70.14
        assertThatThrownBy(() -> policy.evaluate(profile(Gender.FEMALE, 30, "120.0", "101.0", null)))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        e -> assertThat(e.code()).isEqualTo("E-09"));
    }

    @Test
    void E09_BMI_12와_70은_경계_안() {
        // 200cm 48kg → BMI 12.0, 120cm 100.8kg → BMI 70.0
        assertThat(policy.evaluate(profile(Gender.MALE, 30, "200.0", "48.0", null))).isEmpty();
        assertThat(policy.evaluate(profile(Gender.MALE, 30, "120.0", "100.8", null))).isEmpty();
    }

    @Test
    void E23_체지방률이_필수지방_미만이면_도메인_검증_오류() {
        assertThatThrownBy(() -> policy.evaluate(profileWithBodyFat(Gender.FEMALE, "11.9")))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> {
                    assertThat(e.code()).isEqualTo("E-23");
                    assertThat(e.field()).isEqualTo("bodyFatPercent");
                    assertThat(e.getMessage()).doesNotContain("11.9");
                });
        assertThatThrownBy(() -> policy.evaluate(profileWithBodyFat(Gender.MALE, "4.9")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void E23_체지방률이_60_초과면_도메인_검증_오류() {
        assertThatThrownBy(() -> policy.evaluate(profileWithBodyFat(Gender.MALE, "60.1")))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        e -> assertThat(e.code()).isEqualTo("E-23"));
    }

    @Test
    void E23_필수지방과_60은_경계_안() {
        assertThat(policy.evaluate(profileWithBodyFat(Gender.FEMALE, "12.0"))).isEmpty();
        assertThat(policy.evaluate(profileWithBodyFat(Gender.MALE, "5.0"))).isEmpty();
        assertThat(policy.evaluate(profileWithBodyFat(Gender.FEMALE, "60.0"))).isEmpty();
    }
}
