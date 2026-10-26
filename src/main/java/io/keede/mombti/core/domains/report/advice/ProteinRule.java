package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.ImprovementKind;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.util.List;

/** 감량 모드: 계산된 단백질 g을 끼니마다 나눠 먹기 (LEIDY_2015). */
class ProteinRule implements ImprovementRule {

    @Override
    public boolean applies(AdviceContext context) {
        return context.mode() == ReportMode.LOSS;
    }

    @Override
    public Improvement build(AdviceContext context) {
        return new Improvement("단백질을 끼니마다 나눠 드세요",
                "하루 " + context.macroPlan().proteinG().toPlainString()
                        + "g을 아침·점심·저녁에 고르게 나눠 드세요. 감량 중 근육을 지키는 데 도움이 돼요.",
                ImprovementKind.PERSONAL, 30, List.of(Evidence.LEIDY_2015));
    }
}
