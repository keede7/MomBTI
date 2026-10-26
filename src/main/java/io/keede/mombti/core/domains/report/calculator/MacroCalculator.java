package io.keede.mombti.core.domains.report.calculator;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.MacroPlan;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 하루 탄단지. 단백질 → 지방 → 나머지는 탄수화물. 반올림 오차는 탄수화물 kcal에서 보정한다 (E-21).
 */
public class MacroCalculator {

    /** {@link Evidence#LEIDY_2015} 감량 시 1.2~1.6g/kg 중 상단 (보조: {@link Evidence#MORTON_2018}). */
    static final BigDecimal PROTEIN_G_PER_KG = new BigDecimal("1.6");
    /** 이 BMI 이상이면 목표 체중 기준으로 단백질을 계산한다 (KSSO_2022 2단계 비만 하한). */
    static final BigDecimal PROTEIN_GOAL_WEIGHT_BMI = new BigDecimal("30");
    /** {@link Evidence#IOM_AMDR_2005} 지방 20~35% 중 25%. */
    static final BigDecimal FAT_RATIO = new BigDecimal("0.25");

    private static final BigDecimal PROTEIN_KCAL_PER_G = new BigDecimal("4");
    private static final BigDecimal FAT_KCAL_PER_G = new BigDecimal("9");
    private static final BigDecimal CARB_KCAL_PER_G = new BigDecimal("4");
    private static final BigDecimal PERCENT = new BigDecimal("100");

    private final SafetyPolicy safetyPolicy;

    public MacroCalculator(SafetyPolicy safetyPolicy) {
        this.safetyPolicy = safetyPolicy;
    }

    /**
     * @param totalKcal  하루 섭취 목표 (10kcal 단위)
     * @param bmi        현재 BMI
     * @param goalWeightKg 목표 체중. 유지 모드면 현재 체중
     */
    public MacroPlan calculate(BigDecimal totalKcal, BigDecimal weightKg, BigDecimal goalWeightKg,
                               BigDecimal bmi, int age) {
        List<Evidence> evidence = new ArrayList<>(List.of(
                Evidence.LEIDY_2015, Evidence.MORTON_2018, Evidence.IOM_AMDR_2005));

        BigDecimal proteinBasis = bmi.compareTo(PROTEIN_GOAL_WEIGHT_BMI) >= 0 ? goalWeightKg : weightKg;
        BigDecimal proteinRaw = PROTEIN_G_PER_KG.multiply(proteinBasis);
        // 65세 이상 단백질 하한은 SafetyPolicy (E-06). 65세 미만은 0이라 영향 없음
        proteinRaw = proteinRaw.max(safetyPolicy.minProteinGPerKg(age).multiply(weightKg));
        evidence.addAll(safetyPolicy.proteinEvidence(age));
        BigDecimal proteinG = proteinRaw.setScale(0, RoundingMode.HALF_UP);
        BigDecimal proteinKcal = proteinG.multiply(PROTEIN_KCAL_PER_G);

        BigDecimal fatG = totalKcal.multiply(FAT_RATIO).divide(FAT_KCAL_PER_G, 0, RoundingMode.HALF_UP);
        BigDecimal fatKcal = fatG.multiply(FAT_KCAL_PER_G);

        BigDecimal carbKcal = totalKcal.subtract(proteinKcal).subtract(fatKcal);
        if (carbKcal.signum() < 0) {
            throw new IllegalStateException("탄수화물 kcal이 음수");
        }
        BigDecimal carbG = carbKcal.divide(CARB_KCAL_PER_G, 0, RoundingMode.HALF_UP);

        int proteinPercent = percentOf(proteinKcal, totalKcal);
        int fatPercent = percentOf(fatKcal, totalKcal);
        int carbPercent = 100 - proteinPercent - fatPercent;

        return new MacroPlan(totalKcal, proteinG, proteinKcal, fatG, fatKcal, carbG, carbKcal,
                proteinPercent, fatPercent, carbPercent, evidence);
    }

    private static int percentOf(BigDecimal part, BigDecimal total) {
        return part.multiply(PERCENT).divide(total, MathContext.DECIMAL64)
                .setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
