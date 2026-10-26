package io.keede.mombti.core.domains.report.model;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.util.List;

/** 단계별 목표: 지금 → −5% → −10% → 목표. −5%는 WING_2011 (5~10% 감량에서 위험 요인 개선). */
public enum MilestoneType {
    CURRENT(List.of()),
    LOSS_5_PERCENT(List.of(Evidence.WING_2011)),
    LOSS_10_PERCENT(List.of()),
    TARGET(List.of());

    private final List<Evidence> evidence;

    MilestoneType(List<Evidence> evidence) {
        this.evidence = evidence;
    }

    public List<Evidence> evidence() {
        return evidence;
    }
}
