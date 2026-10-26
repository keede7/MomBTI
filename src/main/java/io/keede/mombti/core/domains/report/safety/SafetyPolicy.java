package io.keede.mombti.core.domains.report.safety;

import io.keede.mombti.core.domains.report.calculator.BodyIndexCalculator;
import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.exception.DomainValidationException;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.Goal;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/** 입력값 기반 안전 규칙의 유일한 구현 위치 (ADR-010). */
public class SafetyPolicy {

    static final int MIN_AGE = 19;
    static final int MAX_AGE = 80;
    static final int SENIOR_AGE = 65;

    /** {@link Evidence#KSSO_2022} 저체중 상한. 현재 BMI가 미만이면 감량하지 않고, 목표 BMI도 이 값 이상. */
    static final BigDecimal MIN_HEALTHY_BMI = new BigDecimal("18.5");
    /** 목표가 현재 체중 − 1kg 이상이면 유지 모드 (E-12). */
    static final BigDecimal MAINTAIN_MARGIN_KG = BigDecimal.ONE;

    /** 적자 상한: 소비량의 25%, 65세 이상 20%. */
    static final BigDecimal MAX_DEFICIT_RATIO = new BigDecimal("0.25");
    static final BigDecimal SENIOR_MAX_DEFICIT_RATIO = new BigDecimal("0.20");
    /** {@link Evidence#HELMS_2014} 주당 체중 0.5~1% 중 상한. */
    static final BigDecimal MAX_WEEKLY_LOSS_RATIO = new BigDecimal("0.01");
    /** 안전 범위는 첫 4주 평균 주당 감량으로 판단한다. */
    public static final int SAFE_RANGE_WEEKS = 4;

    /** {@link Evidence#AHA_ACC_TOS_2013} 처방 범위 하단: 여 1,200 / 남 1,500kcal. */
    static final BigDecimal FEMALE_CALORIE_FLOOR = new BigDecimal("1200");
    static final BigDecimal MALE_CALORIE_FLOOR = new BigDecimal("1500");

    /** {@link Evidence#PROT_AGE_2013} 65세 이상 최소 1.2g/kg. */
    static final BigDecimal SENIOR_MIN_PROTEIN_G_PER_KG = new BigDecimal("1.2");

    /** 이 범위를 벗어난 BMI는 키(cm)·몸무게(kg) 단위 착오로 본다 (E-09). */
    static final BigDecimal MIN_PLAUSIBLE_BMI = new BigDecimal("12");
    static final BigDecimal MAX_PLAUSIBLE_BMI = new BigDecimal("70");

    /** {@link Evidence#ESSENTIAL_FAT} 체지방률 입력 하한 남 5% / 여 12% (가정), 상한 60% (E-23). */
    static final BigDecimal MALE_MIN_BODY_FAT_PERCENT = new BigDecimal("5");
    static final BigDecimal FEMALE_MIN_BODY_FAT_PERCENT = new BigDecimal("12");
    static final BigDecimal MAX_BODY_FAT_PERCENT = new BigDecimal("60");

    private static final BigDecimal WEIGHT_STEP = new BigDecimal("0.1");

    private final BodyIndexCalculator bodyIndexCalculator;

    public SafetyPolicy(BodyIndexCalculator bodyIndexCalculator) {
        this.bodyIndexCalculator = bodyIndexCalculator;
    }

    /**
     * 기본값 적용 전 입력 검사. 80세 초과(E-05), BMI 12 미만·70 초과(E-09), 체지방률 범위 밖(E-23),
     * 목표 BMI 18.5 미만(E-13)은 도메인 검증 오류. 19세 미만이면 MINOR_BLOCKED (E-04).
     * 미성년 안내가 다른 검증보다 우선한다.
     */
    public Optional<ReportMode> evaluate(BodyProfile profile) {
        if (profile.age() > MAX_AGE) {
            throw new DomainValidationException("E-05", "age");
        }
        if (profile.age() < MIN_AGE) {
            return Optional.of(ReportMode.MINOR_BLOCKED);
        }
        BigDecimal bmi = bodyIndexCalculator.bmi(profile.heightCm(), profile.weightKg());
        if (bmi.compareTo(MIN_PLAUSIBLE_BMI) < 0 || bmi.compareTo(MAX_PLAUSIBLE_BMI) > 0) {
            throw new DomainValidationException("E-09", "heightCm");
        }
        profile.bodyFatPercent().ifPresent(bodyFat -> {
            if (bodyFat.compareTo(minBodyFatPercent(profile.gender())) < 0
                    || bodyFat.compareTo(MAX_BODY_FAT_PERCENT) > 0) {
                throw new DomainValidationException("E-23", "bodyFatPercent");
            }
        });
        profile.targetWeightKg().ifPresent(target -> {
            BigDecimal targetBmi = bodyIndexCalculator.bmi(profile.heightCm(), target);
            if (targetBmi.compareTo(MIN_HEALTHY_BMI) < 0) {
                throw new DomainValidationException("E-13", "targetWeightKg", minHealthyWeightKg(profile.heightCm()));
            }
        });
        return Optional.empty();
    }

    /** 체지방률 입력 하한 (E-23). */
    public BigDecimal minBodyFatPercent(Gender gender) {
        return gender == Gender.MALE ? MALE_MIN_BODY_FAT_PERCENT : FEMALE_MIN_BODY_FAT_PERCENT;
    }

    /** 건강 범위 최저 체중. 안내값을 그대로 입력해도 BMI 18.5 이상이 되도록 0.1kg 단위로 올린다. */
    public BigDecimal minHealthyWeightKg(BigDecimal heightCm) {
        BigDecimal weight = bodyIndexCalculator.weightForBmi(heightCm, MIN_HEALTHY_BMI);
        if (bodyIndexCalculator.bmi(heightCm, weight).compareTo(MIN_HEALTHY_BMI) < 0) {
            weight = weight.add(WEIGHT_STEP);
        }
        return weight.setScale(1, RoundingMode.UNNECESSARY);
    }

    /**
     * 리포트 모드 (E-11, E-12, E-14, E-15). 기본 목표(BMI 22 / 유지)는 {@code ProfileDefaults}가 정한다.
     */
    public ReportMode decideMode(ResolvedProfile profile) {
        BigDecimal bmi = bodyIndexCalculator.bmi(profile.heightCm(), profile.weightKg());
        if (bmi.compareTo(MIN_HEALTHY_BMI) < 0) {
            return ReportMode.UNDERWEIGHT_MAINTAIN;
        }
        Goal goal = profile.goal().value();
        if (goal instanceof Goal.DefaultHealthyWeight defaultGoal && defaultGoal.maintain()) {
            return ReportMode.MAINTAIN;
        }
        BigDecimal maintainThreshold = profile.weightKg().subtract(MAINTAIN_MARGIN_KG);
        return goal.weightKg().compareTo(maintainThreshold) >= 0 ? ReportMode.MAINTAIN : ReportMode.LOSS;
    }

    /** 65세 이상: 적자 상한 20%, 단백질 하한, 근감소 주의 문구 (E-06). */
    public boolean isSenior(int age) {
        return age >= SENIOR_AGE;
    }

    /** 소비량 대비 적자 상한 비율 (E-06). */
    public BigDecimal maxDeficitRatio(int age) {
        return isSenior(age) ? SENIOR_MAX_DEFICIT_RATIO : MAX_DEFICIT_RATIO;
    }

    /** 주당 체중 대비 감량 상한 비율. */
    public BigDecimal maxWeeklyLossRatio() {
        return MAX_WEEKLY_LOSS_RATIO;
    }

    /** 안전 범위 판단: 첫 4주 평균 주당 감량이 현재 체중의 1% 이하 ({@link Evidence#HELMS_2014}). */
    public boolean isWithinSafeWeeklyLoss(BigDecimal weightKg, BigDecimal weeklyLossKg) {
        return weeklyLossKg.compareTo(weightKg.multiply(MAX_WEEKLY_LOSS_RATIO)) <= 0;
    }

    /** 하루 섭취 하한 (E-16). */
    public BigDecimal calorieFloorKcal(Gender gender) {
        return gender == Gender.MALE ? MALE_CALORIE_FLOOR : FEMALE_CALORIE_FLOOR;
    }

    /** 현재 체중 기준 단백질 하한(g/kg). 65세 미만은 0 (E-06). */
    public BigDecimal minProteinGPerKg(int age) {
        return isSenior(age) ? SENIOR_MIN_PROTEIN_G_PER_KG : BigDecimal.ZERO;
    }

    public List<Evidence> proteinEvidence(int age) {
        return isSenior(age) ? List.of(Evidence.PROT_AGE_2013) : List.of();
    }
}
