package io.keede.mombti.core.domains.report.service;

import io.keede.mombti.core.domains.report.advice.AdviceContext;
import io.keede.mombti.core.domains.report.advice.AdviceNarrator;
import io.keede.mombti.core.domains.report.advice.ImprovementRuleEngine;
import io.keede.mombti.core.domains.report.advice.MealPlanCatalog;
import io.keede.mombti.core.domains.report.calculator.BmrCalculator;
import io.keede.mombti.core.domains.report.calculator.BodyCompositionEstimator;
import io.keede.mombti.core.domains.report.calculator.BodyIndexCalculator;
import io.keede.mombti.core.domains.report.calculator.EnergyTargetCalculator;
import io.keede.mombti.core.domains.report.calculator.ExerciseEnergyCalculator;
import io.keede.mombti.core.domains.report.calculator.MacroCalculator;
import io.keede.mombti.core.domains.report.calculator.TdeeCalculator;
import io.keede.mombti.core.domains.report.defaults.ProfileDefaults;
import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.ChartWindow;
import io.keede.mombti.core.domains.report.model.CurrentStatus;
import io.keede.mombti.core.domains.report.model.EnergyTarget;
import io.keede.mombti.core.domains.report.model.Estimated;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.MacroPlan;
import io.keede.mombti.core.domains.report.model.MealPlan;
import io.keede.mombti.core.domains.report.model.Milestone;
import io.keede.mombti.core.domains.report.model.Narration;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.Report;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.model.ReportResult;
import io.keede.mombti.core.domains.report.model.ScenarioEta;
import io.keede.mombti.core.domains.report.model.ScenarioProjections;
import io.keede.mombti.core.domains.report.model.SummaryCards;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import io.keede.mombti.core.domains.report.simulation.ChartWindowPolicy;
import io.keede.mombti.core.domains.report.simulation.GoalEtaCalculator;
import io.keede.mombti.core.domains.report.simulation.MilestoneCalculator;
import io.keede.mombti.core.domains.report.simulation.ScenarioFactory;
import io.keede.mombti.core.domains.report.simulation.SimulationInput;
import io.keede.mombti.core.domains.report.simulation.WeightProjectionSimulator;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * 리포트 조립 (docs/ARCHITECTURE.md 데이터 흐름). 읽기 전용이고, 계산은 각 도메인 컴포넌트에 맡기고
 * 호출 순서와 결과 조립만 담당한다.
 */
public class ReportQueryService {

    private final SafetyPolicy safetyPolicy;
    private final ProfileDefaults profileDefaults;
    private final BodyIndexCalculator bodyIndexCalculator;
    private final BodyCompositionEstimator bodyCompositionEstimator;
    private final BmrCalculator bmrCalculator;
    private final TdeeCalculator tdeeCalculator;
    private final EnergyTargetCalculator energyTargetCalculator;
    private final ScenarioFactory scenarioFactory;
    private final WeightProjectionSimulator simulator;
    private final GoalEtaCalculator goalEtaCalculator;
    private final MilestoneCalculator milestoneCalculator;
    private final ChartWindowPolicy chartWindowPolicy;
    private final MacroCalculator macroCalculator;
    private final MealPlanCatalog mealPlanCatalog;
    private final ImprovementRuleEngine improvementRuleEngine;
    private final AdviceNarrator adviceNarrator;

    public ReportQueryService(SafetyPolicy safetyPolicy,
                              ProfileDefaults profileDefaults,
                              BodyIndexCalculator bodyIndexCalculator,
                              BodyCompositionEstimator bodyCompositionEstimator,
                              BmrCalculator bmrCalculator,
                              TdeeCalculator tdeeCalculator,
                              EnergyTargetCalculator energyTargetCalculator,
                              ScenarioFactory scenarioFactory,
                              WeightProjectionSimulator simulator,
                              GoalEtaCalculator goalEtaCalculator,
                              MilestoneCalculator milestoneCalculator,
                              ChartWindowPolicy chartWindowPolicy,
                              MacroCalculator macroCalculator,
                              MealPlanCatalog mealPlanCatalog,
                              ImprovementRuleEngine improvementRuleEngine,
                              AdviceNarrator adviceNarrator) {
        this.safetyPolicy = safetyPolicy;
        this.profileDefaults = profileDefaults;
        this.bodyIndexCalculator = bodyIndexCalculator;
        this.bodyCompositionEstimator = bodyCompositionEstimator;
        this.bmrCalculator = bmrCalculator;
        this.tdeeCalculator = tdeeCalculator;
        this.energyTargetCalculator = energyTargetCalculator;
        this.scenarioFactory = scenarioFactory;
        this.simulator = simulator;
        this.goalEtaCalculator = goalEtaCalculator;
        this.milestoneCalculator = milestoneCalculator;
        this.chartWindowPolicy = chartWindowPolicy;
        this.macroCalculator = macroCalculator;
        this.mealPlanCatalog = mealPlanCatalog;
        this.improvementRuleEngine = improvementRuleEngine;
        this.adviceNarrator = adviceNarrator;
    }

    public ReportResult generate(BodyProfile body) {
        // 1. 입력값 기반 안전 규칙 (E-04, E-05, E-13)
        Optional<ReportMode> blocked = safetyPolicy.evaluate(body);
        if (blocked.isPresent()) {
            return new ReportResult.MinorBlocked();
        }

        // 2. 선택 항목 기본값
        ResolvedProfile profile = profileDefaults.resolve(body);

        // 3. 지표
        BigDecimal bmi = bodyIndexCalculator.bmi(profile.heightCm(), profile.weightKg());
        Estimated<BigDecimal> bmr = bmrCalculator.bmr(profile);
        Estimated<BigDecimal> tdee = new Estimated<>(tdeeCalculator.tdee(bmr.value(), profile.pal()),
                profile.activityLevel().estimated(), profile.activityLevel().evidence());
        CurrentStatus currentStatus = currentStatus(profile, bmi, bmr, tdee);

        // 4. Goal 해석과 리포트 모드 (E-11, E-12, E-14, E-15)
        ReportMode mode = safetyPolicy.decideMode(profile);
        BigDecimal goalKg = profile.goal().value().weightKg();

        // 5. 하루 섭취 목표 (E-16, E-17)
        EnergyTarget energyTarget = energyTargetCalculator.calculate(profile, tdee.value(), mode);

        // 6. 시나리오 4개 × (기본, BMR × 0.9, BMR × 1.1, 현실 보정 80%)
        SimulationInput input = simulator.input(profile, energyTarget, mode);
        List<ScenarioProjections> projections = scenarioFactory.create().stream()
                .map(scenario -> simulator.simulate(input, scenario))
                .toList();

        // 7. 도달 예측, 단계별 목표, 그래프 기간
        List<ScenarioEta> etas = projections.stream()
                .map(projection -> scenarioEta(input, projection, profile.weightKg(), goalKg))
                .toList();
        ScenarioEta dietOnlyEta = etas.getFirst();
        Projection dietOnly = projections.getFirst().base();
        List<Milestone> milestones = milestoneCalculator.milestones(input, dietOnly, goalKg);
        ChartWindow chartWindow = chartWindowPolicy.window(dietOnlyEta.eta().expected());
        List<ScenarioProjections> chartProjections = projections.stream()
                .map(chartWindowPolicy::chartProjections)
                .toList();

        // 8. 식단과 개선할 부분
        BigDecimal macroBasisKg = mode == ReportMode.LOSS ? goalKg : profile.weightKg();
        MacroPlan macroPlan = macroCalculator.calculate(energyTarget.intakeKcal(), profile.weightKg(),
                macroBasisKg, bmi, profile.age());
        MealPlan mealPlan = mealPlanCatalog.plan(energyTarget.intakeKcal());
        AdviceContext adviceContext = new AdviceContext(profile, mode, bmi, currentStatus.bmiCategory(),
                currentStatus.bodyFatLargeError(), energyTarget, macroPlan);
        List<Improvement> improvements = improvementRuleEngine.evaluate(adviceContext);

        // 9. 문장
        Narration narration = adviceNarrator.narrate(adviceContext);

        SummaryCards summary = new SummaryCards(dietOnlyEta.eta(), energyTarget.intakeKcal(), tdee,
                energyTarget.deficitKcal(),
                dietOnly.weightAt(ChartWindow.DEFAULT_WEEKS), dietOnly.changeKg(ChartWindow.DEFAULT_WEEKS),
                dietOnlyEta.weeklyLossKg(), dietOnlyEta.withinSafeRange());

        return new Report(mode, profile, milestoneCalculator.dateAfterWeeks(0), summary,
                projections, chartProjections, chartWindow, etas, milestones, currentStatus,
                energyTarget, macroPlan, mealPlan, narration, improvements,
                evidence(profile, currentStatus, energyTarget, macroPlan, milestones, improvements, narration));
    }

    private CurrentStatus currentStatus(ResolvedProfile profile, BigDecimal bmi,
                                        Estimated<BigDecimal> bmr, Estimated<BigDecimal> tdee) {
        Estimated<BigDecimal> bodyFat = profile.bodyFatPercent();
        Estimated<BigDecimal> fatMass = new Estimated<>(
                bodyCompositionEstimator.fatMassKg(profile.weightKg(), bodyFat.value()),
                bodyFat.estimated(), bodyFat.evidence());
        boolean bodyFatMismatch = !bodyFat.estimated() && bodyCompositionEstimator.differsFromEstimate(
                bodyCompositionEstimator.estimateBodyFatPercent(profile.gender(), profile.age(), bmi),
                bodyFat.value());
        return new CurrentStatus(bmi, bodyIndexCalculator.category(bmi), bodyFat, fatMass,
                bodyCompositionEstimator.isLargeErrorExpected(bmi), bodyFatMismatch, bmr, tdee,
                tdeeCalculator.lowerBound(bmr.value(), profile.pal()),
                tdeeCalculator.upperBound(bmr.value(), profile.palBandUpper()));
    }

    private ScenarioEta scenarioEta(SimulationInput input, ScenarioProjections projections,
                                    BigDecimal weightKg, BigDecimal goalKg) {
        BigDecimal weeklyLoss = projections.base().averageWeeklyLossKg(SafetyPolicy.SAFE_RANGE_WEEKS);
        return new ScenarioEta(projections.scenario(),
                goalEtaCalculator.estimate(input, projections, goalKg),
                weeklyLoss,
                safetyPolicy.isWithinSafeWeeklyLoss(weightKg, weeklyLoss));
    }

    /** 리포트에 쓰인 출처만, {@link Evidence} 선언 순서로 모은다. */
    private static List<Evidence> evidence(ResolvedProfile profile, CurrentStatus status, EnergyTarget energyTarget,
                                           MacroPlan macroPlan, List<Milestone> milestones,
                                           List<Improvement> improvements, Narration narration) {
        EnumSet<Evidence> used = EnumSet.noneOf(Evidence.class);
        used.addAll(BodyIndexCalculator.EVIDENCE);
        used.addAll(profile.goal().evidence());
        used.addAll(status.bodyFatPercent().evidence());
        used.addAll(status.bmrKcal().evidence());
        used.addAll(status.tdeeKcal().evidence());
        used.addAll(energyTarget.evidence());
        used.addAll(WeightProjectionSimulator.EVIDENCE);
        used.addAll(ExerciseEnergyCalculator.EVIDENCE);
        milestones.forEach(milestone -> used.addAll(milestone.type().evidence()));
        used.addAll(macroPlan.evidence());
        improvements.forEach(improvement -> used.addAll(improvement.evidence()));
        narration.faqs().forEach(faq -> used.addAll(faq.evidence()));
        return List.copyOf(used);
    }
}
