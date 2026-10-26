package io.keede.mombti.core.domains.report.calculator;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.Gender;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 체지방률 추정. {@link Evidence#DEURENBERG_1991}:
 * 1.20 × BMI + 0.23 × 나이 − 10.8 × 성별(남 1, 여 0) − 5.4
 */
public class BodyCompositionEstimator {

    private static final BigDecimal BMI_COEFFICIENT = new BigDecimal("1.20");
    private static final BigDecimal AGE_COEFFICIENT = new BigDecimal("0.23");
    private static final BigDecimal MALE_COEFFICIENT = new BigDecimal("10.8");
    private static final BigDecimal CONSTANT = new BigDecimal("5.4");
    /** DEURENBERG_1991 개발 표본 BMI 13.9~40.9. 이 값 이상은 범위 밖 추정. */
    static final BigDecimal LARGE_ERROR_BMI = new BigDecimal("41");

    /** 입력 체지방률이 추정값과 이만큼(%p) 이상 다르면 경고만 하고 입력값을 쓴다 (E-24). */
    static final BigDecimal MISMATCH_PERCENT_POINT = new BigDecimal("15");

    private static final BigDecimal PERCENT = new BigDecimal("100");

    /** 체지방률(%), 소수 1자리 반올림. */
    public BigDecimal estimateBodyFatPercent(Gender gender, int age, BigDecimal bmi) {
        BigDecimal result = BMI_COEFFICIENT.multiply(bmi)
                .add(AGE_COEFFICIENT.multiply(BigDecimal.valueOf(age)))
                .subtract(gender == Gender.MALE ? MALE_COEFFICIENT : BigDecimal.ZERO)
                .subtract(CONSTANT);
        return result.setScale(1, RoundingMode.HALF_UP);
    }

    /** BMI ≥ 41이면 추정 체지방률 옆에 "오차가 클 수 있음"을 표시한다. */
    public boolean isLargeErrorExpected(BigDecimal bmi) {
        return bmi.compareTo(LARGE_ERROR_BMI) >= 0;
    }

    /** 입력 체지방률이 BMI 기반 추정값과 15%p 이상 차이나는지 (E-24). */
    public boolean differsFromEstimate(BigDecimal estimatedPercent, BigDecimal providedPercent) {
        return estimatedPercent.subtract(providedPercent).abs().compareTo(MISMATCH_PERCENT_POINT) >= 0;
    }

    /** 지방량(kg) = 체중 × 체지방률 / 100. 내부 계산용이므로 반올림하지 않는다. */
    public BigDecimal fatMassKg(BigDecimal weightKg, BigDecimal bodyFatPercent) {
        return weightKg.multiply(bodyFatPercent).divide(PERCENT, MathContext.DECIMAL64);
    }

    /** 제지방량(kg) = 체중 − 지방량. */
    public BigDecimal leanMassKg(BigDecimal weightKg, BigDecimal bodyFatPercent) {
        return weightKg.subtract(fatMassKg(weightKg, bodyFatPercent));
    }
}
