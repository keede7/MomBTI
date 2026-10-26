package io.keede.mombti.web.dto.report;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.fixture.ReportFixture;
import io.keede.mombti.core.domains.report.model.Report;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReportViewTest {

    @ParameterizedTest
    @CsvSource({
            "2027-03-01,2027년 3월 첫째 주",
            "2027-03-08,2027년 3월 둘째 주",
            "2027-03-21,2027년 3월 셋째 주",
            "2027-03-28,2027년 3월 넷째 주",
            "2027-03-29,2027년 3월 다섯째 주",
    })
    void 날짜는_N월_N째_주로_표기(LocalDate date, String expected) {
        assertThat(ReportView.formatWeekOfMonth(date)).isEqualTo(expected);
    }

    @Test
    void 필수_4개만이면_기본_분석_배지와_추정_표시() {
        ReportView view = ReportView.from((Report) ReportFixture.service().generate(ReportFixture.firstTimer()));

        assertThat(view.badge()).isEqualTo("기본 분석");
        assertThat(view.summary().tdeeEstimated()).isTrue();
        assertThat(view.status().bodyFatEstimated()).isTrue();
        assertThat(view).hasNoNullFieldsOrProperties();
        assertThat(view.summary()).hasNoNullFieldsOrProperties();
        assertThat(view.status()).hasNoNullFieldsOrProperties();
    }

    @Test
    void kcal은_천_단위_구분과_10kcal_단위() {
        ReportView view = ReportView.from((Report) ReportFixture.service().generate(ReportFixture.busyWorker()));

        assertThat(view.badge()).isEqualTo("맞춤 분석 +2");
        assertThat(view.summary().intakeKcal()).matches("\\d{1,2},\\d{2}0");
        assertThat(view.status().bmrKcal()).matches("\\d{1,2},\\d{2}0");
        assertThat(view.etaRows()).hasSize(4);
        assertThat(view.etaRows().getFirst().scenario()).isEqualTo("식단만");
    }
}
