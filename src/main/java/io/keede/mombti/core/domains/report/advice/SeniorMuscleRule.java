package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.ImprovementKind;
import io.keede.mombti.core.domains.report.safety.SafetyPolicy;
import java.util.List;

/** 65세 이상 근감소 주의 (E-06, PROT_AGE_2013). 나이 기준은 {@link SafetyPolicy}. */
class SeniorMuscleRule implements ImprovementRule {

    private final SafetyPolicy safetyPolicy;

    SeniorMuscleRule(SafetyPolicy safetyPolicy) {
        this.safetyPolicy = safetyPolicy;
    }

    @Override
    public boolean applies(AdviceContext context) {
        return safetyPolicy.isSenior(context.profile().age());
    }

    @Override
    public Improvement build(AdviceContext context) {
        return new Improvement("근육량 유지에 신경 써 주세요",
                "나이가 들수록 체중을 줄일 때 근육도 함께 줄기 쉬워요. 단백질을 끼니마다 챙기고, "
                        + "무리하지 않는 속도로 진행해요.",
                ImprovementKind.PERSONAL, 15, List.of(Evidence.PROT_AGE_2013));
    }
}
