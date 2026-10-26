package io.keede.mombti.core.domains.report.simulation;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.math.BigDecimal;

/**
 * 같은 시나리오의 계산 변형.
 * 개인차 밴드는 기초대사량 × 0.9 / × 1.1 ({@link Evidence#FRANKENFIELD_2005}),
 * 현실 보정은 계획의 80%만 지킨 경우 ({@link Evidence#ADHERENCE_80}, 가정).
 */
public enum SimulationVariant {
    BASE(BigDecimal.ONE, false, true),
    BMR_LOW(new BigDecimal("0.9"), false, false),
    BMR_HIGH(new BigDecimal("1.1"), false, true),
    ADHERENCE_80(BigDecimal.ONE, true, true);

    private final BigDecimal bmrFactor;
    private final boolean partialAdherence;
    private final boolean monotonicGuarded;

    SimulationVariant(BigDecimal bmrFactor, boolean partialAdherence, boolean monotonicGuarded) {
        this.bmrFactor = bmrFactor;
        this.partialAdherence = partialAdherence;
        this.monotonicGuarded = monotonicGuarded;
    }

    public BigDecimal bmrFactor() {
        return bmrFactor;
    }

    public boolean partialAdherence() {
        return partialAdherence;
    }

    /**
     * 감량 모드에서 체중 단조 비증가를 검사하는지. BMR × 0.9 밴드는 섭취 하한 근처에서 소비량이 섭취보다
     * 작아질 수 있어(공식 오차 표현) 검사하지 않는다.
     */
    public boolean monotonicGuarded() {
        return monotonicGuarded;
    }
}
