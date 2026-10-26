package io.keede.mombti.core.domains.report.model;

import java.util.List;
import java.util.Objects;

/**
 * {@code AdviceNarrator}가 만든 문장. 숫자는 계산된 값을 그대로 옮긴 것뿐이다.
 *
 * @param headline           리포트 상단 한 줄 요약
 * @param positiveEmptyState 모든 지표가 건강 범위이고 목표가 없음 → "지금 잘 관리하고 있어요" (E-30)
 * @param notices            미리 알아둘 것
 */
public record Narration(String headline, boolean positiveEmptyState, List<Notice> notices, List<Faq> faqs) {

    public Narration {
        Objects.requireNonNull(headline, "headline");
        notices = List.copyOf(notices);
        faqs = List.copyOf(faqs);
    }
}
