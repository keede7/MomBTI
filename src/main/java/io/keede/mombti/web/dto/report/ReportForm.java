package io.keede.mombti.web.dto.report;

import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.validation.Errors;

/**
 * 입력 폼 (필수 4개 + 선택 3개). 리포트 화면에서는 hidden 필드로 왕복한다 (E-26: 매 요청 전체 재검증).
 * 나이 19세 미만은 오류가 아니라 미성년 안내 화면이다 (E-04). BMI(E-09)와 성별별 체지방률 하한(E-23)은 도메인이 검사한다.
 */
public class ReportForm {

    static final BigDecimal MIN_HEIGHT_CM = new BigDecimal("120.0");
    static final BigDecimal MAX_HEIGHT_CM = new BigDecimal("230.0");
    /** 이 값보다 작으면 m로 입력한 것으로 본다 (E-07). */
    static final BigDecimal METER_INPUT_LIMIT = new BigDecimal("3");

    @NotNull(message = "{E-03.gender}")
    private Gender gender;

    @NotNull(message = "{E-03.age}")
    @Min(value = 1, message = "{E-08.age}")
    @Max(value = 80, message = "{E-05}")
    private Integer age;

    /** 범위와 소수 자리는 m 입력(E-07)과 범위 밖(E-08)의 문구가 달라서 {@link #validateHeight(Errors)}가 검사한다. */
    @NotNull(message = "{E-03.heightCm}")
    private BigDecimal heightCm;

    @NotNull(message = "{E-03.weightKg}")
    @DecimalMin(value = "30.0", message = "{E-08.weightKg}")
    @DecimalMax(value = "300.0", message = "{E-08.weightKg}")
    @Digits(integer = 3, fraction = 1, message = "{E-08.decimal}")
    private BigDecimal weightKg;

    private ActivityLevel activityLevel;

    @DecimalMin(value = "30.0", message = "{E-08.targetWeightKg}")
    @DecimalMax(value = "300.0", message = "{E-08.targetWeightKg}")
    @Digits(integer = 3, fraction = 1, message = "{E-08.decimal}")
    private BigDecimal targetWeightKg;

    @DecimalMin(value = "5.0", message = "{E-23}")
    @DecimalMax(value = "60.0", message = "{E-23}")
    @Digits(integer = 2, fraction = 1, message = "{E-08.decimal}")
    private BigDecimal bodyFatPercent;

    /** 키 3 미만은 "cm로 입력" 안내(E-07), 그 밖의 범위 밖은 범위와 예시(E-08), 소수 1자리까지. */
    public void validateHeight(Errors errors) {
        if (heightCm == null || errors.hasFieldErrors("heightCm")) {
            return;
        }
        if (heightCm.compareTo(METER_INPUT_LIMIT) < 0) {
            errors.rejectValue("heightCm", "E-07");
        } else if (heightCm.compareTo(MIN_HEIGHT_CM) < 0 || heightCm.compareTo(MAX_HEIGHT_CM) > 0) {
            errors.rejectValue("heightCm", "E-08");
        } else if (heightCm.stripTrailingZeros().scale() > 1) {
            errors.rejectValue("heightCm", "E-08.decimal");
        }
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    public BigDecimal getTargetWeightKg() {
        return targetWeightKg;
    }

    public void setTargetWeightKg(BigDecimal targetWeightKg) {
        this.targetWeightKg = targetWeightKg;
    }

    public BigDecimal getBodyFatPercent() {
        return bodyFatPercent;
    }

    public void setBodyFatPercent(BigDecimal bodyFatPercent) {
        this.bodyFatPercent = bodyFatPercent;
    }
}
