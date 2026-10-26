package io.keede.mombti.core.config;

import io.keede.mombti.core.domains.report.advice.AdviceNarrator;
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
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import io.keede.mombti.core.domains.report.service.ReportQueryService;
import io.keede.mombti.core.domains.report.simulation.ChartWindowPolicy;
import io.keede.mombti.core.domains.report.simulation.GoalEtaCalculator;
import io.keede.mombti.core.domains.report.simulation.MilestoneCalculator;
import io.keede.mombti.core.domains.report.simulation.ScenarioFactory;
import io.keede.mombti.core.domains.report.simulation.WeightProjectionSimulator;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 순수 Java 도메인 클래스를 Bean으로 등록한다 (ADR-015). */
@Configuration
public class DomainBeanConfig {

    @Bean
    public BodyIndexCalculator bodyIndexCalculator() {
        return new BodyIndexCalculator();
    }

    @Bean
    public BodyCompositionEstimator bodyCompositionEstimator() {
        return new BodyCompositionEstimator();
    }

    @Bean
    public SafetyPolicy safetyPolicy(BodyIndexCalculator bodyIndexCalculator) {
        return new SafetyPolicy(bodyIndexCalculator);
    }

    @Bean
    public ProfileDefaults profileDefaults(BodyIndexCalculator bodyIndexCalculator,
                                           BodyCompositionEstimator bodyCompositionEstimator) {
        return new ProfileDefaults(bodyIndexCalculator, bodyCompositionEstimator);
    }

    @Bean
    public BmrCalculator bmrCalculator(BodyCompositionEstimator bodyCompositionEstimator) {
        return new BmrCalculator(bodyCompositionEstimator);
    }

    @Bean
    public TdeeCalculator tdeeCalculator() {
        return new TdeeCalculator();
    }

    @Bean
    public ExerciseEnergyCalculator exerciseEnergyCalculator() {
        return new ExerciseEnergyCalculator();
    }

    @Bean
    public EnergyTargetCalculator energyTargetCalculator(SafetyPolicy safetyPolicy,
                                                         BodyCompositionEstimator bodyCompositionEstimator) {
        return new EnergyTargetCalculator(safetyPolicy, bodyCompositionEstimator);
    }

    @Bean
    public MacroCalculator macroCalculator(SafetyPolicy safetyPolicy) {
        return new MacroCalculator(safetyPolicy);
    }

    @Bean
    public ScenarioFactory scenarioFactory() {
        return new ScenarioFactory();
    }

    @Bean
    public WeightProjectionSimulator weightProjectionSimulator(BmrCalculator bmrCalculator,
                                                               TdeeCalculator tdeeCalculator,
                                                               ExerciseEnergyCalculator exerciseEnergyCalculator,
                                                               EnergyTargetCalculator energyTargetCalculator,
                                                               BodyCompositionEstimator bodyCompositionEstimator) {
        return new WeightProjectionSimulator(bmrCalculator, tdeeCalculator, exerciseEnergyCalculator,
                energyTargetCalculator, bodyCompositionEstimator);
    }

    @Bean
    public GoalEtaCalculator goalEtaCalculator(WeightProjectionSimulator simulator) {
        return new GoalEtaCalculator(simulator);
    }

    @Bean
    public MilestoneCalculator milestoneCalculator(GoalEtaCalculator goalEtaCalculator, Clock clock) {
        return new MilestoneCalculator(goalEtaCalculator, clock);
    }

    @Bean
    public ChartWindowPolicy chartWindowPolicy() {
        return new ChartWindowPolicy();
    }

    @Bean
    public MealPlanCatalog mealPlanCatalog() {
        return new MealPlanCatalog();
    }

    @Bean
    public ImprovementRuleEngine improvementRuleEngine(SafetyPolicy safetyPolicy) {
        return ImprovementRuleEngine.withDefaultRules(safetyPolicy);
    }

    @Bean
    public AdviceNarrator adviceNarrator() {
        return new TemplateAdviceNarrator();
    }

    @Bean
    public ReportQueryService reportQueryService(SafetyPolicy safetyPolicy,
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
        return new ReportQueryService(safetyPolicy, profileDefaults, bodyIndexCalculator, bodyCompositionEstimator,
                bmrCalculator, tdeeCalculator, energyTargetCalculator, scenarioFactory, simulator,
                goalEtaCalculator, milestoneCalculator, chartWindowPolicy, macroCalculator, mealPlanCatalog,
                improvementRuleEngine, adviceNarrator);
    }
}
