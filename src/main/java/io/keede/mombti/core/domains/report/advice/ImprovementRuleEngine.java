package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import java.util.Comparator;
import java.util.List;

/**
 * 개선할 부분 (docs/PRD.md 리포트 구성 10). 데이터 기반 개선점을 먼저, 일상 팁을 뒤에 두고
 * 같은 종류 안에서는 우선순위 순으로 최대 5개를 고른다.
 */
public class ImprovementRuleEngine {

    public static final int MAX_IMPROVEMENTS = 5;

    private final List<ImprovementRule> rules;

    public ImprovementRuleEngine(List<ImprovementRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static ImprovementRuleEngine withDefaultRules(SafetyPolicy safetyPolicy) {
        return new ImprovementRuleEngine(List.of(
                new BmiRule(),
                new SeniorMuscleRule(safetyPolicy),
                new CalorieFloorRule(),
                new ProteinRule(),
                new ActivityRule(),
                new BodyFatRule(),
                new DefaultGoalRule()));
    }

    public List<Improvement> evaluate(AdviceContext context) {
        return rules.stream()
                .filter(rule -> rule.applies(context))
                .map(rule -> rule.build(context))
                .sorted(Comparator.comparing(Improvement::kind).thenComparingInt(Improvement::priority))
                .limit(MAX_IMPROVEMENTS)
                .toList();
    }
}
