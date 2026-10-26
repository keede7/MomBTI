package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.ImprovementKind;
import java.util.List;

/** 체지방률을 입력하지 않음: 측정값 입력 권유 (DEURENBERG_1991, BMI ≥ 41이면 오차 안내). */
class BodyFatRule implements ImprovementRule {

    @Override
    public boolean applies(AdviceContext context) {
        return context.profile().bodyFatPercent().estimated();
    }

    @Override
    public Improvement build(AdviceContext context) {
        String body = "체지방률은 키·몸무게·나이로 추정했어요. 체성분 측정값이 있으면 입력해 보세요.";
        if (context.bodyFatLargeError()) {
            body += " 지금 체격에서는 추정 오차가 클 수 있어요.";
        }
        return new Improvement("체지방률을 알면 예측이 더 정확해요", body,
                ImprovementKind.GENERAL_TIP, 70, List.of(Evidence.DEURENBERG_1991));
    }
}
