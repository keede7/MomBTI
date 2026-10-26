package io.keede.mombti.core.domains.report.model;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.math.BigDecimal;
import java.util.List;

/**
 * 하루 섭취 목표.
 *
 * @param intakeKcal        10kcal 단위 섭취 목표
 * @param deficitKcal       실제 적자 = max(0, 소비량 − 섭취 목표)를 10kcal 단위로 반올림
 * @param floorApplied      섭취 하한으로 올렸는지 ("최소 권장량으로 맞췄어요", E-16)
 * @param dietOnlyMaintains 적자가 0 이하라 식단만 라인은 유지선 (E-17, 유지 모드 포함)
 */
public record EnergyTarget(
        BigDecimal tdeeKcal,
        BigDecimal intakeKcal,
        BigDecimal deficitKcal,
        boolean floorApplied,
        boolean dietOnlyMaintains,
        List<Evidence> evidence
) {
    public EnergyTarget {
        evidence = List.copyOf(evidence);
    }
}
