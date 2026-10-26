package io.keede.mombti.core.domains.report.calculator;

import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.Estimated;
import io.keede.mombti.core.domains.report.model.Gender;
import java.math.BigDecimal;

/**
 * 기초대사량 (ADR-005). 체지방률 미입력이면 {@link Evidence#MIFFLIN_1990},
 * 입력하면 {@link Evidence#CUNNINGHAM_1991}. 공식 선택 근거는 {@link Evidence#FRANKENFIELD_2005}.
 * 내부 계산용이므로 반올림하지 않는다.
 */
public class BmrCalculator {

    /** MIFFLIN_1990 계수. */
    private static final BigDecimal MIFFLIN_WEIGHT = new BigDecimal("10");
    private static final BigDecimal MIFFLIN_HEIGHT = new BigDecimal("6.25");
    private static final BigDecimal MIFFLIN_AGE = new BigDecimal("5");
    private static final BigDecimal MIFFLIN_MALE_CONSTANT = new BigDecimal("5");
    private static final BigDecimal MIFFLIN_FEMALE_CONSTANT = new BigDecimal("-161");

    /** CUNNINGHAM_1991 계수: 370 + 21.6 × 제지방량. */
    private static final BigDecimal CUNNINGHAM_CONSTANT = new BigDecimal("370");
    private static final BigDecimal CUNNINGHAM_LEAN_MASS = new BigDecimal("21.6");

    private final BodyCompositionEstimator bodyCompositionEstimator;

    public BmrCalculator(BodyCompositionEstimator bodyCompositionEstimator) {
        this.bodyCompositionEstimator = bodyCompositionEstimator;
    }

    /** 남 10W + 6.25H − 5A + 5 / 여 10W + 6.25H − 5A − 161. */
    public BigDecimal mifflinStJeor(Gender gender, int age, BigDecimal heightCm, BigDecimal weightKg) {
        return MIFFLIN_WEIGHT.multiply(weightKg)
                .add(MIFFLIN_HEIGHT.multiply(heightCm))
                .subtract(MIFFLIN_AGE.multiply(BigDecimal.valueOf(age)))
                .add(gender == Gender.MALE ? MIFFLIN_MALE_CONSTANT : MIFFLIN_FEMALE_CONSTANT);
    }

    /** 370 + 21.6 × 제지방량(kg). */
    public BigDecimal cunningham(BigDecimal leanMassKg) {
        return CUNNINGHAM_CONSTANT.add(CUNNINGHAM_LEAN_MASS.multiply(leanMassKg));
    }

    /** 사용자가 체지방률을 입력했을 때만 Cunningham을 쓴다 (추정 체지방률로는 Mifflin). */
    public Estimated<BigDecimal> bmr(ResolvedProfile profile) {
        if (!profile.bodyFatPercent().estimated()) {
            BigDecimal leanMass = bodyCompositionEstimator.leanMassKg(
                    profile.weightKg(), profile.bodyFatPercent().value());
            return Estimated.provided(cunningham(leanMass), Evidence.CUNNINGHAM_1991, Evidence.FRANKENFIELD_2005);
        }
        return Estimated.provided(
                mifflinStJeor(profile.gender(), profile.age(), profile.heightCm(), profile.weightKg()),
                Evidence.MIFFLIN_1990, Evidence.FRANKENFIELD_2005);
    }
}
