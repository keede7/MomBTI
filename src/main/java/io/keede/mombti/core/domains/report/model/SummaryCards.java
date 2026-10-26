package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 요약 카드 4개 (docs/PRD.md 리포트 구성 2). 예측 값은 모두 식단만 기본 예측 기준이다.
 *
 * @param dietOnlyEta               목표 도달 예상 (주차 범위)
 * @param intakeKcal                하루 섭취 목표
 * @param tdeeKcal                  하루 소비량 (활동량 미입력이면 추정)
 * @param deficitKcal               소비량 대비 차이
 * @param weightAt12WeeksKg         3개월(12주) 후 예상 체중
 * @param changeAt12WeeksKg         3개월 후 변화량 (감량이면 음수)
 * @param weeklyLossKg              첫 4주 평균 주당 감량
 * @param weeklyLossWithinSafeRange 주당 감량이 현재 체중의 1% 이하 (HELMS_2014)
 */
public record SummaryCards(
        GoalEtaRange dietOnlyEta,
        BigDecimal intakeKcal,
        Estimated<BigDecimal> tdeeKcal,
        BigDecimal deficitKcal,
        BigDecimal weightAt12WeeksKg,
        BigDecimal changeAt12WeeksKg,
        BigDecimal weeklyLossKg,
        boolean weeklyLossWithinSafeRange
) {
    public SummaryCards {
        Objects.requireNonNull(dietOnlyEta, "dietOnlyEta");
        Objects.requireNonNull(intakeKcal, "intakeKcal");
        Objects.requireNonNull(tdeeKcal, "tdeeKcal");
        Objects.requireNonNull(deficitKcal, "deficitKcal");
        Objects.requireNonNull(weightAt12WeeksKg, "weightAt12WeeksKg");
        Objects.requireNonNull(changeAt12WeeksKg, "changeAt12WeeksKg");
        Objects.requireNonNull(weeklyLossKg, "weeklyLossKg");
    }
}
