package io.keede.mombti.core.domains.report.calculator;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.math.BigDecimal;

/** 하루 소비량 = 기초대사량 × PAL ({@link Evidence#FAO_WHO_UNU_2004}). 운동은 포함하지 않는다 (ADR-011). */
public class TdeeCalculator {

    /** {@link Evidence#FRANKENFIELD_2005} 기초대사량 공식 오차 약 ±10%. */
    static final BigDecimal BMR_BAND_LOWER = new BigDecimal("0.9");
    static final BigDecimal BMR_BAND_UPPER = new BigDecimal("1.1");

    public BigDecimal tdee(BigDecimal bmr, BigDecimal pal) {
        return bmr.multiply(pal);
    }

    /** 하루 소비량 범위 하단 = 기초대사량 × 0.9 × PAL. */
    public BigDecimal lowerBound(BigDecimal bmr, BigDecimal pal) {
        return tdee(bmr.multiply(BMR_BAND_LOWER), pal);
    }

    /** 하루 소비량 범위 상단 = 기초대사량 × 1.1 × 밴드 상단 PAL (활동량 미입력이면 1.60). */
    public BigDecimal upperBound(BigDecimal bmr, BigDecimal palBandUpper) {
        return tdee(bmr.multiply(BMR_BAND_UPPER), palBandUpper);
    }
}
