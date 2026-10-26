package io.keede.mombti.core.domains.report.model;

import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 리포트 전체 결과 (docs/PRD.md 리포트 구성 1~11). 모드는 LOSS, MAINTAIN, UNDERWEIGHT_MAINTAIN 중 하나다.
 *
 * @param profile          선택 항목까지 확정된 입력과 추정 여부 (상단 바 맞춤 배지, 기본 목표 라벨)
 * @param startDate        오늘(서울). 예상 날짜는 {@link #dateAfterWeeks(int)}
 * @param projections      시나리오 4개의 0~52주 예측 (기본, 개인차 밴드, 현실 보정)
 * @param chartProjections 그래프와 표 데이터: 같은 예측의 0~26주 2주 간격
 * @param etas             목표 도달 예측 표 (식단만 / 주 3·4·5일)
 * @param milestones       단계별 목표. 감량 모드가 아니면 비어 있다
 * @param evidence         리포트에 쓰인 출처만, {@link Evidence} 선언 순서
 */
public record Report(
        ReportMode mode,
        ResolvedProfile profile,
        LocalDate startDate,
        SummaryCards summary,
        List<ScenarioProjections> projections,
        List<ScenarioProjections> chartProjections,
        ChartWindow chartWindow,
        List<ScenarioEta> etas,
        List<Milestone> milestones,
        CurrentStatus currentStatus,
        EnergyTarget energyTarget,
        MacroPlan macroPlan,
        MealPlan mealPlan,
        Narration narration,
        List<Improvement> improvements,
        List<Evidence> evidence
) implements ReportResult {

    public Report {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(chartWindow, "chartWindow");
        Objects.requireNonNull(currentStatus, "currentStatus");
        Objects.requireNonNull(energyTarget, "energyTarget");
        Objects.requireNonNull(macroPlan, "macroPlan");
        Objects.requireNonNull(mealPlan, "mealPlan");
        Objects.requireNonNull(narration, "narration");
        if (mode == ReportMode.MINOR_BLOCKED) {
            throw new IllegalArgumentException("MINOR_BLOCKED는 ReportResult.MinorBlocked로 표현한다");
        }
        projections = List.copyOf(projections);
        chartProjections = List.copyOf(chartProjections);
        etas = List.copyOf(etas);
        milestones = List.copyOf(milestones);
        improvements = List.copyOf(improvements);
        evidence = List.copyOf(evidence);
    }

    /** 오늘부터 N주 뒤 날짜. */
    public LocalDate dateAfterWeeks(int weeks) {
        return startDate.plusWeeks(weeks);
    }

    /** 사용자가 직접 입력한 선택 항목 수 ("맞춤 분석 +N"). */
    public int providedOptionalCount() {
        return profile.providedOptionalCount();
    }
}
