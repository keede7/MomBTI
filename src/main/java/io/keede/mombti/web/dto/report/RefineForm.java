package io.keede.mombti.web.dto.report;

import io.keede.mombti.core.domains.report.model.ActivityLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * "더 정확하게 보기" 패널 입력 (FR-08). 기존 값(hidden, {@link ReportForm})과 이름이 겹치지 않게 refine 접두사를 쓴다.
 * 패널 오류는 패널 안에 표시하고 기존 리포트를 유지한다 (E-25).
 */
public class RefineForm {

    private ActivityLevel refineActivityLevel;

    @DecimalMin(value = "30.0", message = "{E-08.targetWeightKg}")
    @DecimalMax(value = "300.0", message = "{E-08.targetWeightKg}")
    @Digits(integer = 3, fraction = 1, message = "{E-08.decimal}")
    private BigDecimal refineTargetWeightKg;

    @DecimalMin(value = "5.0", message = "{E-23}")
    @DecimalMax(value = "60.0", message = "{E-23}")
    @Digits(integer = 2, fraction = 1, message = "{E-08.decimal}")
    private BigDecimal refineBodyFatPercent;

    /** 도메인 검증 오류의 필드가 패널에서 입력한 값이면 패널 필드 이름을, 아니면 비어 있음을 돌려준다. */
    public Optional<String> panelFieldOf(String field) {
        return switch (field) {
            case "activityLevel" -> refineActivityLevel != null
                    ? Optional.of("refineActivityLevel") : Optional.empty();
            case "targetWeightKg" -> refineTargetWeightKg != null
                    ? Optional.of("refineTargetWeightKg") : Optional.empty();
            case "bodyFatPercent" -> refineBodyFatPercent != null
                    ? Optional.of("refineBodyFatPercent") : Optional.empty();
            default -> Optional.empty();
        };
    }

    public ActivityLevel getRefineActivityLevel() {
        return refineActivityLevel;
    }

    public void setRefineActivityLevel(ActivityLevel refineActivityLevel) {
        this.refineActivityLevel = refineActivityLevel;
    }

    public BigDecimal getRefineTargetWeightKg() {
        return refineTargetWeightKg;
    }

    public void setRefineTargetWeightKg(BigDecimal refineTargetWeightKg) {
        this.refineTargetWeightKg = refineTargetWeightKg;
    }

    public BigDecimal getRefineBodyFatPercent() {
        return refineBodyFatPercent;
    }

    public void setRefineBodyFatPercent(BigDecimal refineBodyFatPercent) {
        this.refineBodyFatPercent = refineBodyFatPercent;
    }
}
