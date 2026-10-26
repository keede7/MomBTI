package io.keede.mombti.core.domains.report.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.keede.mombti.core.domains.report.model.Meal;
import io.keede.mombti.core.domains.report.model.MealItem;
import io.keede.mombti.core.domains.report.model.MealPlan;
import io.keede.mombti.core.domains.report.model.MealType;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MealPlanCatalogTest {

    private final MealPlanCatalog catalog = new MealPlanCatalog();

    @Test
    void 템플릿은_1200부터_3600까지_200kcal_간격() {
        assertThat(catalog.templateKcals()).containsExactlyElementsOf(
                IntStream.rangeClosed(0, 12).mapToObj(i -> new BigDecimal(1200 + i * 200)).toList());
    }

    @Test
    void 각_템플릿의_원래_합계는_구간_kcal과_5퍼센트_이내() {
        for (BigDecimal kcal : catalog.templateKcals()) {
            BigDecimal sum = catalog.templateSumKcal(kcal);
            // 허용 오차: 구간 kcal의 ±5%
            assertThat(ratio(sum, kcal)).as("%s kcal 템플릿", kcal).isCloseTo(1.0, within(0.05));
        }
    }

    @Test
    void 끼니는_아침_점심_저녁_간식이고_대체_메뉴와_편의점_옵션이_있음() {
        MealPlan plan = catalog.plan(new BigDecimal("1800"));

        assertThat(plan.meals()).extracting(Meal::type)
                .containsExactly(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK);
        for (Meal meal : plan.meals()) {
            assertThat(meal.items()).isNotEmpty();
            assertThat(meal.alternative()).isNotBlank();
            assertThat(meal.convenienceOption()).isNotBlank();
            for (MealItem item : meal.items()) {
                assertThat(item.name()).isNotBlank();
                assertThat(item.portion()).isNotBlank();
                assertThat(item.grams().signum()).isPositive();
                assertThat(item.kcal().signum()).isPositive();
            }
        }
    }

    @Test
    void 생활_단위_분량으로_표시() {
        // 1,200kcal 템플릿 아침 밥은 1/2공기 (비율 조정 후에도 가장 가까운 분수로 표시)
        MealPlan plan = catalog.plan(new BigDecimal("1200"));

        MealItem rice = plan.meals().getFirst().items().getFirst();
        assertThat(rice.name()).isEqualTo("잡곡밥");
        assertThat(rice.portion()).isEqualTo("1/2공기");
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 1200, 1270, 1300, 1500, 1890, 2230, 2750, 3110, 3600, 3700, 4200})
    void 식단_합계_kcal이_목표_대비_5퍼센트_이내(int target) {
        MealPlan plan = catalog.plan(new BigDecimal(target));

        BigDecimal sum = plan.meals().stream().map(Meal::kcal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(plan.totalKcal()).isEqualByComparingTo(sum);
        // 허용 오차: 목표 kcal의 ±5%
        assertThat(ratio(plan.totalKcal(), new BigDecimal(target))).isCloseTo(1.0, within(0.05));
    }

    @Test
    void 끼니_kcal은_항목_kcal의_합() {
        MealPlan plan = catalog.plan(new BigDecimal("2000"));

        for (Meal meal : plan.meals()) {
            BigDecimal sum = meal.items().stream().map(MealItem::kcal).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(meal.kcal()).isEqualByComparingTo(sum);
        }
    }

    @Test
    void 범위_안의_목표는_가장_가까운_템플릿을_사용() {
        assertThat(catalog.plan(new BigDecimal("1270")).templateKcal()).isEqualByComparingTo("1200");
        assertThat(catalog.plan(new BigDecimal("1310")).templateKcal()).isEqualByComparingTo("1400");
        assertThat(catalog.plan(new BigDecimal("1270")).outOfTemplateRange()).isFalse();
    }

    @Test
    void E22_목표_kcal이_템플릿_범위_밖이면_가까운_템플릿을_비율로_조정() {
        MealPlan base = catalog.plan(new BigDecimal("3600"));
        MealPlan high = catalog.plan(new BigDecimal("4200"));
        MealPlan low = catalog.plan(new BigDecimal("1000"));

        assertThat(high.outOfTemplateRange()).isTrue();
        assertThat(high.templateKcal()).isEqualByComparingTo("3600");
        assertThat(low.outOfTemplateRange()).isTrue();
        assertThat(low.templateKcal()).isEqualByComparingTo("1200");

        // 같은 템플릿을 비율(4200 / 템플릿 합계)로 늘렸으므로 분량(g)이 커진다
        BigDecimal baseRice = base.meals().getFirst().items().getFirst().grams();
        BigDecimal highRice = high.meals().getFirst().items().getFirst().grams();
        assertThat(highRice).isGreaterThan(baseRice);
        // 같은 템플릿이므로 g 비율 = 조정 비율의 비율. 허용 오차: g 반올림 ±1g
        assertThat(highRice.doubleValue()).isCloseTo(
                baseRice.doubleValue() * high.scale().doubleValue() / base.scale().doubleValue(), within(1.0));
        assertThat(ratio(high.totalKcal(), new BigDecimal("4200"))).isCloseTo(1.0, within(0.05));
        assertThat(ratio(low.totalKcal(), new BigDecimal("1000"))).isCloseTo(1.0, within(0.05));
    }

    private static double ratio(BigDecimal value, BigDecimal reference) {
        return value.divide(reference, MathContext.DECIMAL64).doubleValue();
    }
}
