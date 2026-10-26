package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.calculator.BmrCalculator;
import io.keede.mombti.core.domains.report.calculator.BodyCompositionEstimator;
import io.keede.mombti.core.domains.report.calculator.BodyIndexCalculator;
import io.keede.mombti.core.domains.report.calculator.EnergyTargetCalculator;
import io.keede.mombti.core.domains.report.calculator.MacroCalculator;
import io.keede.mombti.core.domains.report.calculator.TdeeCalculator;
import io.keede.mombti.core.domains.report.defaults.ProfileDefaults;
import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.BodyProfile;
import io.keede.mombti.core.domains.report.model.EnergyTarget;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.MacroPlan;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import java.math.BigDecimal;
import java.util.Optional;

/** 조언 테스트용 조립. 서비스 계층과 같은 순서로 {@link AdviceContext}를 만든다. */
final class AdviceFixture {

    final BodyIndexCalculator bodyIndexCalculator = new BodyIndexCalculator();
    final BodyCompositionEstimator bodyCompositionEstimator = new BodyCompositionEstimator();
    final SafetyPolicy safetyPolicy = new SafetyPolicy(bodyIndexCalculator);
    final ProfileDefaults defaults = new ProfileDefaults(bodyIndexCalculator, bodyCompositionEstimator);
    final BmrCalculator bmrCalculator = new BmrCalculator(bodyCompositionEstimator);
    final TdeeCalculator tdeeCalculator = new TdeeCalculator();
    final EnergyTargetCalculator energyTargetCalculator =
            new EnergyTargetCalculator(safetyPolicy, bodyCompositionEstimator);
    final MacroCalculator macroCalculator = new MacroCalculator(safetyPolicy);

    AdviceContext of(Gender gender, int age, String heightCm, String weightKg) {
        return of(gender, age, heightCm, weightKg, null, null, null);
    }

    AdviceContext of(Gender gender, int age, String heightCm, String weightKg,
                     ActivityLevel activity, String targetKg, String bodyFatPercent) {
        BodyProfile body = new BodyProfile(gender, age, new BigDecimal(heightCm), new BigDecimal(weightKg),
                Optional.ofNullable(activity),
                Optional.ofNullable(targetKg).map(BigDecimal::new),
                Optional.ofNullable(bodyFatPercent).map(BigDecimal::new));
        ResolvedProfile profile = defaults.resolve(body);
        ReportMode mode = safetyPolicy.decideMode(profile);
        BigDecimal bmi = bodyIndexCalculator.bmi(profile.heightCm(), profile.weightKg());
        BigDecimal tdee = tdeeCalculator.tdee(bmrCalculator.bmr(profile).value(), profile.pal());
        EnergyTarget target = energyTargetCalculator.calculate(profile, tdee, mode);
        BigDecimal goalKg = mode == ReportMode.LOSS ? profile.goal().value().weightKg() : profile.weightKg();
        MacroPlan macro = macroCalculator.calculate(target.intakeKcal(), profile.weightKg(), goalKg, bmi,
                profile.age());
        return new AdviceContext(profile, mode, bmi, bodyIndexCalculator.category(bmi),
                bodyCompositionEstimator.isLargeErrorExpected(bmi), target, macro);
    }
}
