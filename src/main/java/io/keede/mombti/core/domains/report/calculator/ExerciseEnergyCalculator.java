package io.keede.mombti.core.domains.report.calculator;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * 운동 소비량의 하루 평균 (ADR-020): (MET − 1) × 체중 × 시간 × 주 N회 ÷ 7.
 * 1 MET ≈ 1kcal/kg/h 근사.
 */
public class ExerciseEnergyCalculator {

    /** 운동 라인에 쓰는 출처. */
    public static final List<Evidence> EVIDENCE = List.of(Evidence.HERRMANN_2024, Evidence.EXERCISE_SESSION_45);

    /** {@link Evidence#HERRMANN_2024} 코드 17200 빠르게 걷기. */
    public static final BigDecimal BRISK_WALKING_MET = new BigDecimal("4.8");
    /** 순소비량에서 빼는 휴식 대사 1 MET. */
    private static final BigDecimal RESTING_MET = BigDecimal.ONE;
    /** {@link Evidence#EXERCISE_SESSION_45} 운동 1회 45분 (가정). */
    public static final int SESSION_MINUTES = 45;
    static final BigDecimal SESSION_HOURS = BigDecimal.valueOf(SESSION_MINUTES)
            .divide(new BigDecimal("60"), MathContext.DECIMAL64);
    private static final BigDecimal DAYS_PER_WEEK = new BigDecimal("7");

    /** 하루 평균 운동 순소비량(kcal). 내부 계산용이므로 반올림하지 않는다. */
    public BigDecimal dailyAverageKcal(BigDecimal weightKg, int sessionsPerWeek) {
        return BRISK_WALKING_MET.subtract(RESTING_MET)
                .multiply(weightKg)
                .multiply(SESSION_HOURS)
                .multiply(BigDecimal.valueOf(sessionsPerWeek))
                .divide(DAYS_PER_WEEK, MathContext.DECIMAL64);
    }
}
