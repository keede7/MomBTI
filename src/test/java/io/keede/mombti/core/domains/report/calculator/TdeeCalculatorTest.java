package io.keede.mombti.core.domains.report.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TdeeCalculatorTest {

    private final TdeeCalculator calculator = new TdeeCalculator();

    @Test
    void 하루_소비량은_BMR_곱하기_PAL() {
        // 1780 × 1.55 = 2759
        BigDecimal tdee = calculator.tdee(new BigDecimal("1780"), new BigDecimal("1.55"));

        assertThat(tdee).isCloseTo(new BigDecimal("2759"), within(BigDecimal.ONE));
    }

    @Test
    void 기본_활동량_PAL_140() {
        // 1345.25 × 1.40 = 1883.35
        BigDecimal tdee = calculator.tdee(new BigDecimal("1345.25"), new BigDecimal("1.40"));

        assertThat(tdee).isCloseTo(new BigDecimal("1883.35"), within(BigDecimal.ONE));
    }

    @Test
    void 하루_소비량_범위는_BMR_10퍼센트_오차와_PAL_밴드_상단() {
        // 하한 1345.25 × 0.9 × 1.40 = 1695.02, 상한 1345.25 × 1.1 × 1.60 = 2367.64 (FRANKENFIELD_2005 ±10%)
        BigDecimal bmr = new BigDecimal("1345.25");

        assertThat(calculator.lowerBound(bmr, new BigDecimal("1.40")))
                .isCloseTo(new BigDecimal("1695.02"), within(BigDecimal.ONE));
        assertThat(calculator.upperBound(bmr, new BigDecimal("1.60")))
                .isCloseTo(new BigDecimal("2367.64"), within(BigDecimal.ONE));
    }
}
