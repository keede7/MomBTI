package io.keede.mombti.core.domains.report.model;

/**
 * 그래프 기간 규칙 (ADR-021, docs/PRD.md "그래프 기간 규칙"). 기본 12주, 6개월 보기는 26주.
 * "식단만" 기본 예측의 도달 시점으로 정한다.
 */
public sealed interface ChartWindow {

    int DEFAULT_WEEKS = 12;
    int EXTENDED_WEEKS = 26;

    boolean sixMonthToggle();

    default int defaultWeeks() {
        return DEFAULT_WEEKS;
    }

    /** 12주 안에 도달: 도달 지점에 "목표 도달" 표시, 토글 숨김 (E-18). */
    record GoalInView(int week) implements ChartWindow {
        @Override
        public boolean sixMonthToggle() {
            return false;
        }
    }

    /** 13~26주에 도달: 오른쪽 끝에 "목표까지 약 N주" + 6개월 토글 (E-19). */
    record GoalWithinSixMonths(int week) implements ChartWindow {
        @Override
        public boolean sixMonthToggle() {
            return true;
        }
    }

    /** 26주 초과 또는 미도달: "6개월 안에는 어려워요" + 6개월 토글 + 중간 목표 안내 (E-20). */
    record GoalBeyondSixMonths() implements ChartWindow {
        @Override
        public boolean sixMonthToggle() {
            return true;
        }
    }

    /** 유지 모드: 목표선 없이 12주. */
    record NoGoal() implements ChartWindow {
        @Override
        public boolean sixMonthToggle() {
            return false;
        }
    }
}
