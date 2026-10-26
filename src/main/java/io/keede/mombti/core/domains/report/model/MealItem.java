package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 끼니의 음식 하나.
 *
 * @param portion 생활 단위 분량 ("2/3공기", "약 1과 1/3개")
 * @param grams   분량(g), 정수
 * @param kcal    열량, 정수
 */
public record MealItem(String name, String portion, BigDecimal grams, BigDecimal kcal) {

    public MealItem {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(portion, "portion");
        Objects.requireNonNull(grams, "grams");
        Objects.requireNonNull(kcal, "kcal");
    }
}
