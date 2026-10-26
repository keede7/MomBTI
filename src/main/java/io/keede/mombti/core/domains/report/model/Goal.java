package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.util.Objects;

/** 목표 체중. 사용자가 입력했거나, 입력이 없어 건강 체중 기본값을 쓴 경우. */
public sealed interface Goal {

    BigDecimal weightKg();

    record TargetWeight(BigDecimal weightKg) implements Goal {
        public TargetWeight {
            Objects.requireNonNull(weightKg, "weightKg");
        }
    }

    /**
     * 기본 목표. BMI ≥ 23이면 BMI 22.0 체중, 그 밖에는 현재 체중 유지({@code maintain = true}).
     */
    record DefaultHealthyWeight(BigDecimal weightKg, boolean maintain) implements Goal {
        public DefaultHealthyWeight {
            Objects.requireNonNull(weightKg, "weightKg");
        }
    }
}
