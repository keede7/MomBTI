package io.keede.mombti.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.keede.mombti.core.domains.report.advice.AdviceNarrator;
import io.keede.mombti.core.domains.report.advice.TemplateAdviceNarrator;
import io.keede.mombti.core.domains.report.fixture.ReportFixture;
import io.keede.mombti.core.domains.report.model.Report;
import io.keede.mombti.core.domains.report.service.ReportQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class DomainBeanConfigTest {

    @Test
    void 순수_도메인_클래스가_Bean으로_등록되어_리포트를_만든다() {
        try (var context = new AnnotationConfigApplicationContext(ClockConfig.class, DomainBeanConfig.class)) {
            ReportQueryService service = context.getBean(ReportQueryService.class);

            assertThat(context.getBean(AdviceNarrator.class)).isInstanceOf(TemplateAdviceNarrator.class);
            assertThat(service.generate(ReportFixture.firstTimer())).isInstanceOf(Report.class);
        }
    }
}
