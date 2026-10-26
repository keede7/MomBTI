package io.keede.mombti.core.domains.report.defaults;

import io.keede.mombti.core.domains.report.calculator.BodyCompositionEstimator;
import io.keede.mombti.core.domains.report.calculator.BodyIndexCalculator;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.Estimated;
import io.keede.mombti.core.domains.report.model.Goal;
import java.math.BigDecimal;

/** 선택 항목 기본값의 유일한 구현 위치 (ADR-008, docs/PRD.md "선택 항목 기본값"). */
public class ProfileDefaults {

    /** 활동량 미입력: 좌식 구간 하한 (FAO_WHO_UNU_2004 1.40~1.69). */
    static final ActivityLevel DEFAULT_ACTIVITY = ActivityLevel.SEDENTARY;
    /** 활동량 미입력 시 개인차 밴드 상단 PAL (FAO_WHO_UNU_2004 좌식·가벼운 활동 구간 안의 값). */
    static final BigDecimal DEFAULT_PAL_BAND_UPPER = new BigDecimal("1.60");
    /** 이 BMI 이상이면 기본 목표로 감량 (KSSO_2022 비만 전단계 하한). */
    static final BigDecimal LOSS_GOAL_BMI_THRESHOLD = new BigDecimal("23");
    /** 기본 목표 BMI (KSSO_2022 정상 범위 18.5~22.9의 중간값). */
    static final BigDecimal DEFAULT_GOAL_BMI = new BigDecimal("22.0");

    private final BodyIndexCalculator bodyIndexCalculator;
    private final BodyCompositionEstimator bodyCompositionEstimator;

    public ProfileDefaults(BodyIndexCalculator bodyIndexCalculator,
                           BodyCompositionEstimator bodyCompositionEstimator) {
        this.bodyIndexCalculator = bodyIndexCalculator;
        this.bodyCompositionEstimator = bodyCompositionEstimator;
    }

    public ResolvedProfile resolve(BodyProfile profile) {
        BigDecimal bmi = bodyIndexCalculator.bmi(profile.heightCm(), profile.weightKg());

        Estimated<ActivityLevel> activity = profile.activityLevel()
                .map(level -> Estimated.provided(level, Evidence.FAO_WHO_UNU_2004, Evidence.TUDOR_LOCKE_2004))
                .orElseGet(() -> Estimated.estimated(DEFAULT_ACTIVITY,
                        Evidence.FAO_WHO_UNU_2004, Evidence.TUDOR_LOCKE_2004));
        BigDecimal palBandUpper = profile.activityLevel().isPresent()
                ? activity.value().pal()
                : DEFAULT_PAL_BAND_UPPER;

        Estimated<Goal> goal = profile.targetWeightKg()
                .<Estimated<Goal>>map(weight -> Estimated.provided(new Goal.TargetWeight(weight)))
                .orElseGet(() -> Estimated.estimated(defaultGoal(profile, bmi), Evidence.KSSO_2022));

        Estimated<BigDecimal> bodyFat = profile.bodyFatPercent()
                .map(Estimated::provided)
                .orElseGet(() -> Estimated.estimated(
                        bodyCompositionEstimator.estimateBodyFatPercent(profile.gender(), profile.age(), bmi),
                        Evidence.DEURENBERG_1991));

        return new ResolvedProfile(profile.gender(), profile.age(), profile.heightCm(), profile.weightKg(),
                activity, palBandUpper, goal, bodyFat);
    }

    private Goal defaultGoal(BodyProfile profile, BigDecimal bmi) {
        if (bmi.compareTo(LOSS_GOAL_BMI_THRESHOLD) >= 0) {
            return new Goal.DefaultHealthyWeight(
                    bodyIndexCalculator.weightForBmi(profile.heightCm(), DEFAULT_GOAL_BMI), false);
        }
        return new Goal.DefaultHealthyWeight(profile.weightKg(), true);
    }
}
