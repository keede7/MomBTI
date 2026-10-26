package io.keede.mombti.core.domains.report.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.model.ChartWindow;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.GoalEta;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.ProjectionPoint;
import io.keede.mombti.core.domains.report.model.Scenario;
import io.keede.mombti.core.domains.report.model.ScenarioProjections;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ChartWindowPolicyTest {

    private final ChartWindowPolicy policy = new ChartWindowPolicy();

    @Test
    void E18_12주에_도달하면_도달_지점_표시하고_6개월_토글_숨김() {
        ChartWindow window = policy.window(new GoalEta.Reached(12));

        assertThat(window).isEqualTo(new ChartWindow.GoalInView(12));
        assertThat(window.sixMonthToggle()).isFalse();
        assertThat(window.defaultWeeks()).isEqualTo(12);
    }

    @Test
    void E19_13주에_도달하면_목표까지_약_N주와_6개월_토글() {
        ChartWindow window = policy.window(new GoalEta.Reached(13));

        assertThat(window).isEqualTo(new ChartWindow.GoalWithinSixMonths(13));
        assertThat(window.sixMonthToggle()).isTrue();
        assertThat(window.defaultWeeks()).isEqualTo(12);
    }

    @Test
    void E19_26주에_도달하면_목표까지_약_N주와_6개월_토글() {
        ChartWindow window = policy.window(new GoalEta.Reached(26));

        assertThat(window).isEqualTo(new ChartWindow.GoalWithinSixMonths(26));
        assertThat(window.sixMonthToggle()).isTrue();
    }

    @Test
    void E20_27주에_도달하면_6개월_토글과_중간_목표_안내() {
        ChartWindow window = policy.window(new GoalEta.Reached(27));

        assertThat(window).isInstanceOf(ChartWindow.GoalBeyondSixMonths.class);
        assertThat(window.sixMonthToggle()).isTrue();
        assertThat(window.defaultWeeks()).isEqualTo(12);
    }

    @Test
    void E20_52주_미도달이면_6개월_토글과_중간_목표_안내() {
        ChartWindow window = policy.window(
                new GoalEta.Unreachable(new BigDecimal("70.1"), new BigDecimal("65.0")));

        assertThat(window).isInstanceOf(ChartWindow.GoalBeyondSixMonths.class);
        assertThat(window.sixMonthToggle()).isTrue();
    }

    @Test
    void 유지_모드는_12주_고정이고_토글_없음() {
        ChartWindow window = policy.window(new GoalEta.NotApplicable());

        assertThat(window).isInstanceOf(ChartWindow.NoGoal.class);
        assertThat(window.sixMonthToggle()).isFalse();
        assertThat(window.defaultWeeks()).isEqualTo(12);
    }

    @Test
    void 차트_데이터는_0주부터_26주까지_2주_간격() {
        SimulationFixture fixture = new SimulationFixture();
        SimulationFixture.Case c = fixture.of(Gender.MALE, 30, "175.0", "85.0");
        Projection projection = fixture.simulator.project(c.input(), Scenario.DIET_ONLY, SimulationVariant.BASE);

        assertThat(policy.chartPoints(projection)).extracting(ProjectionPoint::week)
                .containsExactlyElementsOf(IntStream.rangeClosed(0, 13).map(i -> i * 2).boxed().toList());
        assertThat(policy.chartPoints(projection).getLast()).isEqualTo(projection.points().get(26));
    }

    @Test
    void 차트용_시나리오_예측은_기본_밴드_현실보정_모두_2주_간격_26주() {
        SimulationFixture fixture = new SimulationFixture();
        SimulationFixture.Case c = fixture.of(Gender.MALE, 30, "175.0", "85.0");
        ScenarioProjections full = fixture.simulator.simulate(c.input(), Scenario.EX4);

        ScenarioProjections chart = policy.chartProjections(full);

        assertThat(chart.scenario()).isEqualTo(Scenario.EX4);
        assertThat(List.of(chart.base(), chart.bmrLow(), chart.bmrHigh(), chart.adherence()))
                .allSatisfy(projection -> {
                    assertThat(projection.scenario()).isEqualTo(Scenario.EX4);
                    assertThat(projection.points()).hasSize(14);
                    assertThat(projection.points().getLast().week()).isEqualTo(26);
                });
        assertThat(chart.bmrLow().points()).isEqualTo(policy.chartPoints(full.bmrLow()));
    }
}
