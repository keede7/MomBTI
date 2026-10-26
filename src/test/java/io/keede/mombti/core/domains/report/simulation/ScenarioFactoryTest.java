package io.keede.mombti.core.domains.report.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.model.Scenario;
import org.junit.jupiter.api.Test;

class ScenarioFactoryTest {

    @Test
    void 식단만과_주_3_4_5일_운동_시나리오_4개를_순서대로_만든다() {
        assertThat(new ScenarioFactory().create())
                .containsExactly(Scenario.DIET_ONLY, Scenario.EX3, Scenario.EX4, Scenario.EX5)
                .extracting(Scenario::sessionsPerWeek).containsExactly(0, 3, 4, 5);
    }
}
