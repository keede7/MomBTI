package io.keede.mombti.core.domains.report.advice;

import io.keede.mombti.core.domains.report.model.Improvement;

/** 개선할 부분 규칙 하나. 규칙 하나가 클래스 하나다. */
public interface ImprovementRule {

    boolean applies(AdviceContext context);

    Improvement build(AdviceContext context);
}
