package io.keede.mombti.core.domains.report.fixture;

import io.keede.mombti.core.domains.report.advice.ImprovementRuleEngine;
import io.keede.mombti.core.domains.report.advice.MealPlanCatalog;
import io.keede.mombti.core.domains.report.advice.TemplateAdviceNarrator;
import io.keede.mombti.core.domains.report.calculator.BmrCalculator;
import io.keede.mombti.core.domains.report.calculator.BodyCompositionEstimator;
import io.keede.mombti.core.domains.report.calculator.BodyIndexCalculator;
import io.keede.mombti.core.domains.report.calculator.EnergyTargetCalculator;
import io.keede.mombti.core.domains.report.calculator.ExerciseEnergyCalculator;
import io.keede.mombti.core.domains.report.calculator.MacroCalculator;
import io.keede.mombti.core.domains.report.calculator.TdeeCalculator;
import io.keede.mombti.core.domains.report.defaults.ProfileDefaults;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import io.keede.mombti.core.domains.report.service.ReportQueryService;
import io.keede.mombti.core.domains.report.simulation.ChartWindowPolicy;
import io.keede.mombti.core.domains.report.simulation.GoalEtaCalculator;
import io.keede.mombti.core.domains.report.simulation.MilestoneCalculator;
import io.keede.mombti.core.domains.report.simulation.ScenarioFactory;
import io.keede.mombti.core.domains.report.simulation.WeightProjectionSimulator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/** 리포트 테스트용 조립 (고정 Clock)과 docs/PRD.md 페르소나 3개. */
public final class ReportFixture {

    /** 2026-10-26 09:00 서울. */
    public static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    private ReportFixture() {
    }

    public static ReportQueryService service() {
        BodyIndexCalculator bodyIndexCalculator = new BodyIndexCalculator();
        BodyCompositionEstimator bodyCompositionEstimator = new BodyCompositionEstimator();
        SafetyPolicy safetyPolicy = new SafetyPolicy(bodyIndexCalculator);
        BmrCalculator bmrCalculator = new BmrCalculator(bodyCompositionEstimator);
        TdeeCalculator tdeeCalculator = new TdeeCalculator();
        EnergyTargetCalculator energyTargetCalculator =
                new EnergyTargetCalculator(safetyPolicy, bodyCompositionEstimator);
        WeightProjectionSimulator simulator = new WeightProjectionSimulator(bmrCalculator, tdeeCalculator,
                new ExerciseEnergyCalculator(), energyTargetCalculator, bodyCompositionEstimator);
        GoalEtaCalculator goalEtaCalculator = new GoalEtaCalculator(simulator);
        return new ReportQueryService(
                safetyPolicy,
                new ProfileDefaults(bodyIndexCalculator, bodyCompositionEstimator),
                bodyIndexCalculator,
                bodyCompositionEstimator,
                bmrCalculator,
                tdeeCalculator,
                energyTargetCalculator,
                new ScenarioFactory(),
                simulator,
                goalEtaCalculator,
                new MilestoneCalculator(goalEtaCalculator, FIXED_CLOCK),
                new ChartWindowPolicy(),
                new MacroCalculator(safetyPolicy),
                new MealPlanCatalog(),
                ImprovementRuleEngine.withDefaultRules(safetyPolicy),
                new TemplateAdviceNarrator());
    }

    public static BodyProfile profile(Gender gender, int age, String heightCm, String weightKg,
                                      ActivityLevel activity, String targetKg, String bodyFatPercent) {
        return new BodyProfile(gender, age, new BigDecimal(heightCm), new BigDecimal(weightKg),
                Optional.ofNullable(activity),
                Optional.ofNullable(targetKg).map(BigDecimal::new),
                Optional.ofNullable(bodyFatPercent).map(BigDecimal::new));
    }

    /** ① 처음 다이어트하는 20~30대: 필수 4개만. 여 27세 163cm 68kg. */
    public static BodyProfile firstTimer() {
        return profile(Gender.FEMALE, 27, "163.0", "68.0", null, null, null);
    }

    /** ② 정체기에 빠진 운동인: 필수 + 체지방률. 남 32세 176cm 80kg, 체지방 18%. */
    public static BodyProfile plateauedAthlete() {
        return profile(Gender.MALE, 32, "176.0", "80.0", null, null, "18.0");
    }

    /** ③ 바쁜 직장인: 필수 + 활동량 + 목표 체중. 남 41세 172cm 85kg, 사무직(2단계), 목표 75kg. */
    public static BodyProfile busyWorker() {
        return profile(Gender.MALE, 41, "172.0", "85.0", ActivityLevel.LIGHT, "75.0", null);
    }
}
