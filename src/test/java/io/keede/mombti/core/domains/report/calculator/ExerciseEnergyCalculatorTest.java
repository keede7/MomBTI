package io.keede.mombti.core.domains.report.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ExerciseEnergyCalculatorTest {

    /** kcal 허용 오차 ±1. */
    private static final BigDecimal TOLERANCE = BigDecimal.ONE;

    private final ExerciseEnergyCalculator calculator = new ExerciseEnergyCalculator();

    @Test
    void 체중_82kg_주4일은_하루_약_134kcal() {
        // (4.8 − 1) × 82 × 0.75 × 4 ÷ 7 = 133.54
        BigDecimal kcal = calculator.dailyAverageKcal(new BigDecimal("82"), 4);

        assertThat(kcal).isCloseTo(new BigDecimal("134"), within(TOLERANCE));
    }

    @Test
    void 체중_60kg_주3일() {
        // 3.8 × 60 × 0.75 × 3 ÷ 7 = 73.29
        BigDecimal kcal = calculator.dailyAverageKcal(new BigDecimal("60"), 3);

        assertThat(kcal).isCloseTo(new BigDecimal("73.29"), within(TOLERANCE));
    }

    @Test
    void 운동_0회는_0kcal() {
        assertThat(calculator.dailyAverageKcal(new BigDecimal("82"), 0)).isEqualByComparingTo("0");
    }
}
