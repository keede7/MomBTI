package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.model.Narration;

/**
 * 계산된 결과를 문장으로 옮기는 확장점 (ADR-002). 숫자를 만들거나 바꾸지 않는다.
 * 2차 LLM 구현체도 이 계약을 지킨다 (docs/REPORT_STRATEGY.md).
 */
public interface AdviceNarrator {

    Narration narrate(AdviceContext context);
}
