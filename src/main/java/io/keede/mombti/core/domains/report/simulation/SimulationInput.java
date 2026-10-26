package io.keede.mombti.core.domains.report.simulation;

import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 시뮬레이션 시작 상태와 고정 섭취량. {@link WeightProjectionSimulator#input}으로 만든다.
 *
 * @param useLeanMassBmr 체지방률을 입력했으면 Cunningham(제지방량), 아니면 Mifflin-St Jeor(체중)
 * @param intakeKcal     하루 섭취량 (52주 고정). 식단만으로 적자가 없으면 시작 소비량 (유지선, E-17)
 * @param tdeeKcal       시작 시점 하루 소비량 (운동 제외). 현실 보정 섭취 계산에 쓴다
 */
public record SimulationInput(
        Gender gender,
        int age,
        BigDecimal heightCm,
        BigDecimal pal,
        boolean useLeanMassBmr,
        BigDecimal initialFatMassKg,
        BigDecimal initialLeanMassKg,
        BigDecimal intakeKcal,
        BigDecimal tdeeKcal,
        ReportMode mode
) {
    public SimulationInput {
        Objects.requireNonNull(gender, "gender");
        Objects.requireNonNull(heightCm, "heightCm");
        Objects.requireNonNull(pal, "pal");
        Objects.requireNonNull(initialFatMassKg, "initialFatMassKg");
        Objects.requireNonNull(initialLeanMassKg, "initialLeanMassKg");
        Objects.requireNonNull(intakeKcal, "intakeKcal");
        Objects.requireNonNull(tdeeKcal, "tdeeKcal");
        Objects.requireNonNull(mode, "mode");
    }

    public BigDecimal initialWeightKg() {
        return initialFatMassKg.add(initialLeanMassKg);
    }
}
