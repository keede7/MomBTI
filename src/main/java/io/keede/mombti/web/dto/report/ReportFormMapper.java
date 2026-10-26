package io.keede.mombti.web.dto.report;

import io.keede.mombti.core.domains.report.model.BodyProfile;
import java.util.Optional;

/** 폼 → 도메인 입력. 값을 옮기기만 하고 계산하지 않는다. */
public final class ReportFormMapper {

    private ReportFormMapper() {
    }

    /** 검증을 통과한 폼만 넘긴다 (필수 4개 non-null). */
    public static BodyProfile toProfile(ReportForm form) {
        return new BodyProfile(form.getGender(), form.getAge(), form.getHeightCm(), form.getWeightKg(),
                Optional.ofNullable(form.getActivityLevel()),
                Optional.ofNullable(form.getTargetWeightKg()),
                Optional.ofNullable(form.getBodyFatPercent()));
    }

    /** hidden 기존 값 + 패널 입력. 패널에 입력한 항목만 바꾼다. */
    public static ReportForm merge(ReportForm base, RefineForm refine) {
        ReportForm merged = new ReportForm();
        merged.setGender(base.getGender());
        merged.setAge(base.getAge());
        merged.setHeightCm(base.getHeightCm());
        merged.setWeightKg(base.getWeightKg());
        merged.setActivityLevel(Optional.ofNullable(refine.getRefineActivityLevel()).orElse(base.getActivityLevel()));
        merged.setTargetWeightKg(Optional.ofNullable(refine.getRefineTargetWeightKg()).orElse(base.getTargetWeightKg()));
        merged.setBodyFatPercent(Optional.ofNullable(refine.getRefineBodyFatPercent()).orElse(base.getBodyFatPercent()));
        return merged;
    }
}
