package io.keede.mombti.core.domains.report.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectionTest {

    private final Projection projection = new Projection(Scenario.DIET_ONLY, List.of(
            point(0, "80.000"), point(4, "77.600"), point(12, "74.150")));

    private static ProjectionPoint point(int week, String weightKg) {
        return new ProjectionPoint(week, new BigDecimal(weightKg), BigDecimal.ONE, BigDecimal.ONE);
    }

    @Test
    void N주_체중과_시작_대비_변화량() {
        assertThat(projection.weightAt(12)).isEqualByComparingTo("74.150");
        assertThat(projection.changeKg(12)).isEqualByComparingTo("-5.850");
    }

    @Test
    void 첫_4주_평균_주당_감량() {
        // (80.0 − 77.6) ÷ 4 = 0.6kg/주
        assertThat(projection.averageWeeklyLossKg(4)).isEqualByComparingTo("0.600");
    }
}
