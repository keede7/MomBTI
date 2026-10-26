package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 하루 한식 식단. 가장 가까운 템플릿을 목표 kcal에 맞게 비율로 조정한 결과.
 *
 * @param templateKcal       고른 템플릿의 구간 kcal (1,200~3,600)
 * @param scale              분량 조정 비율 = 목표 kcal / 템플릿 합계 kcal
 * @param outOfTemplateRange 목표 kcal이 템플릿 범위 밖이라 가장 가까운 끝 템플릿을 조정했는지 (E-22)
 * @param totalKcal          끼니 kcal의 합
 */
public record MealPlan(
        BigDecimal targetKcal,
        BigDecimal templateKcal,
        BigDecimal scale,
        boolean outOfTemplateRange,
        List<Meal> meals,
        BigDecimal totalKcal
) {
    public MealPlan {
        Objects.requireNonNull(targetKcal, "targetKcal");
        Objects.requireNonNull(templateKcal, "templateKcal");
        Objects.requireNonNull(scale, "scale");
        Objects.requireNonNull(totalKcal, "totalKcal");
        meals = List.copyOf(meals);
    }
}
