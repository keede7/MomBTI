package io.keede.mombti.core.domains.report.model;

/** 개선할 부분의 종류. 선언 순서가 화면 순서다 (데이터 기반이 먼저). */
public enum ImprovementKind {
    /** 입력값과 계산 결과에서 나온 개선점. */
    PERSONAL,
    /** 입력이 없어 일반적인 팁으로 안내하는 항목 (docs/PRD.md 리포트 구성 10). */
    GENERAL_TIP
}
