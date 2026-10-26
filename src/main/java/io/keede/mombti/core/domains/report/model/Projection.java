package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 시나리오 하나의 0~52주 예측. */
public record Projection(Scenario scenario, List<ProjectionPoint> points) {

    private static final int WEIGHT_SCALE = 3;

    public Projection {
        points = List.copyOf(points);
    }

    /** 기록된 주차의 체중. */
    public BigDecimal weightAt(int week) {
        return points.stream()
                .filter(point -> point.week() == week)
                .map(ProjectionPoint::weightKg)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("week=" + week));
    }

    /** 시작 대비 N주 뒤 체중 변화 (감량이면 음수). */
    public BigDecimal changeKg(int week) {
        return weightAt(week).subtract(weightAt(0));
    }

    /** 첫 N주 평균 주당 감량 (늘면 음수). */
    public BigDecimal averageWeeklyLossKg(int weeks) {
        return changeKg(weeks).negate().divide(BigDecimal.valueOf(weeks), WEIGHT_SCALE, RoundingMode.HALF_UP);
    }
}
