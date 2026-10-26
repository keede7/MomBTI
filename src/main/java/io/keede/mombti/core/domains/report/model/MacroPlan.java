package io.keede.mombti.core.domains.report.model;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.math.BigDecimal;
import java.util.List;

/**
 * 하루 탄단지. g은 정수, kcal 합계는 항상 {@code totalKcal}과 같다 (탄수화물로 보정, E-21).
 * 비율(%) 합계는 항상 100이다.
 */
public record MacroPlan(
        BigDecimal totalKcal,
        BigDecimal proteinG,
        BigDecimal proteinKcal,
        BigDecimal fatG,
        BigDecimal fatKcal,
        BigDecimal carbG,
        BigDecimal carbKcal,
        int proteinPercent,
        int fatPercent,
        int carbPercent,
        List<Evidence> evidence
) {
    public MacroPlan {
        evidence = List.copyOf(evidence);
    }
}
