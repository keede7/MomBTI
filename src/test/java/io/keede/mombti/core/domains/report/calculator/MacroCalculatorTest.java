package io.keede.mombti.core.domains.report.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.MacroPlan;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MacroCalculatorTest {

    private final MacroCalculator calculator = new MacroCalculator(new SafetyPolicy(new BodyIndexCalculator()));

    @Test
    void 단백질은_현재_체중_곱하기_16g() {
        // 1.6 × 65 = 104g = 416kcal
        MacroPlan plan = calculator.calculate(new BigDecimal("1270"), new BigDecimal("65.0"),
                new BigDecimal("58.0"), new BigDecimal("24.0"), 30);

        assertThat(plan.proteinG()).isEqualByComparingTo("104");
        assertThat(plan.proteinKcal()).isEqualByComparingTo("416");
        assertThat(plan.evidence()).contains(Evidence.LEIDY_2015, Evidence.IOM_AMDR_2005);
    }

    @Test
    void BMI_30_이상이면_목표_체중_기준_단백질() {
        // 1.6 × 70 = 112g
        MacroPlan plan = calculator.calculate(new BigDecimal("2000"), new BigDecimal("100.0"),
                new BigDecimal("70.0"), new BigDecimal("30"), 40);

        assertThat(plan.proteinG()).isEqualByComparingTo("112");
    }

    @Test
    void E06_65세_이상은_현재_체중_기준_최소_12g() {
        // 목표 기준 1.6 × 60 = 96g < 현재 기준 1.2 × 95 = 114g → 114g
        MacroPlan plan = calculator.calculate(new BigDecimal("1800"), new BigDecimal("95.0"),
                new BigDecimal("60.0"), new BigDecimal("33.0"), 70);

        assertThat(plan.proteinG()).isEqualByComparingTo("114");
        assertThat(plan.evidence()).contains(Evidence.PROT_AGE_2013);
    }

    @Test
    void 지방은_섭취_kcal의_25퍼센트() {
        // 2000 × 25% = 500kcal ÷ 9 = 55.6 → 56g = 504kcal
        MacroPlan plan = calculator.calculate(new BigDecimal("2000"), new BigDecimal("70.0"),
                new BigDecimal("65.0"), new BigDecimal("24.0"), 30);

        assertThat(plan.fatG()).isEqualByComparingTo("56");
        assertThat(plan.fatKcal()).isEqualByComparingTo("504");
    }

    @Test
    void E21_탄단지_반올림_합계가_목표와_다르면_탄수화물로_보정() {
        // 여 65kg, 1270kcal: 단백질 104g(416), 지방 317.5÷9 = 35.3 → 35g(315)
        // 탄수화물 (1270 − 416 − 315) = 539kcal ÷ 4 = 134.75 → 135g. 135 × 4 = 540이면 합계 1271 ≠ 1270
        MacroPlan plan = calculator.calculate(new BigDecimal("1270"), new BigDecimal("65.0"),
                new BigDecimal("58.0"), new BigDecimal("24.0"), 30);

        assertThat(plan.carbG()).isEqualByComparingTo("135");
        assertThat(plan.carbG().multiply(BigDecimal.valueOf(4))).isNotEqualByComparingTo("539");
        assertThat(plan.carbKcal()).isEqualByComparingTo("539");
        assertThat(plan.proteinKcal().add(plan.fatKcal()).add(plan.carbKcal())).isEqualByComparingTo("1270");
        assertThat(plan.proteinPercent() + plan.fatPercent() + plan.carbPercent()).isEqualTo(100);
    }

    @Test
    void 비율은_kcal_기준_정수_퍼센트() {
        // 단백질 416/1270 = 32.8% → 33, 지방 315/1270 = 24.8% → 25, 탄수화물 100 − 58 = 42 (IOM_AMDR_2005 FAQ 예시)
        MacroPlan plan = calculator.calculate(new BigDecimal("1270"), new BigDecimal("65.0"),
                new BigDecimal("58.0"), new BigDecimal("24.0"), 30);

        assertThat(plan.proteinPercent()).isEqualTo(33);
        assertThat(plan.fatPercent()).isEqualTo(25);
        assertThat(plan.carbPercent()).isEqualTo(42);
    }
}
