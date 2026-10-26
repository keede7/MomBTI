package io.keede.mombti.core.domains.report.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.keede.mombti.core.domains.report.model.BmiCategory;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BodyIndexCalculatorTest {

    private final BodyIndexCalculator calculator = new BodyIndexCalculator();

    @Test
    void BMI는_몸무게_나누기_키m_제곱() {
        // 80 / 1.8² = 24.691...
        BigDecimal bmi = calculator.bmi(new BigDecimal("180.0"), new BigDecimal("80.0"));

        assertThat(bmi).isCloseTo(new BigDecimal("24.69"), within(new BigDecimal("0.01")));
    }

    @Test
    void BMI에_해당하는_체중은_소수_1자리() {
        // 22 × 1.75² = 67.375 → 67.4
        BigDecimal weight = calculator.weightForBmi(new BigDecimal("175.0"), new BigDecimal("22.0"));

        assertThat(weight).isEqualByComparingTo("67.4");
        assertThat(weight.scale()).isEqualTo(1);
    }

    @ParameterizedTest(name = "BMI {0} → {1}")
    @CsvSource({
            "12.0,  UNDERWEIGHT",
            "18.49, UNDERWEIGHT",
            "18.5,  NORMAL",
            "22.99, NORMAL",
            "23,    PRE_OBESE",
            "24.99, PRE_OBESE",
            "25,    OBESE_1",
            "29.99, OBESE_1",
            "30,    OBESE_2_PLUS",
            "45.0,  OBESE_2_PLUS"
    })
    void KSSO_2022_BMI_구간_경계값(String bmi, BmiCategory expected) {
        assertThat(calculator.category(new BigDecimal(bmi))).isEqualTo(expected);
    }
}
