package io.keede.mombti.core.domains.report.simulation;

import io.keede.mombti.core.domains.report.calculator.BmrCalculator;
import io.keede.mombti.core.domains.report.calculator.BodyCompositionEstimator;
import io.keede.mombti.core.domains.report.calculator.BodyIndexCalculator;
import io.keede.mombti.core.domains.report.calculator.EnergyTargetCalculator;
import io.keede.mombti.core.domains.report.calculator.ExerciseEnergyCalculator;
import io.keede.mombti.core.domains.report.calculator.TdeeCalculator;
import io.keede.mombti.core.domains.report.defaults.ProfileDefaults;
import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.EnergyTarget;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import java.math.BigDecimal;
import java.util.Optional;

/** 시뮬레이션 테스트용 조립. 서비스 계층과 같은 순서로 입력을 만든다. */
final class SimulationFixture {

    final BodyIndexCalculator bodyIndexCalculator = new BodyIndexCalculator();
    final BodyCompositionEstimator bodyCompositionEstimator = new BodyCompositionEstimator();
    final SafetyPolicy safetyPolicy = new SafetyPolicy(bodyIndexCalculator);
    final ProfileDefaults defaults = new ProfileDefaults(bodyIndexCalculator, bodyCompositionEstimator);
    final BmrCalculator bmrCalculator = new BmrCalculator(bodyCompositionEstimator);
    final TdeeCalculator tdeeCalculator = new TdeeCalculator();
    final EnergyTargetCalculator energyTargetCalculator =
            new EnergyTargetCalculator(safetyPolicy, bodyCompositionEstimator);
    final WeightProjectionSimulator simulator = new WeightProjectionSimulator(bmrCalculator, tdeeCalculator,
            new ExerciseEnergyCalculator(), energyTargetCalculator, bodyCompositionEstimator);
    final GoalEtaCalculator goalEtaCalculator = new GoalEtaCalculator(simulator);

    record Case(ResolvedProfile profile, ReportMode mode, EnergyTarget target, SimulationInput input) {
    }

    Case of(Gender gender, int age, String heightCm, String weightKg) {
        return of(gender, age, heightCm, weightKg, null, null, null);
    }

    Case of(Gender gender, int age, String heightCm, String weightKg,
            ActivityLevel activity, String targetKg, String bodyFatPercent) {
        BodyProfile body = new BodyProfile(gender, age, new BigDecimal(heightCm), new BigDecimal(weightKg),
                Optional.ofNullable(activity),
                Optional.ofNullable(targetKg).map(BigDecimal::new),
                Optional.ofNullable(bodyFatPercent).map(BigDecimal::new));
        ResolvedProfile profile = defaults.resolve(body);
        ReportMode mode = safetyPolicy.decideMode(profile);
        BigDecimal tdee = tdeeCalculator.tdee(bmrCalculator.bmr(profile).value(), profile.pal());
        EnergyTarget target = energyTargetCalculator.calculate(profile, tdee, mode);
        return new Case(profile, mode, target, simulator.input(profile, target, mode));
    }

    BigDecimal goalKg(Case c) {
        return c.profile().goal().value().weightKg();
    }
}
