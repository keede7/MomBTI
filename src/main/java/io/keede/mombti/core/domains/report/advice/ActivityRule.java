package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Estimated;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.ImprovementKind;
import java.util.List;

/**
 * 활동량. 입력한 단계가 2단계 이하면 걸음 수 늘리기(데이터 기반),
 * 입력하지 않았으면 활동량 입력 권유(일상 팁) (TUDOR_LOCKE_2004, FAO_WHO_UNU_2004).
 */
class ActivityRule implements ImprovementRule {

    private static final List<Evidence> EVIDENCE = List.of(Evidence.TUDOR_LOCKE_2004, Evidence.FAO_WHO_UNU_2004);

    @Override
    public boolean applies(AdviceContext context) {
        Estimated<ActivityLevel> activity = context.profile().activityLevel();
        return activity.estimated() || activity.value().step() <= ActivityLevel.LIGHT.step();
    }

    @Override
    public Improvement build(AdviceContext context) {
        Estimated<ActivityLevel> activity = context.profile().activityLevel();
        if (activity.estimated()) {
            return new Improvement("평소 활동량을 알려주면 오차가 줄어요",
                    "활동량을 입력하지 않아 가장 낮은 단계로 계산했어요. '더 정확하게 보기'에서 알려주세요.",
                    ImprovementKind.GENERAL_TIP, 60, EVIDENCE);
        }
        ActivityLevel next = ActivityLevel.values()[activity.value().ordinal() + 1];
        return new Improvement("일상 걸음 수를 조금씩 늘려 보세요",
                "지금은 '" + activity.value().description() + "' 단계예요. 걸음 수를 다음 단계(" + next.steps()
                        + ")로 늘리면 운동을 따로 하지 않아도 소비량이 커져요.",
                ImprovementKind.PERSONAL, 40, EVIDENCE);
    }
}
