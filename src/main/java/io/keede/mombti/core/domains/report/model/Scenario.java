package io.keede.mombti.core.domains.report.model;

/** 식단만 / 식단 + 주 3·4·5일 운동 (ADR-020). */
public enum Scenario {
    DIET_ONLY(0),
    EX3(3),
    EX4(4),
    EX5(5);

    private final int sessionsPerWeek;

    Scenario(int sessionsPerWeek) {
        this.sessionsPerWeek = sessionsPerWeek;
    }

    public int sessionsPerWeek() {
        return sessionsPerWeek;
    }
}
