package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** 필수 4개 + 선택 3개 입력. 선택값은 {@code ProfileDefaults}가 확정한다. */
public record BodyProfile(
        Gender gender,
        int age,
        BigDecimal heightCm,
        BigDecimal weightKg,
        Optional<ActivityLevel> activityLevel,
        Optional<BigDecimal> targetWeightKg,
        Optional<BigDecimal> bodyFatPercent
) {
    public BodyProfile {
        Objects.requireNonNull(gender, "gender");
        Objects.requireNonNull(heightCm, "heightCm");
        Objects.requireNonNull(weightKg, "weightKg");
        Objects.requireNonNull(activityLevel, "activityLevel");
        Objects.requireNonNull(targetWeightKg, "targetWeightKg");
        Objects.requireNonNull(bodyFatPercent, "bodyFatPercent");
    }
}
