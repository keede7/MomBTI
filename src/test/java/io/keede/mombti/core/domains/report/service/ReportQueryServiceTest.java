package io.keede.mombti.core.domains.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.fixture.ReportFixture;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.ChartWindow;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.GoalEta;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.Report;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.model.ReportResult;
import io.keede.mombti.core.domains.report.model.Scenario;
import io.keede.mombti.core.domains.report.model.ScenarioEta;
import io.keede.mombti.core.domains.report.model.ScenarioProjections;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReportQueryServiceTest {

    /** 페르소나 수기 계산 허용 오차 ±1% (docs/PRD.md 성공 기준). */
    private static final double ONE_PERCENT = 0.01;

    private final ReportQueryService service = ReportFixture.service();

    private Report report(ReportResult result) {
        assertThat(result).isInstanceOf(Report.class);
        return (Report) result;
    }

    private static Offset<Double> onePercentOf(double expected) {
        return within(expected * ONE_PERCENT);
    }

    /** 리포트 11개 섹션에 필요한 값이 모두 채워졌는지 (null·빈 값 금지, E-01). */
    private static void assertAllSectionsFilled(Report report) {
        assertThat(report).hasNoNullFieldsOrProperties();
        assertThat(report.summary()).hasNoNullFieldsOrProperties();
        assertThat(report.currentStatus()).hasNoNullFieldsOrProperties();
        assertThat(report.projections()).extracting(ScenarioProjections::scenario)
                .containsExactly(Scenario.DIET_ONLY, Scenario.EX3, Scenario.EX4, Scenario.EX5);
        assertThat(report.projections()).allSatisfy(p -> assertThat(p.base().points()).hasSize(53));
        assertThat(report.chartProjections()).hasSize(4)
                .allSatisfy(p -> assertThat(p.base().points()).hasSize(14));
        assertThat(report.etas()).extracting(ScenarioEta::scenario)
                .containsExactly(Scenario.DIET_ONLY, Scenario.EX3, Scenario.EX4, Scenario.EX5);
        assertThat(report.macroPlan().totalKcal()).isEqualByComparingTo(report.energyTarget().intakeKcal());
        assertThat(report.mealPlan().meals()).isNotEmpty();
        assertThat(report.narration().headline()).isNotBlank();
        assertThat(report.narration().notices()).isNotEmpty();
        assertThat(report.narration().faqs()).isNotEmpty();
        assertThat(report.improvements()).isNotEmpty();
        assertThat(report.evidence()).isNotEmpty();
        assertThat(report.startDate()).isEqualTo(ReportFixture.TODAY);
    }

    @Test
    void E01_필수_4개만_입력해도_모든_섹션이_채워짐() {
        Report report = report(service.generate(ReportFixture.firstTimer()));

        assertAllSectionsFilled(report);
        assertThat(report.mode()).isEqualTo(ReportMode.LOSS);
        assertThat(report.milestones()).isNotEmpty();
        assertThat(report.providedOptionalCount()).isZero();
        // 기본값으로 계산한 항목은 모두 추정
        assertThat(report.profile().activityLevel().estimated()).isTrue();
        assertThat(report.profile().goal().estimated()).isTrue();
        assertThat(report.profile().bodyFatPercent().estimated()).isTrue();
        assertThat(report.currentStatus().bodyFatPercent().estimated()).isTrue();
        assertThat(report.currentStatus().fatMassKg().estimated()).isTrue();
        assertThat(report.currentStatus().tdeeKcal().estimated()).isTrue();
        assertThat(report.summary().tdeeKcal().estimated()).isTrue();
    }

    @Test
    void E01_근거_목록은_리포트에_쓰인_출처만_모음() {
        Report report = report(service.generate(ReportFixture.firstTimer()));

        List<Evidence> used = new ArrayList<>();
        used.addAll(report.currentStatus().bmrKcal().evidence());
        used.addAll(report.currentStatus().tdeeKcal().evidence());
        used.addAll(report.currentStatus().bodyFatPercent().evidence());
        used.addAll(report.energyTarget().evidence());
        used.addAll(report.macroPlan().evidence());
        report.improvements().forEach(i -> used.addAll(i.evidence()));
        report.narration().faqs().forEach(f -> used.addAll(f.evidence()));

        assertThat(report.evidence()).containsAll(used)
                .contains(Evidence.MIFFLIN_1990, Evidence.DEURENBERG_1991, Evidence.HALL_2011,
                        Evidence.ADAPTIVE_THERMOGENESIS, Evidence.ADHERENCE_80, Evidence.FRANKENFIELD_2005,
                        Evidence.KSSO_2022, Evidence.WING_2011)
                // 체지방률을 입력하지 않았고 65세 미만이므로 쓰지 않은 출처
                .doesNotContain(Evidence.CUNNINGHAM_1991, Evidence.PROT_AGE_2013, Evidence.ESSENTIAL_FAT)
                .doesNotHaveDuplicates()
                .isSorted();
    }

    static Stream<Arguments> optionalCombinations() {
        List<Arguments> combinations = new ArrayList<>();
        for (boolean activity : new boolean[]{false, true}) {
            for (boolean target : new boolean[]{false, true}) {
                for (boolean bodyFat : new boolean[]{false, true}) {
                    combinations.add(Arguments.of(activity, target, bodyFat));
                }
            }
        }
        return combinations.stream();
    }

    @ParameterizedTest(name = "활동량={0}, 목표={1}, 체지방률={2}")
    @MethodSource("optionalCombinations")
    void E02_선택_항목_조합_8가지(boolean activity, boolean target, boolean bodyFat) {
        Report report = report(service.generate(ReportFixture.profile(Gender.MALE, 35, "175.0", "85.0",
                activity ? ActivityLevel.MODERATE : null,
                target ? "75.0" : null,
                bodyFat ? "25.0" : null)));

        assertAllSectionsFilled(report);
        assertThat(report.mode()).isEqualTo(ReportMode.LOSS);
        assertThat(report.profile().activityLevel().estimated()).isEqualTo(!activity);
        assertThat(report.profile().goal().estimated()).isEqualTo(!target);
        assertThat(report.profile().bodyFatPercent().estimated()).isEqualTo(!bodyFat);
        assertThat(report.currentStatus().tdeeKcal().estimated()).isEqualTo(!activity);
        assertThat(report.currentStatus().bodyFatPercent().estimated()).isEqualTo(!bodyFat);
        assertThat(report.providedOptionalCount())
                .isEqualTo((activity ? 1 : 0) + (target ? 1 : 0) + (bodyFat ? 1 : 0));
        // 입력한 항목만 반영
        assertThat(report.profile().activityLevel().value())
                .isEqualTo(activity ? ActivityLevel.MODERATE : ActivityLevel.SEDENTARY);
        assertThat(report.profile().goal().value().weightKg())
                .isEqualByComparingTo(target ? "75.0" : "67.4");
        assertThat(report.evidence().contains(Evidence.CUNNINGHAM_1991)).isEqualTo(bodyFat);
        assertThat(report.evidence().contains(Evidence.MIFFLIN_1990)).isEqualTo(!bodyFat);
    }

    @Test
    void 페르소나1_처음_다이어트_여_27세_필수만_수기_계산과_1퍼센트_이내() {
        // BMI 68 ÷ 1.63² = 25.59 → 기본 목표 BMI 22.0 = 58.5kg
        // Mifflin 10×68 + 6.25×163 − 5×27 − 161 = 1402.75, 소비량 × 1.40 = 1963.85
        // Deurenberg 1.2×25.59 + 0.23×27 − 5.4 = 31.5% → 지방량 21.42kg, ρ ≈ 6948, 1% 상한 ≈ 675
        // 적자 = min(1963.85 × 25% = 490.96, 675) → 섭취 1472.9 → 1,470kcal
        // 단백질 1.6 × 68 = 108.8 → 109g
        Report report = report(service.generate(ReportFixture.firstTimer()));

        assertThat(report.currentStatus().bmi().doubleValue()).isCloseTo(25.59, onePercentOf(25.59));
        assertThat(report.currentStatus().bodyFatPercent().value().doubleValue()).isCloseTo(31.5, onePercentOf(31.5));
        assertThat(report.profile().goal().value().weightKg().doubleValue()).isCloseTo(58.5, onePercentOf(58.5));
        assertThat(report.currentStatus().bmrKcal().value().doubleValue()).isCloseTo(1402.75, onePercentOf(1402.75));
        assertThat(report.currentStatus().tdeeKcal().value().doubleValue()).isCloseTo(1963.85, onePercentOf(1963.85));
        assertThat(report.energyTarget().intakeKcal().doubleValue()).isCloseTo(1470, onePercentOf(1470));
        assertThat(report.macroPlan().proteinG().doubleValue()).isCloseTo(109, onePercentOf(109));
    }

    @Test
    void 페르소나2_정체기_운동인_남_32세_체지방률_입력_수기_계산과_1퍼센트_이내() {
        // 제지방량 80 × 0.82 = 65.6kg → Cunningham 370 + 21.6 × 65.6 = 1786.96, 소비량 × 1.40 = 2501.74
        // 지방량 14.4kg, ρ ≈ 6243, 1% 상한 ≈ 713.5. 적자 = min(625.4, 713.5) → 섭취 1876.3 → 1,880kcal
        // 기본 목표 BMI 22.0 × 1.76² = 68.1kg, 단백질 1.6 × 80 = 128g
        Report report = report(service.generate(ReportFixture.plateauedAthlete()));

        assertThat(report.currentStatus().bmrKcal().value().doubleValue()).isCloseTo(1786.96, onePercentOf(1786.96));
        assertThat(report.currentStatus().bmrKcal().evidence()).contains(Evidence.CUNNINGHAM_1991);
        assertThat(report.currentStatus().tdeeKcal().value().doubleValue()).isCloseTo(2501.74, onePercentOf(2501.74));
        assertThat(report.currentStatus().fatMassKg().value().doubleValue()).isCloseTo(14.4, onePercentOf(14.4));
        assertThat(report.currentStatus().bodyFatPercent().estimated()).isFalse();
        assertThat(report.profile().goal().value().weightKg().doubleValue()).isCloseTo(68.1, onePercentOf(68.1));
        assertThat(report.energyTarget().intakeKcal().doubleValue()).isCloseTo(1880, onePercentOf(1880));
        assertThat(report.macroPlan().proteinG().doubleValue()).isCloseTo(128, onePercentOf(128));
    }

    @Test
    void 페르소나3_바쁜_직장인_남_41세_활동량_목표_입력_수기_계산과_1퍼센트_이내() {
        // Mifflin 10×85 + 6.25×172 − 5×41 + 5 = 1725, 소비량 × 1.55 = 2673.75
        // Deurenberg 1.2×28.73 + 0.23×41 − 10.8 − 5.4 = 27.7% → 지방량 23.55kg, ρ ≈ 7104, 1% 상한 ≈ 862.6
        // 적자 = min(668.4, 862.6) → 섭취 2005.3 → 2,010kcal, 단백질 1.6 × 85 = 136g
        Report report = report(service.generate(ReportFixture.busyWorker()));

        assertThat(report.currentStatus().bmrKcal().value().doubleValue()).isCloseTo(1725, onePercentOf(1725));
        assertThat(report.currentStatus().tdeeKcal().value().doubleValue()).isCloseTo(2673.75, onePercentOf(2673.75));
        assertThat(report.currentStatus().tdeeKcal().estimated()).isFalse();
        assertThat(report.energyTarget().intakeKcal().doubleValue()).isCloseTo(2010, onePercentOf(2010));
        assertThat(report.macroPlan().proteinG().doubleValue()).isCloseTo(136, onePercentOf(136));
        assertThat(report.profile().goal().estimated()).isFalse();
        // 운동을 더하면 목표에 더 빨리 (또는 같게) 도달
        assertThat(report.summary().dietOnlyEta()).isEqualTo(report.etas().getFirst().eta());
        int dietOnly = reachedWeek(report.etas().getFirst().eta().expected());
        int ex5 = reachedWeek(report.etas().getLast().eta().expected());
        assertThat(ex5).isLessThanOrEqualTo(dietOnly);
    }

    private static int reachedWeek(GoalEta eta) {
        return eta instanceof GoalEta.Reached reached ? reached.week() : Integer.MAX_VALUE;
    }

    @Test
    void 요약_카드는_식단만_기본_예측에서_나옴() {
        Report report = report(service.generate(ReportFixture.busyWorker()));
        Projection dietOnly = report.projections().getFirst().base();

        assertThat(report.summary().intakeKcal()).isEqualByComparingTo(report.energyTarget().intakeKcal());
        assertThat(report.summary().deficitKcal()).isEqualByComparingTo(report.energyTarget().deficitKcal());
        assertThat(report.summary().weightAt12WeeksKg()).isEqualByComparingTo(dietOnly.weightAt(12));
        assertThat(report.summary().changeAt12WeeksKg()).isEqualByComparingTo(dietOnly.changeKg(12));
        assertThat(report.summary().changeAt12WeeksKg()).isNegative();
        assertThat(report.summary().weeklyLossKg()).isEqualByComparingTo(dietOnly.averageWeeklyLossKg(4));
        // 식단만 적자는 1% 상한 안이므로 안전 범위 (HELMS_2014)
        assertThat(report.summary().weeklyLossWithinSafeRange()).isTrue();
        assertThat(report.etas().getFirst().weeklyLossKg()).isEqualByComparingTo(report.summary().weeklyLossKg());
    }

    @Test
    void 하루_소비량_범위는_기본값을_감싼다() {
        Report report = report(service.generate(ReportFixture.firstTimer()));

        assertThat(report.currentStatus().tdeeLowerKcal()).isLessThan(report.currentStatus().tdeeKcal().value());
        assertThat(report.currentStatus().tdeeUpperKcal()).isGreaterThan(report.currentStatus().tdeeKcal().value());
    }

    @Test
    void 날짜는_고정_Clock_기준_N주_뒤() {
        Report report = report(service.generate(ReportFixture.firstTimer()));

        assertThat(report.dateAfterWeeks(0)).isEqualTo(ReportFixture.TODAY);
        assertThat(report.dateAfterWeeks(12)).isEqualTo(ReportFixture.TODAY.plusWeeks(12));
    }

    @Test
    void E15_목표_없음_정상_BMI면_유지_모드에서도_모든_섹션이_채워짐() {
        Report report = report(service.generate(
                ReportFixture.profile(Gender.FEMALE, 30, "165.0", "55.0", null, null, null)));

        assertAllSectionsFilled(report);
        assertThat(report.mode()).isEqualTo(ReportMode.MAINTAIN);
        assertThat(report.chartWindow()).isInstanceOf(ChartWindow.NoGoal.class);
        assertThat(report.etas()).allSatisfy(eta ->
                assertThat(eta.eta().expected()).isInstanceOf(GoalEta.NotApplicable.class));
        assertThat(report.summary().weeklyLossKg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.narration().positiveEmptyState()).isTrue();
    }

    @Test
    void E11_저체중이면_감량하지_않고_유지_모드() {
        Report report = report(service.generate(
                ReportFixture.profile(Gender.FEMALE, 25, "165.0", "48.0", null, null, null)));

        assertAllSectionsFilled(report);
        assertThat(report.mode()).isEqualTo(ReportMode.UNDERWEIGHT_MAINTAIN);
        assertThat(report.energyTarget().dietOnlyMaintains()).isTrue();
    }

    @Test
    void E04_미성년자는_MINOR_BLOCKED이고_감량_수치가_없음() {
        ReportResult result = service.generate(
                ReportFixture.profile(Gender.MALE, 17, "175.0", "85.0", null, "70.0", null));

        assertThat(result).isInstanceOf(ReportResult.MinorBlocked.class);
        assertThat(result.mode()).isEqualTo(ReportMode.MINOR_BLOCKED);
        assertThat(result).isNotInstanceOf(Report.class);
    }

    @Test
    void E24_입력_체지방률이_BMI_추정값과_15퍼센트포인트_이상_차이나면_경고만_하고_입력값_사용() {
        // 남 32세 176cm 80kg → Deurenberg 추정 22.2%, 입력 40.0% → 차이 17.8%p
        Report report = report(service.generate(ReportFixture.profile(Gender.MALE, 32, "176.0", "80.0",
                null, null, "40.0")));

        assertThat(report.currentStatus().bodyFatMismatch()).isTrue();
        assertThat(report.currentStatus().bodyFatPercent().value()).isEqualByComparingTo("40.0");
        assertThat(report.currentStatus().bodyFatPercent().estimated()).isFalse();
    }

    @Test
    void E24_체지방률이_추정값과_가깝거나_미입력이면_경고_없음() {
        assertThat(report(service.generate(ReportFixture.plateauedAthlete())).currentStatus().bodyFatMismatch())
                .isFalse();
        assertThat(report(service.generate(ReportFixture.firstTimer())).currentStatus().bodyFatMismatch())
                .isFalse();
    }
}
