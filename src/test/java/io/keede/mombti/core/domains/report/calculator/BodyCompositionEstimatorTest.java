package io.keede.mombti.core.domains.report.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.keede.mombti.core.domains.report.model.Gender;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BodyCompositionEstimatorTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.05");

    private final BodyCompositionEstimator estimator = new BodyCompositionEstimator();

    @Test
    void Deurenberg_남자_수기계산() {
        // 1.20 × 25.0 + 0.23 × 30 − 10.8 × 1 − 5.4 = 20.7
        BigDecimal result = estimator.estimateBodyFatPercent(Gender.MALE, 30, new BigDecimal("25.0"));

        assertThat(result).isCloseTo(new BigDecimal("20.7"), within(TOLERANCE));
    }

    @Test
    void Deurenberg_여자_수기계산() {
        // 1.20 × 22.0 + 0.23 × 40 − 0 − 5.4 = 30.2
        BigDecimal result = estimator.estimateBodyFatPercent(Gender.FEMALE, 40, new BigDecimal("22.0"));

        assertThat(result).isCloseTo(new BigDecimal("30.2"), within(TOLERANCE));
    }

    @Test
    void Deurenberg_남자_수기계산_고령() {
        // 1.20 × 27.5 + 0.23 × 66 − 10.8 − 5.4 = 33.0 + 15.18 − 16.2 = 31.98 → 32.0
        BigDecimal result = estimator.estimateBodyFatPercent(Gender.MALE, 66, new BigDecimal("27.5"));

        assertThat(result).isCloseTo(new BigDecimal("32.0"), within(TOLERANCE));
    }

    @Test
    void 결과는_소수_1자리() {
        BigDecimal result = estimator.estimateBodyFatPercent(Gender.MALE, 33, new BigDecimal("24.37"));

        assertThat(result.scale()).isEqualTo(1);
    }

    @Test
    void BMI_41_이상이면_오차_큼_플래그() {
        // Deurenberg 1991 개발 표본 BMI 13.9~40.9
        assertThat(estimator.isLargeErrorExpected(new BigDecimal("40.99"))).isFalse();
        assertThat(estimator.isLargeErrorExpected(new BigDecimal("41"))).isTrue();
        assertThat(estimator.isLargeErrorExpected(new BigDecimal("55.0"))).isTrue();
    }

    @Test
    void 지방량과_제지방량은_체중을_체지방률로_나눈다() {
        // 80 × 25% = 20, 80 − 20 = 60
        BigDecimal fat = estimator.fatMassKg(new BigDecimal("80.0"), new BigDecimal("25.0"));
        BigDecimal lean = estimator.leanMassKg(new BigDecimal("80.0"), new BigDecimal("25.0"));

        assertThat(fat).isEqualByComparingTo("20");
        assertThat(lean).isEqualByComparingTo("60");
    }

    @Test
    void E24_입력_체지방률이_추정값과_15퍼센트포인트_이상_차이나면_경고() {
        assertThat(estimator.differsFromEstimate(new BigDecimal("22.2"), new BigDecimal("7.2"))).isTrue();
        assertThat(estimator.differsFromEstimate(new BigDecimal("22.2"), new BigDecimal("37.2"))).isTrue();
        assertThat(estimator.differsFromEstimate(new BigDecimal("22.2"), new BigDecimal("7.3"))).isFalse();
        assertThat(estimator.differsFromEstimate(new BigDecimal("22.2"), new BigDecimal("18.0"))).isFalse();
    }
}
