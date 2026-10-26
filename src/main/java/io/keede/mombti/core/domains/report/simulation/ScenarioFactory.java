package io.keede.mombti.core.domains.report.simulation;

import io.keede.mombti.core.domains.report.model.Scenario;
import java.util.List;

/** 식단만 / 식단 + 주 3·4·5일 운동 (ADR-020). 표와 범례 순서와 같다. */
public class ScenarioFactory {

    public List<Scenario> create() {
        return List.of(Scenario.DIET_ONLY, Scenario.EX3, Scenario.EX4, Scenario.EX5);
    }
}
