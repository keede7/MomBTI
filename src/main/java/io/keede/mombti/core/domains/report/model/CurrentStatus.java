package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 현재 상태 (docs/PRD.md 리포트 구성 6).
 *
 * @param bmi               반올림 전 BMI (KSSO_2022)
 * @param bodyFatLargeError BMI ≥ 41이라 체지방률 추정 오차가 클 수 있음 (DEURENBERG_1991)
 * @param bodyFatMismatch   입력 체지방률이 BMI 기반 추정값과 15%p 이상 차이남. 경고만 하고 입력값을 쓴다 (E-24)
 * @param tdeeLowerKcal     하루 소비량 범위 하단: 기초대사량 × 0.9 × PAL (FRANKENFIELD_2005)
 * @param tdeeUpperKcal     하루 소비량 범위 상단: 기초대사량 × 1.1 × 밴드 상단 PAL
 */
public record CurrentStatus(
        BigDecimal bmi,
        BmiCategory bmiCategory,
        Estimated<BigDecimal> bodyFatPercent,
        Estimated<BigDecimal> fatMassKg,
        boolean bodyFatLargeError,
        boolean bodyFatMismatch,
        Estimated<BigDecimal> bmrKcal,
        Estimated<BigDecimal> tdeeKcal,
        BigDecimal tdeeLowerKcal,
        BigDecimal tdeeUpperKcal
) {
    public CurrentStatus {
        Objects.requireNonNull(bmi, "bmi");
        Objects.requireNonNull(bmiCategory, "bmiCategory");
        Objects.requireNonNull(bodyFatPercent, "bodyFatPercent");
        Objects.requireNonNull(fatMassKg, "fatMassKg");
        Objects.requireNonNull(bmrKcal, "bmrKcal");
        Objects.requireNonNull(tdeeKcal, "tdeeKcal");
        Objects.requireNonNull(tdeeLowerKcal, "tdeeLowerKcal");
        Objects.requireNonNull(tdeeUpperKcal, "tdeeUpperKcal");
    }
}
