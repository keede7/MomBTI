package io.keede.mombti.core.domains.report.defaults;

import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Estimated;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.Goal;
import java.math.BigDecimal;

/**
 * 선택 항목까지 모두 확정된 프로필. 이후 계층은 null과 Optional을 다루지 않는다.
 *
 * @param palBandUpper 개인차 밴드 상단 PAL. 활동량 미입력이면 1.60, 입력하면 해당 PAL
 */
public record ResolvedProfile(
        Gender gender,
        int age,
        BigDecimal heightCm,
        BigDecimal weightKg,
        Estimated<ActivityLevel> activityLevel,
        BigDecimal palBandUpper,
        Estimated<Goal> goal,
        Estimated<BigDecimal> bodyFatPercent
) {

    public BigDecimal pal() {
        return activityLevel.value().pal();
    }

    /** 사용자가 직접 입력한 선택 항목 수 ("맞춤 분석 +N"). */
    public int providedOptionalCount() {
        int count = 0;
        if (!activityLevel.estimated()) count++;
        if (!goal.estimated()) count++;
        if (!bodyFatPercent.estimated()) count++;
        return count;
    }
}
