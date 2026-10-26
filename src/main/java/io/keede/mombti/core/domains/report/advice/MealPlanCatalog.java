package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.model.Meal;
import io.keede.mombti.core.domains.report.model.MealItem;
import io.keede.mombti.core.domains.report.model.MealPlan;
import io.keede.mombti.core.domains.report.model.MealType;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 칼로리 구간별 한식 끼니 템플릿 (1,200~3,600kcal, 200kcal 간격).
 * 가장 가까운 템플릿을 골라 목표 kcal에 맞게 분량을 비율로 조정한다. 범위 밖이면 끝 템플릿을 조정한다 (E-22).
 * 식품 열량은 1회 분량의 일반적인 대략값이며, 식단은 하루 섭취 목표를 보여주기 위한 예시다.
 */
public class MealPlanCatalog {

    /** 분량은 1/6 단위 정수로 적어 2/3 같은 분수를 정확히 다룬다. */
    private static final int SIXTHS = 6;
    private static final BigDecimal SIX = BigDecimal.valueOf(SIXTHS);
    /** 조정한 분량이 생활 단위 분수에서 이만큼(1/6 단위, 0.05단위) 넘게 벗어나면 "약"을 붙인다. */
    private static final BigDecimal APPROX_THRESHOLD_SIXTHS = new BigDecimal("0.3");

    private enum Food {
        MULTIGRAIN_RICE("잡곡밥", "공기", 210, 315),
        EGG("삶은 계란", "개", 50, 75),
        CHICKEN_BREAST("닭가슴살", "팩", 100, 110),
        MACKEREL("고등어구이", "토막", 80, 180),
        KIMCHI("배추김치", "접시", 40, 15),
        NAMUL("나물 반찬", "접시", 70, 50),
        DOENJANG_SOUP("된장국", "그릇", 250, 80),
        SALAD("채소 샐러드", "접시", 80, 20),
        APPLE("사과", "개", 200, 105),
        BANANA("바나나", "개", 120, 105),
        GREEK_YOGURT("플레인 요거트", "컵", 100, 100),
        NUTS("견과류", "줌", 20, 120),
        LOW_FAT_MILK("저지방 우유", "컵", 200, 90),
        SWEET_POTATO("찐 고구마", "개", 150, 195);

        private final String label;
        private final String unit;
        private final BigDecimal gramsPerUnit;
        private final BigDecimal kcalPerUnit;

        Food(String label, String unit, int gramsPerUnit, int kcalPerUnit) {
            this.label = label;
            this.unit = unit;
            this.gramsPerUnit = BigDecimal.valueOf(gramsPerUnit);
            this.kcalPerUnit = BigDecimal.valueOf(kcalPerUnit);
        }
    }

    private record Portion(Food food, int sixths) {

        BigDecimal units() {
            return BigDecimal.valueOf(sixths).divide(SIX, MathContext.DECIMAL64);
        }

        BigDecimal kcal() {
            return food.kcalPerUnit.multiply(units());
        }
    }

    private record MealTemplate(MealType type, List<Portion> portions, String alternative,
                                String convenienceOption) {
    }

    private record DayTemplate(BigDecimal kcal, List<MealTemplate> meals) {

        BigDecimal sumKcal() {
            return meals.stream().flatMap(meal -> meal.portions().stream())
                    .map(Portion::kcal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private static final String BREAKFAST_ALTERNATIVE = "밥 대신 통밀빵, 계란 대신 두부 부침으로 바꿔도 좋아요";
    private static final String BREAKFAST_CONVENIENCE = "편의점: 구운 계란 + 삼각김밥 1개 + 흰 우유";
    private static final String LUNCH_ALTERNATIVE = "닭가슴살 대신 살코기 제육이나 두부조림, 된장국 대신 맑은 국도 좋아요";
    private static final String LUNCH_CONVENIENCE = "편의점: 닭가슴살 도시락 또는 샐러드 + 즉석밥 작은 것";
    private static final String DINNER_ALTERNATIVE = "고등어 대신 연어구이나 수육(살코기), 샐러드 대신 쌈 채소도 좋아요";
    private static final String DINNER_CONVENIENCE = "편의점: 훈제 닭가슴살 + 컵 샐러드 + 즉석 잡곡밥";
    private static final String SNACK_ALTERNATIVE = "과일은 제철 과일로, 요거트는 무가당 두유로 바꿔도 좋아요";
    private static final String SNACK_CONVENIENCE = "편의점: 바나나 + 그릭요거트 또는 하루 견과 1봉";

    /** 1/6 단위 분량: 1/2 = 3, 2/3 = 4, 1 = 6, 1과 1/2 = 9, 2 = 12, 3 = 18. */
    private static final List<DayTemplate> TEMPLATES = List.of(
            day(1200, breakfast(3, 6), lunch(4, 6), dinner(3, 6), snack(p(Food.APPLE, 3))),
            day(1400, breakfast(4, 6), lunch(4, 9), dinner(4, 6), snack(p(Food.APPLE, 6))),
            day(1600, breakfast(4, 12), lunch(6, 6), dinner(4, 6),
                    snack(p(Food.GREEK_YOGURT, 6), p(Food.APPLE, 6))),
            day(1800, breakfast(6, 12), lunch(6, 6), dinner(6, 6),
                    snack(p(Food.GREEK_YOGURT, 6), p(Food.BANANA, 6))),
            day(2000, breakfast(6, 12), lunch(6, 9), dinner(6, 6), snackWithNuts()),
            day(2200, breakfast(6, 12), lunch(6, 12), dinner(6, 9), snackWithNuts()),
            day(2400, breakfast(6, 18), lunch(9, 12), dinner(6, 9), snackWithNuts()),
            day(2600, breakfast(9, 12), lunch(9, 12), dinner(9, 9), snackWithNuts()),
            day(2800, breakfast(9, 18), lunch(9, 12), dinner(9, 9), snackWithMilk()),
            day(3000, breakfast(9, 18), lunch(12, 12), dinner(9, 9), snackWithMilk()),
            day(3200, breakfast(12, 18), lunch(12, 12), dinner(9, 12), snackWithMilk()),
            day(3400, breakfast(12, 18), lunch(12, 12), dinner(12, 12), snackWithMilk()),
            day(3600, breakfast(12, 18), lunch(12, 18), dinner(12, 12),
                    snack(p(Food.GREEK_YOGURT, 6), p(Food.BANANA, 6), p(Food.NUTS, 6),
                            p(Food.LOW_FAT_MILK, 6), p(Food.SWEET_POTATO, 6))));

    public MealPlan plan(BigDecimal targetKcal) {
        DayTemplate template = nearest(targetKcal);
        boolean outOfRange = targetKcal.compareTo(TEMPLATES.getFirst().kcal()) < 0
                || targetKcal.compareTo(TEMPLATES.getLast().kcal()) > 0;
        BigDecimal scale = targetKcal.divide(template.sumKcal(), MathContext.DECIMAL64);

        List<Meal> meals = template.meals().stream().map(meal -> scaleMeal(meal, scale)).toList();
        BigDecimal total = meals.stream().map(Meal::kcal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MealPlan(targetKcal, template.kcal(), scale, outOfRange, meals, total);
    }

    public List<BigDecimal> templateKcals() {
        return TEMPLATES.stream().map(DayTemplate::kcal).toList();
    }

    /** 템플릿을 조정하기 전 원래 합계 kcal. */
    BigDecimal templateSumKcal(BigDecimal templateKcal) {
        return TEMPLATES.stream().filter(template -> template.kcal().compareTo(templateKcal) == 0)
                .findFirst().orElseThrow().sumKcal();
    }

    /** 구간 kcal이 가장 가까운 템플릿. 같은 거리면 낮은 쪽. */
    private static DayTemplate nearest(BigDecimal targetKcal) {
        DayTemplate best = TEMPLATES.getFirst();
        for (DayTemplate template : TEMPLATES) {
            BigDecimal distance = template.kcal().subtract(targetKcal).abs();
            if (distance.compareTo(best.kcal().subtract(targetKcal).abs()) < 0) {
                best = template;
            }
        }
        return best;
    }

    private static Meal scaleMeal(MealTemplate meal, BigDecimal scale) {
        List<MealItem> items = new ArrayList<>();
        for (Portion portion : meal.portions()) {
            BigDecimal units = portion.units().multiply(scale, MathContext.DECIMAL64);
            items.add(new MealItem(
                    portion.food().label,
                    portionLabel(BigDecimal.valueOf(portion.sixths()).multiply(scale), portion.food().unit),
                    portion.food().gramsPerUnit.multiply(units).setScale(0, RoundingMode.HALF_UP),
                    portion.food().kcalPerUnit.multiply(units).setScale(0, RoundingMode.HALF_UP)));
        }
        BigDecimal kcal = items.stream().map(MealItem::kcal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Meal(meal.type(), items, kcal, meal.alternative(), meal.convenienceOption());
    }

    /** 1/6 단위 분량을 1/3, 1/2, 2/3 단위의 가장 가까운 생활 단위로 표시한다 (최소 1/3). */
    static String portionLabel(BigDecimal sixths, String unit) {
        int nearest = 2;
        BigDecimal bestDistance = null;
        int center = sixths.setScale(0, RoundingMode.HALF_UP).intValueExact();
        for (int candidate = Math.max(2, center - 3); candidate <= center + 3; candidate++) {
            int remainder = candidate % SIXTHS;
            if (remainder == 1 || remainder == 5) {
                continue;
            }
            BigDecimal distance = sixths.subtract(BigDecimal.valueOf(candidate)).abs();
            if (bestDistance == null || distance.compareTo(bestDistance) < 0) {
                bestDistance = distance;
                nearest = candidate;
            }
        }
        String prefix = bestDistance.compareTo(APPROX_THRESHOLD_SIXTHS) > 0 ? "약 " : "";
        return prefix + formatSixths(nearest, unit);
    }

    private static String formatSixths(int sixths, String unit) {
        int whole = sixths / SIXTHS;
        int remainder = sixths % SIXTHS;
        if (remainder == 0) {
            return whole + unit;
        }
        if (whole > 0 && remainder == 3) {
            return whole + unit + " 반";
        }
        String fraction = switch (remainder) {
            case 2 -> "1/3";
            case 3 -> "1/2";
            case 4 -> "2/3";
            default -> throw new IllegalArgumentException("지원하지 않는 분량");
        };
        return whole == 0 ? fraction + unit : whole + " " + fraction + unit;
    }

    private static DayTemplate day(int kcal, MealTemplate... meals) {
        return new DayTemplate(BigDecimal.valueOf(kcal), List.of(meals));
    }

    private static Portion p(Food food, int sixths) {
        return new Portion(food, sixths);
    }

    private static MealTemplate breakfast(int riceSixths, int eggSixths) {
        return new MealTemplate(MealType.BREAKFAST, List.of(
                p(Food.MULTIGRAIN_RICE, riceSixths), p(Food.EGG, eggSixths), p(Food.KIMCHI, 6), p(Food.NAMUL, 6)),
                BREAKFAST_ALTERNATIVE, BREAKFAST_CONVENIENCE);
    }

    private static MealTemplate lunch(int riceSixths, int chickenSixths) {
        return new MealTemplate(MealType.LUNCH, List.of(
                p(Food.MULTIGRAIN_RICE, riceSixths), p(Food.CHICKEN_BREAST, chickenSixths),
                p(Food.DOENJANG_SOUP, 6), p(Food.NAMUL, 6), p(Food.KIMCHI, 6)),
                LUNCH_ALTERNATIVE, LUNCH_CONVENIENCE);
    }

    private static MealTemplate dinner(int riceSixths, int mackerelSixths) {
        return new MealTemplate(MealType.DINNER, List.of(
                p(Food.MULTIGRAIN_RICE, riceSixths), p(Food.MACKEREL, mackerelSixths),
                p(Food.SALAD, 6), p(Food.KIMCHI, 6)),
                DINNER_ALTERNATIVE, DINNER_CONVENIENCE);
    }

    private static MealTemplate snack(Portion... portions) {
        return new MealTemplate(MealType.SNACK, List.of(portions), SNACK_ALTERNATIVE, SNACK_CONVENIENCE);
    }

    private static MealTemplate snackWithNuts() {
        return snack(p(Food.GREEK_YOGURT, 6), p(Food.BANANA, 6), p(Food.NUTS, 6));
    }

    private static MealTemplate snackWithMilk() {
        return snack(p(Food.GREEK_YOGURT, 6), p(Food.BANANA, 6), p(Food.NUTS, 6), p(Food.LOW_FAT_MILK, 6));
    }
}
