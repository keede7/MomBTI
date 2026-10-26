package io.keede.mombti.core.domains.report.advice;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.ImprovementKind;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ImprovementRuleEngineTest {

    private final AdviceFixture fixture = new AdviceFixture();
    private final ImprovementRuleEngine engine = ImprovementRuleEngine.withDefaultRules(fixture.safetyPolicy);

    @Test
    void 개선점은_최대_5개() {
        // 규칙이 가장 많이 걸리는 경우: 감량 + 기본값 전부 + 65세 이상 + 하한 적용
        List<Improvement> improvements = engine.evaluate(fixture.of(Gender.FEMALE, 70, "150.0", "62.0"));

        assertThat(improvements).hasSizeLessThanOrEqualTo(ImprovementRuleEngine.MAX_IMPROVEMENTS);
        assertThat(ImprovementRuleEngine.MAX_IMPROVEMENTS).isEqualTo(5);
    }

    @Test
    void 규칙이_5개보다_많이_걸려도_5개로_자른다() {
        ImprovementRule always = new ImprovementRule() {
            @Override
            public boolean applies(AdviceContext context) {
                return true;
            }

            @Override
            public Improvement build(AdviceContext context) {
                return new Improvement("팁", "설명", ImprovementKind.PERSONAL, 1, List.of());
            }
        };
        ImprovementRuleEngine many = new ImprovementRuleEngine(IntStream.range(0, 8).mapToObj(i -> always).toList());

        assertThat(many.evaluate(fixture.of(Gender.MALE, 30, "175.0", "80.0"))).hasSize(5);
    }

    @Test
    void 데이터_기반_개선점이_일반_팁보다_먼저() {
        List<Improvement> improvements = engine.evaluate(fixture.of(Gender.MALE, 30, "175.0", "85.0"));

        List<ImprovementKind> kinds = improvements.stream().map(Improvement::kind).toList();
        assertThat(kinds).isSortedAccordingTo(ImprovementKind::compareTo);
        assertThat(kinds).contains(ImprovementKind.PERSONAL, ImprovementKind.GENERAL_TIP);
    }

    @Test
    void BMI_23_이상이면_BMI_개선점() {
        List<Improvement> improvements = engine.evaluate(fixture.of(Gender.MALE, 30, "175.0", "85.0"));

        assertThat(improvements).anySatisfy(improvement -> {
            assertThat(improvement.kind()).isEqualTo(ImprovementKind.PERSONAL);
            assertThat(improvement.evidence()).contains(Evidence.KSSO_2022);
        });
    }

    @Test
    void 활동량을_입력하지_않으면_일반_팁으로_구분() {
        List<Improvement> improvements = engine.evaluate(fixture.of(Gender.MALE, 30, "175.0", "85.0"));

        assertThat(improvements)
                .filteredOn(improvement -> improvement.evidence().contains(Evidence.TUDOR_LOCKE_2004))
                .singleElement()
                .extracting(Improvement::kind)
                .isEqualTo(ImprovementKind.GENERAL_TIP);
    }

    @Test
    void 활동량이_낮다고_입력하면_데이터_기반_개선점() {
        List<Improvement> improvements = engine.evaluate(
                fixture.of(Gender.MALE, 30, "175.0", "85.0", ActivityLevel.SEDENTARY, null, null));

        assertThat(improvements)
                .filteredOn(improvement -> improvement.evidence().contains(Evidence.TUDOR_LOCKE_2004))
                .singleElement()
                .extracting(Improvement::kind)
                .isEqualTo(ImprovementKind.PERSONAL);
    }

    @Test
    void 감량_모드면_단백질_개선점에_계산된_g을_사용() {
        AdviceContext context = fixture.of(Gender.MALE, 30, "175.0", "85.0");
        String proteinG = context.macroPlan().proteinG().toPlainString();

        assertThat(engine.evaluate(context))
                .filteredOn(improvement -> improvement.evidence().contains(Evidence.LEIDY_2015))
                .singleElement()
                .satisfies(improvement -> assertThat(improvement.body()).contains(proteinG + "g"));
    }

    @Test
    void E06_65세_이상이면_근감소_주의() {
        List<Improvement> improvements = engine.evaluate(fixture.of(Gender.MALE, 70, "170.0", "80.0"));

        assertThat(improvements)
                .anySatisfy(improvement -> assertThat(improvement.evidence()).contains(Evidence.PROT_AGE_2013));
    }

    @Test
    void E30_모든_지표가_건강_범위이고_목표가_없으면_데이터_기반_개선점이_없음() {
        List<Improvement> improvements = engine.evaluate(fixture.of(Gender.FEMALE, 30, "165.0", "55.0"));

        assertThat(improvements).allSatisfy(
                improvement -> assertThat(improvement.kind()).isEqualTo(ImprovementKind.GENERAL_TIP));
    }
}
