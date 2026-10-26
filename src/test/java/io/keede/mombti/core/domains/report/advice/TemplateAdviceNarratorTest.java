package io.keede.mombti.core.domains.report.advice;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Faq;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.Narration;
import io.keede.mombti.core.domains.report.model.Notice;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TemplateAdviceNarratorTest {

    private static final AdviceFixture FIXTURE = new AdviceFixture();
    private static final List<String> FORBIDDEN = List.of("비만입니다", "위험합니다", "실패", "반드시", "죄송합니다");

    private final AdviceNarrator narrator = new TemplateAdviceNarrator();
    private final ImprovementRuleEngine engine = ImprovementRuleEngine.withDefaultRules(FIXTURE.safetyPolicy);

    static Stream<AdviceContext> contexts() {
        return Stream.of(
                FIXTURE.of(Gender.MALE, 30, "175.0", "85.0"),                                  // E-14 감량
                FIXTURE.of(Gender.FEMALE, 28, "160.0", "65.0", ActivityLevel.LIGHT, "55.0", "32.0"),
                FIXTURE.of(Gender.FEMALE, 30, "165.0", "55.0"),                                // E-30
                FIXTURE.of(Gender.FEMALE, 30, "165.0", "48.0"),                                // E-11 저체중
                FIXTURE.of(Gender.MALE, 40, "175.0", "70.0", null, "69.5", null),              // E-12
                FIXTURE.of(Gender.FEMALE, 70, "150.0", "62.0"),                                // E-06, E-16
                FIXTURE.of(Gender.MALE, 35, "170.0", "125.0"));                                // BMI ≥ 41
    }

    @ParameterizedTest
    @MethodSource("contexts")
    void Narrator_출력에_금지_표현이_없음(AdviceContext context) {
        List<String> texts = allTexts(narrator.narrate(context));
        engine.evaluate(context).forEach(improvement -> {
            texts.add(improvement.title());
            texts.add(improvement.body());
        });

        assertThat(texts).isNotEmpty().allSatisfy(text -> {
            assertThat(text).isNotBlank().doesNotContain("null", "N/A");
            FORBIDDEN.forEach(word -> assertThat(text).doesNotContain(word));
        });
    }

    @ParameterizedTest
    @MethodSource("contexts")
    void 문장은_존댓말_요체로_끝남(AdviceContext context) {
        Narration narration = narrator.narrate(context);
        List<String> sentences = new ArrayList<>();
        sentences.add(narration.headline());
        narration.notices().forEach(notice -> sentences.add(notice.body()));
        narration.faqs().forEach(faq -> sentences.add(faq.answer()));

        assertThat(sentences).allSatisfy(text -> assertThat(text.strip()).matches("(?s).*요[.?!]?$"));
    }

    @Test
    void 미리_알아둘_것_4개() {
        Narration narration = narrator.narrate(FIXTURE.of(Gender.MALE, 30, "175.0", "85.0"));

        assertThat(narration.notices()).extracting(Notice::title).containsExactly(
                "처음 1~2주는 수분이 먼저 빠져요",
                "하루 1~2kg 변동은 정상이에요",
                "정체기는 자연스럽게 와요",
                "4주마다 다시 계산해 보세요");
    }

    @Test
    void FAQ_3개() {
        Narration narration = narrator.narrate(FIXTURE.of(Gender.MALE, 30, "175.0", "85.0"));

        assertThat(narration.faqs()).extracting(Faq::question).containsExactly(
                "다른 계산기보다 왜 느리게 빠지나요?",
                "운동 라인은 어떤 강도 기준인가요?",
                "탄수화물 비율이 왜 낮은가요?");
        assertThat(narration.faqs()).allSatisfy(faq -> assertThat(faq.evidence()).isNotEmpty());
    }

    @Test
    void 계산된_값만_문장에_넣는다() {
        AdviceContext context = FIXTURE.of(Gender.FEMALE, 28, "160.0", "65.0", ActivityLevel.LIGHT, "55.0", null);
        Narration narration = narrator.narrate(context);

        assertThat(narration.headline())
                .contains(TemplateAdviceNarrator.kcal(context.energyTarget().intakeKcal()) + "kcal");
        assertThat(narration.faqs().get(2).answer())
                .contains(context.macroPlan().carbPercent() + "%")
                .contains(context.macroPlan().proteinG().toPlainString() + "g");
    }

    @Test
    void E16_하한을_적용하면_최소_권장량_안내() {
        AdviceContext context = FIXTURE.of(Gender.FEMALE, 70, "150.0", "62.0");

        assertThat(context.energyTarget().floorApplied()).isTrue();
        assertThat(narrator.narrate(context).headline()).contains("최소 권장량으로 맞췄어요");
    }

    @Test
    void E11_저체중이면_중립적인_유지_문구() {
        AdviceContext context = FIXTURE.of(Gender.FEMALE, 30, "165.0", "48.0");

        assertThat(context.mode()).isEqualTo(ReportMode.UNDERWEIGHT_MAINTAIN);
        assertThat(narrator.narrate(context).headline()).contains("유지").doesNotContain("감량하세요");
    }

    @Test
    void E12_목표가_현재_체중_근처면_이미_목표_근처() {
        AdviceContext context = FIXTURE.of(Gender.MALE, 40, "175.0", "70.0", null, "69.5", null);

        assertThat(context.mode()).isEqualTo(ReportMode.MAINTAIN);
        assertThat(narrator.narrate(context).headline()).contains("이미 목표 근처예요");
    }

    @Test
    void E30_모든_지표가_건강_범위이고_목표가_없으면_긍정_문구() {
        AdviceContext context = FIXTURE.of(Gender.FEMALE, 30, "165.0", "55.0");

        Narration narration = narrator.narrate(context);

        assertThat(context.allHealthyWithoutGoal()).isTrue();
        assertThat(narration.positiveEmptyState()).isTrue();
        assertThat(narration.headline()).startsWith("지금 잘 관리하고 있어요");
    }

    @Test
    void 감량_모드는_긍정_빈_상태가_아님() {
        Narration narration = narrator.narrate(FIXTURE.of(Gender.MALE, 30, "175.0", "85.0"));

        assertThat(narration.positiveEmptyState()).isFalse();
    }

    private static List<String> allTexts(Narration narration) {
        List<String> texts = new ArrayList<>();
        texts.add(narration.headline());
        for (Notice notice : narration.notices()) {
            texts.add(notice.title());
            texts.add(notice.body());
        }
        for (Faq faq : narration.faqs()) {
            texts.add(faq.question());
            texts.add(faq.answer());
        }
        return texts;
    }
}
