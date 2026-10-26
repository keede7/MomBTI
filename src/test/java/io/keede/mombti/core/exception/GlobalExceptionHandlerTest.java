package io.keede.mombti.core.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import io.keede.mombti.core.domains.report.exception.SimulationInvariantException;
import io.keede.mombti.core.domains.report.service.ReportQueryService;
import io.keede.mombti.web.controller.ReportController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ReportController.class)
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportQueryService reportQueryService;

    private MvcResult postReport() throws Exception {
        return mockMvc.perform(post("/report")
                        .param("gender", "FEMALE")
                        .param("age", "37")
                        .param("heightCm", "163.4")
                        .param("weightKg", "87.3")
                        .param("targetWeightKg", "61.9")
                        .param("bodyFatPercent", "33.3"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/5xx"))
                .andExpect(model().attributeExists("requestId"))
                .andReturn();
    }

    @Test
    void E29_시뮬레이션_불변식_위반은_5xx_페이지와_requestId(CapturedOutput output) throws Exception {
        given(reportQueryService.generate(any())).willThrow(new SimulationInvariantException("FAT_MASS_MIN", 120));

        MvcResult result = postReport();

        String requestId = (String) result.getModelAndView().getModel().get("requestId");
        assertThat(result.getResponse().getContentAsString()).contains(requestId);
        assertThat(output).contains("requestId=" + requestId)
                .contains(SimulationInvariantException.class.getName());
    }

    @Test
    void E29_에러_로그에는_requestId와_예외_타입만_남고_입력값은_남지_않음(CapturedOutput output) throws Exception {
        // 예외 메시지에 입력값이 섞여 들어와도 로그에는 남기지 않는다
        given(reportQueryService.generate(any())).willThrow(new IllegalStateException("weight=87.3 height=163.4"));

        MvcResult result = postReport();

        String requestId = (String) result.getModelAndView().getModel().get("requestId");
        assertThat(output).contains("requestId=" + requestId).contains(IllegalStateException.class.getName());
        assertThat(output.getAll()).doesNotContain("87.3", "163.4", "61.9", "33.3", "weight=", "FEMALE");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("87.3", "weight=");
    }

    @Test
    void E29_5xx_페이지는_스택트레이스를_노출하지_않음() throws Exception {
        given(reportQueryService.generate(any())).willThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/report")
                        .param("gender", "MALE").param("age", "30").param("heightCm", "175").param("weightKg", "80"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("boom"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("at io.keede"))))
                .andExpect(content().string(containsString("홈으로 가기")));
    }
}
