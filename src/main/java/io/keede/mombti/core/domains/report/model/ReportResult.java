package io.keede.mombti.core.domains.report.model;

/** 리포트 생성 결과. 미성년자는 감량 수치 없이 안내만 한다 (E-04). */
public sealed interface ReportResult permits Report, ReportResult.MinorBlocked {

    ReportMode mode();

    /** 19세 미만: 감량 수치 없이 일반 건강 정보와 보호자·전문가 상담 안내. */
    record MinorBlocked() implements ReportResult {
        @Override
        public ReportMode mode() {
            return ReportMode.MINOR_BLOCKED;
        }
    }
}
