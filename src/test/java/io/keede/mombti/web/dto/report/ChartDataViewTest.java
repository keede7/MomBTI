package io.keede.mombti.web.dto.report;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.fixture.ReportFixture;
import io.keede.mombti.core.domains.report.model.Report;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ChartDataViewTest {

    private final Report report = (Report) ReportFixture.service().generate(ReportFixture.busyWorker());

    @Test
    void 차트_데이터는_0주부터_26주까지_2주_간격() {
        ChartDataView chart = ChartDataView.from(report);

        assertThat(chart.weeks()).containsExactlyElementsOf(IntStream.rangeClosed(0, 13).map(i -> i * 2).boxed().toList());
        assertThat(chart.dates()).hasSize(14).first().isEqualTo("2026년 10월 넷째 주");
        assertThat(chart.series()).hasSize(4).allSatisfy(series -> {
            assertThat(series.weightsKg()).hasSize(14);
            assertThat(series.adherenceWeightsKg()).hasSize(14);
        });
        assertThat(chart.bandUpperKg()).hasSize(14);
        assertThat(chart.bandLowerKg()).hasSize(14);
        assertThat(chart.defaultWeeks()).isEqualTo(12);
        assertThat(chart.extendedWeeks()).isEqualTo(26);
    }

    @Test
    void 체중은_0_1kg_단위이고_시작_체중에서_출발() {
        ChartDataView chart = ChartDataView.from(report);

        assertThat(chart.series().getFirst().weightsKg().getFirst()).isEqualByComparingTo("85.0");
        assertThat(chart.series()).allSatisfy(series ->
                assertThat(series.weightsKg()).allSatisfy(weight -> assertThat(weight.scale()).isEqualTo(1)));
        assertThat(chart.targetKg()).isEqualByComparingTo("75.0");
        assertThat(chart.showTarget()).isTrue();
    }

    @Test
    void 개인차_밴드는_식단만_라인을_감싼다() {
        ChartDataView chart = ChartDataView.from(report);

        for (int i = 0; i < chart.weeks().size(); i++) {
            assertThat(chart.bandLowerKg().get(i)).isLessThanOrEqualTo(chart.series().getFirst().weightsKg().get(i));
            assertThat(chart.bandUpperKg().get(i)).isGreaterThanOrEqualTo(chart.series().getFirst().weightsKg().get(i));
        }
    }

    @Test
    void JSON으로_직렬화하면_null이_없음() {
        String json = JsonMapper.builder().build().writeValueAsString(ChartDataView.from(report));

        assertThat(json).contains("\"weeks\":[0,2,4,6,8,10,12,14,16,18,20,22,24,26]").doesNotContain("null");
    }
}
