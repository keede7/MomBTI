package io.keede.mombti.core.domains.report.calculator;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.BmiCategory;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/** BMI와 {@link Evidence#KSSO_2022} 구간. */
public class BodyIndexCalculator {

    public static final List<Evidence> EVIDENCE = List.of(Evidence.KSSO_2022);

    private static final BigDecimal CM_PER_M = new BigDecimal("100");

    /** KSSO_2022 저체중 상한 (미만이면 저체중). */
    static final BigDecimal UNDERWEIGHT_UPPER = new BigDecimal("18.5");
    /** KSSO_2022 비만 전단계 하한. */
    static final BigDecimal PRE_OBESE_LOWER = new BigDecimal("23");
    /** KSSO_2022 1단계 비만 하한. */
    static final BigDecimal OBESE_1_LOWER = new BigDecimal("25");
    /** KSSO_2022 2단계 비만 하한 (MVP는 이상을 한 구간으로 묶음). */
    static final BigDecimal OBESE_2_LOWER = new BigDecimal("30");

    /** 몸무게(kg) / 키(m)². 내부 계산용이므로 반올림하지 않는다 (DECIMAL64 정밀도). */
    public BigDecimal bmi(BigDecimal heightCm, BigDecimal weightKg) {
        return weightKg.divide(heightMSquared(heightCm), MathContext.DECIMAL64);
    }

    /** 주어진 BMI에 해당하는 체중. 0.1kg 단위 반올림. */
    public BigDecimal weightForBmi(BigDecimal heightCm, BigDecimal bmi) {
        return bmi.multiply(heightMSquared(heightCm)).setScale(1, RoundingMode.HALF_UP);
    }

    /** KSSO_2022 구간: <18.5 / 18.5~22.9 / 23~24.9 / 25~29.9 / ≥30. */
    public BmiCategory category(BigDecimal bmi) {
        if (bmi.compareTo(UNDERWEIGHT_UPPER) < 0) return BmiCategory.UNDERWEIGHT;
        if (bmi.compareTo(PRE_OBESE_LOWER) < 0) return BmiCategory.NORMAL;
        if (bmi.compareTo(OBESE_1_LOWER) < 0) return BmiCategory.PRE_OBESE;
        if (bmi.compareTo(OBESE_2_LOWER) < 0) return BmiCategory.OBESE_1;
        return BmiCategory.OBESE_2_PLUS;
    }

    private static BigDecimal heightMSquared(BigDecimal heightCm) {
        BigDecimal heightM = heightCm.divide(CM_PER_M, MathContext.DECIMAL64);
        return heightM.multiply(heightM, MathContext.DECIMAL64);
    }
}
