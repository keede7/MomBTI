package io.keede.mombti.core.domains.report.calculator;

import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.EnergyTarget;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 하루 섭취 목표. 적자 = min(소비량 × 상한 비율, 주당 체중 1% × ρ ÷ 7), 섭취 = max(하한, 소비량 − 적자).
 * 체중 1%의 kcal 환산은 정적 규칙이 아니라 시작 시점 Forbes 분배로 구한 에너지 밀도 ρ를 쓴다 (ADR-004, ADR-010).
 */
public class EnergyTargetCalculator {

    /** {@link Evidence#HALL_2007_FORBES} p = 10.4 / (10.4 + F). */
    public static final BigDecimal FORBES_CONSTANT = new BigDecimal("10.4");
    /** {@link Evidence#HALL_2008} 제지방 7.6MJ/kg ≈ 1,816kcal/kg. */
    public static final BigDecimal LEAN_ENERGY_DENSITY = new BigDecimal("1816");
    /** {@link Evidence#HALL_2008} 지방 39.5MJ/kg ≈ 9,440kcal/kg. */
    public static final BigDecimal FAT_ENERGY_DENSITY = new BigDecimal("9440");

    private static final BigDecimal DAYS_PER_WEEK = new BigDecimal("7");
    private static final BigDecimal KCAL_STEP = new BigDecimal("10");

    private final SafetyPolicy safetyPolicy;
    private final BodyCompositionEstimator bodyCompositionEstimator;

    public EnergyTargetCalculator(SafetyPolicy safetyPolicy, BodyCompositionEstimator bodyCompositionEstimator) {
        this.safetyPolicy = safetyPolicy;
        this.bodyCompositionEstimator = bodyCompositionEstimator;
    }

    /** 체중 변화에서 제지방이 차지하는 비율 p = 10.4 / (10.4 + 지방량). */
    public BigDecimal leanFraction(BigDecimal fatMassKg) {
        return FORBES_CONSTANT.divide(FORBES_CONSTANT.add(fatMassKg), MathContext.DECIMAL64);
    }

    /** 에너지 밀도 ρ = p × 1,816 + (1 − p) × 9,440 (kcal/kg). */
    public BigDecimal energyDensityKcalPerKg(BigDecimal fatMassKg) {
        BigDecimal p = leanFraction(fatMassKg);
        return p.multiply(LEAN_ENERGY_DENSITY).add(BigDecimal.ONE.subtract(p).multiply(FAT_ENERGY_DENSITY));
    }

    /** 주당 체중 1% 감량에 해당하는 하루 적자(kcal) = 체중 × 1% × ρ ÷ 7. */
    public BigDecimal weeklyLossCapKcalPerDay(BigDecimal weightKg, BigDecimal fatMassKg) {
        return weightKg.multiply(safetyPolicy.maxWeeklyLossRatio())
                .multiply(energyDensityKcalPerKg(fatMassKg))
                .divide(DAYS_PER_WEEK, MathContext.DECIMAL64);
    }

    public EnergyTarget calculate(ResolvedProfile profile, BigDecimal tdeeKcal, ReportMode mode) {
        if (mode != ReportMode.LOSS) {
            return new EnergyTarget(tdeeKcal, roundToTen(tdeeKcal), BigDecimal.ZERO, false, true,
                    List.of(Evidence.FAO_WHO_UNU_2004));
        }
        List<Evidence> evidence = new ArrayList<>(List.of(
                Evidence.HELMS_2014, Evidence.HALL_2007_FORBES, Evidence.HALL_2008));

        BigDecimal fatMassKg = bodyCompositionEstimator.fatMassKg(
                profile.weightKg(), profile.bodyFatPercent().value());
        BigDecimal ratioCap = tdeeKcal.multiply(safetyPolicy.maxDeficitRatio(profile.age()));
        BigDecimal deficit = ratioCap.min(weeklyLossCapKcalPerDay(profile.weightKg(), fatMassKg));

        BigDecimal floor = safetyPolicy.calorieFloorKcal(profile.gender());
        BigDecimal intake = roundToTen(tdeeKcal.subtract(deficit));
        boolean floorApplied = intake.compareTo(floor) < 0;
        if (floorApplied) {
            intake = floor;
            evidence.add(Evidence.AHA_ACC_TOS_2013);
        }

        BigDecimal actualDeficit = tdeeKcal.subtract(intake);
        boolean dietOnlyMaintains = actualDeficit.signum() <= 0;
        return new EnergyTarget(tdeeKcal, intake, roundToTen(actualDeficit.max(BigDecimal.ZERO)),
                floorApplied, dietOnlyMaintains, evidence);
    }

    private static BigDecimal roundToTen(BigDecimal kcal) {
        return kcal.divide(KCAL_STEP, 0, RoundingMode.HALF_UP).multiply(KCAL_STEP);
    }
}
