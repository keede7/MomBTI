package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.BmiCategory;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.ImprovementKind;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.util.List;

/** BMI 23 이상 감량 모드: 첫 목표로 체중의 5~10% 감량 (KSSO_2022, WING_2011). */
class BmiRule implements ImprovementRule {

    @Override
    public boolean applies(AdviceContext context) {
        return context.mode() == ReportMode.LOSS
                && context.bmiCategory().compareTo(BmiCategory.PRE_OBESE) >= 0;
    }

    @Override
    public Improvement build(AdviceContext context) {
        return new Improvement("체중 관리가 권장되는 구간이에요",
                "BMI " + TemplateAdviceNarrator.oneDecimal(context.bmi()) + "로 '" + context.bmiCategory().label()
                        + "' 구간이에요. 지금 체중의 5~10%만 줄여도 건강 지표가 좋아질 수 있어요.",
                ImprovementKind.PERSONAL, 10, List.of(Evidence.KSSO_2022, Evidence.WING_2011));
    }
}
