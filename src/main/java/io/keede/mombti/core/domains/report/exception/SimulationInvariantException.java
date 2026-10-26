package io.keede.mombti.core.domains.report.exception;

/**
 * 시뮬레이션 불변식 위반 (E-29). 메시지에는 불변식 이름과 일차만 담는다 (사용자 입력값 금지).
 */
public class SimulationInvariantException extends RuntimeException {

    private final String invariant;

    public SimulationInvariantException(String invariant, int day) {
        super("invariant=" + invariant + ", day=" + day);
        this.invariant = invariant;
    }

    public String invariant() {
        return invariant;
    }
}
