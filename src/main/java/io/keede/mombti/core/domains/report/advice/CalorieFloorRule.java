package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.ImprovementKind;
import java.util.List;

/** 섭취 하한을 적용함 (E-16): 더 줄이기보다 움직임을 늘리도록 안내 (AHA_ACC_TOS_2013). */
class CalorieFloorRule implements ImprovementRule {

    @Override
    public boolean applies(AdviceContext context) {
        return context.energyTarget().floorApplied();
    }

    @Override
    public Improvement build(AdviceContext context) {
        return new Improvement("섭취를 더 줄이기보다 움직임을 늘려 보세요",
                "하루 " + TemplateAdviceNarrator.kcal(context.energyTarget().intakeKcal())
                        + "kcal은 최소 권장량이에요. 이보다 적게 먹기보다 운동 라인을 참고해 소비를 늘리는 쪽을 권해요.",
                ImprovementKind.PERSONAL, 20, List.of(Evidence.AHA_ACC_TOS_2013));
    }
}
