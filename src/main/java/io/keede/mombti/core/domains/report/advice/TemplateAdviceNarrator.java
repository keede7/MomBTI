package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.calculator.ExerciseEnergyCalculator;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.EnergyTarget;
import io.keede.mombti.core.domains.report.model.Faq;
import io.keede.mombti.core.domains.report.model.MacroPlan;
import io.keede.mombti.core.domains.report.model.Narration;
import io.keede.mombti.core.domains.report.model.Notice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * MVP 문장 템플릿 구현체. 문구 톤은 docs/UI_GUIDE.md "문구 톤" (존댓말 "~해요", 금지 표현 없음).
 * 문장 속 숫자는 {@link AdviceContext}의 계산값과 {@link Evidence}에 연결된 상수뿐이다.
 */
public class TemplateAdviceNarrator implements AdviceNarrator {

    private static final List<Notice> NOTICES = List.of(
            new Notice("처음 1~2주는 수분이 먼저 빠져요",
                    "식사량을 바꾸면 처음에는 몸속 수분과 글리코겐이 먼저 줄어 체중이 빠르게 변해요. "
                            + "이후 속도가 느려지는 건 자연스러운 일이에요."),
            new Notice("하루 1~2kg 변동은 정상이에요",
                    "물, 소금, 식사 시간에 따라 하루에도 체중이 오르내려요. 같은 시간에 재고 주 평균으로 봐 주세요."),
            new Notice("정체기는 자연스럽게 와요",
                    "체중이 줄면 소비량도 함께 줄어서 같은 양을 먹어도 속도가 느려져요. "
                            + "그래프의 곡선이 점점 완만해지는 이유예요."),
            new Notice("4주마다 다시 계산해 보세요",
                    "이 예측은 지금 정한 섭취량을 계속 지킨다고 가정했어요. "
                            + "4주마다 바뀐 체중으로 다시 계산하면 계획이 몸에 맞게 조정돼요."));

    @Override
    public Narration narrate(AdviceContext context) {
        return new Narration(headline(context), context.allHealthyWithoutGoal(), NOTICES, faqs(context.macroPlan()));
    }

    private static String headline(AdviceContext context) {
        EnergyTarget target = context.energyTarget();
        String intake = kcal(target.intakeKcal());
        return switch (context.mode()) {
            case LOSS -> lossHeadline(target, intake);
            case MAINTAIN -> context.allHealthyWithoutGoal()
                    ? "지금 잘 관리하고 있어요. 하루 " + intake + "kcal 안팎으로 지금처럼 유지해 보세요."
                    : context.profile().goal().estimated()
                            ? "지금 체중을 유지하는 계획이에요. 하루 " + intake + "kcal 안팎으로 드세요."
                            : "이미 목표 근처예요. 하루 " + intake + "kcal 안팎으로 지금 체중을 유지해 보세요.";
            case UNDERWEIGHT_MAINTAIN -> "지금은 체중을 줄이기보다 유지하는 쪽을 권해요. 하루 " + intake
                    + "kcal 정도를 세 끼에 고르게 나눠 드세요.";
            case MINOR_BLOCKED -> "지금은 감량 계획보다 고른 식사와 충분한 활동이 더 중요해요.";
        };
    }

    private static String lossHeadline(EnergyTarget target, String intake) {
        if (target.dietOnlyMaintains()) {
            return "너무 적게 먹지 않도록 최소 권장량으로 맞췄어요. 하루 " + intake
                    + "kcal로는 식단만으로 체중이 유지에 가까워서, 운동 라인으로 감량 속도를 확인해 보세요.";
        }
        String headline = "하루 " + intake + "kcal로 시작해 보세요. 소비량보다 " + kcal(target.deficitKcal())
                + "kcal 적게 먹는 계획이에요.";
        if (target.floorApplied()) {
            headline += " 너무 적게 먹지 않도록 최소 권장량으로 맞췄어요.";
        }
        return headline;
    }

    private static List<Faq> faqs(MacroPlan macro) {
        return List.of(
                new Faq("다른 계산기보다 왜 느리게 빠지나요?",
                        "많은 계산기는 '7,700kcal = 1kg' 규칙을 써요. 이 규칙은 체중이 줄면서 소비량도 줄어드는 점을 "
                                + "반영하지 않아 오래 할수록 감량을 크게 잡아요. 여기서는 하루 단위로 소비량과 "
                                + "지방·제지방 변화를 다시 계산하는 모델을 써서 더 현실적인 속도를 보여줘요.",
                        List.of(Evidence.HALL_2011, Evidence.HALL_2007_FORBES, Evidence.HALL_2008)),
                new Faq("운동 라인은 어떤 강도 기준인가요?",
                        "운동 1회를 중강도 " + ExerciseEnergyCalculator.SESSION_MINUTES + "분, 빠르게 걷기(MET "
                                + ExerciseEnergyCalculator.BRISK_WALKING_MET.toPlainString()
                                + ") 수준으로 가정했어요. 주 3·4·5일 운동한 소비량을 일주일에 고르게 나눠 더했어요. "
                                + "실제 강도와 시간에 따라 결과가 달라질 수 있어요.",
                        List.of(Evidence.HERRMANN_2024, Evidence.EXERCISE_SESSION_45)),
                new Faq("탄수화물 비율이 왜 낮은가요?",
                        "근육을 지키려고 단백질을 하루 " + macro.proteinG().toPlainString() + "g으로 먼저 정하고, "
                                + "지방을 " + macro.fatPercent() + "%로 둔 뒤 나머지를 탄수화물로 채웠어요. "
                                + "그래서 탄수화물이 " + macro.carbPercent() + "%로 일반 권장 비율보다 낮을 수 있어요.",
                        List.of(Evidence.LEIDY_2015, Evidence.IOM_AMDR_2005)));
    }

    /** kcal 표기: 천 단위 쉼표, 정수. */
    static String kcal(BigDecimal value) {
        return new DecimalFormat("#,##0").format(value.setScale(0, RoundingMode.HALF_UP));
    }

    /** 소수 1자리 표기 (BMI). */
    static String oneDecimal(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }
}
