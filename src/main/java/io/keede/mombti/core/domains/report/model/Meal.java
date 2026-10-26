package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 끼니 하나. {@code kcal}은 항목 kcal의 합.
 *
 * @param alternative       대체 메뉴
 * @param convenienceOption 편의점 옵션
 */
public record Meal(MealType type, List<MealItem> items, BigDecimal kcal, String alternative,
                   String convenienceOption) {

    public Meal {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kcal, "kcal");
        Objects.requireNonNull(alternative, "alternative");
        Objects.requireNonNull(convenienceOption, "convenienceOption");
        items = List.copyOf(items);
    }
}
