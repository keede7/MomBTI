package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.ImprovementKind;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.util.List;

/** 목표 체중 없이 감량 모드 (E-14): 기본 목표(BMI 22.0)로 계산했음을 안내 (KSSO_2022). */
class DefaultGoalRule implements ImprovementRule {

    @Override
    public boolean applies(AdviceContext context) {
        return context.mode() == ReportMode.LOSS && context.profile().goal().estimated();
    }

    @Override
    public Improvement build(AdviceContext context) {
        return new Improvement("나만의 목표 체중을 정해 보세요",
                "목표 체중을 입력하지 않아 기본 목표(건강 체중)로 계산했어요. 원하는 목표가 있으면 바꿔 볼 수 있어요.",
                ImprovementKind.GENERAL_TIP, 80, List.of(Evidence.KSSO_2022));
    }
}
