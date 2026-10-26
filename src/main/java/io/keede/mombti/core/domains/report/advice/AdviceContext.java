package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.model.BmiCategory;
import io.keede.mombti.core.domains.report.model.EnergyTarget;
import io.keede.mombti.core.domains.report.model.MacroPlan;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 개선점 규칙과 {@link AdviceNarrator}가 읽는 계산 결과. 이 값들만 문장에 옮기고 새 숫자는 만들지 않는다.
 *
 * @param bmi               현재 BMI (반올림 전)
 * @param bodyFatLargeError 체지방률 추정 오차가 클 수 있음 (BMI ≥ 41, DEURENBERG_1991)
 */
public record AdviceContext(
        ResolvedProfile profile,
        ReportMode mode,
        BigDecimal bmi,
        BmiCategory bmiCategory,
        boolean bodyFatLargeError,
        EnergyTarget energyTarget,
        MacroPlan macroPlan
) {
    public AdviceContext {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(bmi, "bmi");
        Objects.requireNonNull(bmiCategory, "bmiCategory");
        Objects.requireNonNull(energyTarget, "energyTarget");
        Objects.requireNonNull(macroPlan, "macroPlan");
    }

    /** 모든 지표가 건강 범위이고 목표 체중을 입력하지 않음 → 긍정 빈 상태 (E-30). */
    public boolean allHealthyWithoutGoal() {
        return mode == ReportMode.MAINTAIN
                && bmiCategory == BmiCategory.NORMAL
                && profile.goal().estimated();
    }
}
