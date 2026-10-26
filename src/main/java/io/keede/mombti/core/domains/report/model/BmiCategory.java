package io.keede.mombti.core.domains.report.model;

/**
 * KSSO_2022 BMI 구간. MVP 화면은 ≥30을 한 구간으로 묶는다.
 * 라벨은 중립 표현 (docs/UI_GUIDE.md "범위 막대").
 */
public enum BmiCategory {
    UNDERWEIGHT("저체중"),
    NORMAL("정상"),
    PRE_OBESE("관리 권장"),
    OBESE_1("관리 필요"),
    OBESE_2_PLUS("적극 관리");

    private final String label;

    BmiCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
