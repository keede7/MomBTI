package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;

/**
 * 운동을 제외한 평소 활동량 5단계 (ADR-011).
 * 걸음 수 구간은 TUDOR_LOCKE_2004, PAL은 FAO_WHO_UNU_2004 구간 안에서 고른 값.
 */
public enum ActivityLevel {
    SEDENTARY(1, new BigDecimal("1.40"), "거의 앉아 있음", "하루 5천 보 미만"),
    LIGHT(2, new BigDecimal("1.55"), "사무직 + 출퇴근 걷기", "5천~7.5천 보"),
    MODERATE(3, new BigDecimal("1.70"), "서서 일하는 편", "7.5천~1만 보"),
    ACTIVE(4, new BigDecimal("1.85"), "많이 움직이는 직업", "1만~1.25만 보"),
    VERY_ACTIVE(5, new BigDecimal("2.00"), "육체노동", "1.25만 보 이상");

    private final int step;
    private final BigDecimal pal;
    private final String description;
    private final String steps;

    ActivityLevel(int step, BigDecimal pal, String description, String steps) {
        this.step = step;
        this.pal = pal;
        this.description = description;
        this.steps = steps;
    }

    public int step() {
        return step;
    }

    public BigDecimal pal() {
        return pal;
    }

    public String description() {
        return description;
    }

    public String steps() {
        return steps;
    }
}
