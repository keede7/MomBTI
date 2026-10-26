package io.keede.mombti.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import io.keede.mombti.core.config.ClockConfig;
import io.keede.mombti.core.config.DomainBeanConfig;
import io.keede.mombti.web.dto.report.ReportView;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ReportController.class)
@Import({DomainBeanConfig.class, ClockConfig.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 남 30세 175cm 80kg (BMI 26.1, 감량 모드). */
    private static MockHttpServletRequestBuilder report(Map<String, String> overrides) {
        return form(post("/report"), Map.of(
                "gender", "MALE", "age", "30", "heightCm", "175", "weightKg", "80"), overrides);
    }

    private static MockHttpServletRequestBuilder refine(Map<String, String> overrides) {
        return form(post("/report/refine"), Map.of(
                "gender", "MALE", "age", "30", "heightCm", "175.0", "weightKg", "80.0"), overrides);
    }

    private static MockHttpServletRequestBuilder form(MockHttpServletRequestBuilder builder,
                                                      Map<String, String> base, Map<String, String> overrides) {
        base.forEach((name, value) -> {
            if (!overrides.containsKey(name)) builder.param(name, value);
        });
        overrides.forEach(builder::param);
        return builder;
    }

    @Test
    void 입력_화면() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("reportForm"));
    }

    @Test
    void 필수_4개만_입력하면_리포트() throws Exception {
        mockMvc.perform(report(Map.of()))
                .andExpect(status().isOk())
                .andExpect(view().name("report"))
                .andExpect(model().attribute("report", instanceOf(ReportView.class)))
                .andExpect(model().attributeExists("chartJson"))
                .andExpect(content().string(not(containsString("null"))))
                .andExpect(content().string(not(containsString("N/A"))));
    }

    @Test
    void E04_미성년자는_안내_화면() throws Exception {
        mockMvc.perform(report(Map.of("age", "17")))
                .andExpect(status().isOk())
                .andExpect(view().name("minor"))
                .andExpect(model().attributeDoesNotExist("report"));
    }

    @Test
    void E03_필수_항목_누락은_입력_화면_400_오류_요약과_입력값_유지() throws Exception {
        mockMvc.perform(report(Map.of("gender", "", "age", "")).param("weightKg", "81.5"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrors("reportForm", "gender", "age"))
                .andExpect(content().string(containsString("성별을 선택해 주세요")))
                .andExpect(content().string(containsString("나이를 입력해 주세요")))
                .andExpect(content().string(containsString("href=\"#gender\"")))
                .andExpect(content().string(containsString("value=\"175\"")));
    }

    @Test
    void E05_80세_초과는_나이_필드_오류() throws Exception {
        mockMvc.perform(report(Map.of("age", "81")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("reportForm", "age"))
                .andExpect(content().string(containsString("80세 이하만 계산할 수 있어요")));
    }

    @Test
    void E07_키를_m로_입력하면_cm_안내_오류() throws Exception {
        mockMvc.perform(report(Map.of("heightCm", "1.75")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "heightCm", "E-07"))
                .andExpect(content().string(containsString("m가 아닌 cm로 입력해 주세요 (예: 175)")))
                .andExpect(content().string(not(containsString("120~230cm"))));
    }

    @Test
    void E08_키_범위_밖은_범위와_예시를_담은_오류() throws Exception {
        mockMvc.perform(report(Map.of("heightCm", "110")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "heightCm", "E-08"))
                .andExpect(content().string(containsString("키는 120~230cm 사이로 입력해 주세요 (예: 175)")));
    }

    @Test
    void E08_몸무게_범위_밖은_범위와_예시를_담은_오류() throws Exception {
        mockMvc.perform(report(Map.of("weightKg", "301")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("reportForm", "weightKg"))
                .andExpect(content().string(containsString("몸무게는 30~300kg 사이로 입력해 주세요 (예: 72.5)")));
    }

    @Test
    void E09_BMI_12_미만이면_키와_몸무게_모두_단위_확인_오류() throws Exception {
        mockMvc.perform(report(Map.of("heightCm", "230", "weightKg", "60")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "heightCm", "E-09"))
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "weightKg", "E-09"))
                .andExpect(content().string(containsString("키(cm)와 몸무게(kg) 단위를 확인해 주세요")));
    }

    @Test
    void E09_BMI_70_초과면_단위_확인_오류() throws Exception {
        mockMvc.perform(report(Map.of("heightCm", "120", "weightKg", "120")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "weightKg", "E-09"));
    }

    @Test
    void E10_쉼표_소수점과_단위_문자는_정규화해서_계산() throws Exception {
        mockMvc.perform(report(Map.of("heightCm", "175cm", "weightKg", "80,5", "age", "30세")))
                .andExpect(status().isOk())
                .andExpect(view().name("report"));
    }

    @Test
    void E10_정규화해도_숫자가_아니면_필드_오류() throws Exception {
        mockMvc.perform(report(Map.of("weightKg", "팔십")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "weightKg", "typeMismatch"))
                .andExpect(content().string(containsString("숫자로 입력해 주세요")));
    }

    @Test
    void E13_목표_BMI_18_5_미만은_최저_체중_안내() throws Exception {
        mockMvc.perform(report(Map.of("targetWeightKg", "50")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "targetWeightKg", "E-13"))
                .andExpect(content().string(containsString("건강 범위 최저 체중은 56.7kg이에요")));
    }

    @Test
    void E23_체지방률이_필수지방_미만이면_필드_오류() throws Exception {
        mockMvc.perform(report(Map.of("gender", "FEMALE", "bodyFatPercent", "10")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "bodyFatPercent", "E-23"))
                .andExpect(content().string(containsString("체지방률은 남 5~60%, 여 12~60% 사이로 입력해 주세요")));
        mockMvc.perform(report(Map.of("bodyFatPercent", "4.9")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("reportForm", "bodyFatPercent"));
    }

    @Test
    void E23_체지방률이_60_초과면_필드_오류() throws Exception {
        mockMvc.perform(report(Map.of("bodyFatPercent", "61")))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("reportForm", "bodyFatPercent"))
                .andExpect(content().string(containsString("체지방률은 남 5~60%, 여 12~60% 사이로 입력해 주세요")));
    }

    @Test
    void E24_체지방률이_BMI_추정과_15퍼센트포인트_이상_차이나면_경고만_하고_리포트() throws Exception {
        // 남 30세 175cm 80kg → 추정 22.0%, 입력 40% → 차이 18%p
        mockMvc.perform(report(Map.of("bodyFatPercent", "40")))
                .andExpect(status().isOk())
                .andExpect(view().name("report"))
                .andExpect(content().string(containsString("키·몸무게로 추정한 값과 많이 달라요")));
    }

    @Test
    void E24_차이가_작으면_경고_없음() throws Exception {
        mockMvc.perform(report(Map.of("bodyFatPercent", "22")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("키·몸무게로 추정한 값과 많이 달라요"))));
    }

    @Test
    void 리포트는_재계산용_hidden_필드로_기존_값을_왕복() throws Exception {
        mockMvc.perform(report(Map.of("targetWeightKg", "72.5")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("type=\"hidden\" name=\"heightCm\" value=\"175\"")))
                .andExpect(content().string(containsString("type=\"hidden\" name=\"targetWeightKg\" value=\"72.5\"")));
    }

    @Test
    void 더_정확하게_보기는_패널_입력을_병합해_재계산() throws Exception {
        mockMvc.perform(refine(Map.of("refineActivityLevel", "ACTIVE", "refineBodyFatPercent", "24,5")))
                .andExpect(status().isOk())
                .andExpect(view().name("report"))
                .andExpect(model().attribute("report", instanceOf(ReportView.class)))
                .andExpect(content().string(containsString("맞춤 분석 +2")))
                .andExpect(content().string(containsString("type=\"hidden\" name=\"activityLevel\" value=\"ACTIVE\"")))
                .andExpect(content().string(containsString("type=\"hidden\" name=\"bodyFatPercent\" value=\"24.5\"")));
    }

    @Test
    void E25_더_정확하게_보기_입력_오류는_패널_안_오류와_기존_리포트_유지() throws Exception {
        mockMvc.perform(refine(Map.of("refineBodyFatPercent", "70")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("report"))
                .andExpect(model().attribute("report", instanceOf(ReportView.class)))
                .andExpect(model().attributeHasFieldErrors("refineForm", "refineBodyFatPercent"))
                .andExpect(model().attributeHasNoErrors("reportForm"))
                .andExpect(content().string(containsString("기본 분석")))
                .andExpect(content().string(containsString("체지방률은 남 5~60%, 여 12~60% 사이로 입력해 주세요")));
    }

    @Test
    void E25_패널의_목표_체중이_도메인_검증에_걸려도_패널_안_오류() throws Exception {
        mockMvc.perform(refine(Map.of("refineTargetWeightKg", "50")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("report"))
                .andExpect(model().attributeHasFieldErrorCode("refineForm", "refineTargetWeightKg", "E-13"))
                .andExpect(content().string(containsString("건강 범위 최저 체중은 56.7kg이에요")));
    }

    @Test
    void E25_패널의_숫자가_아닌_값은_패널_안_오류() throws Exception {
        mockMvc.perform(refine(Map.of("refineTargetWeightKg", "칠십")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("report"))
                .andExpect(model().attributeHasFieldErrorCode("refineForm", "refineTargetWeightKg", "typeMismatch"));
    }

    @Test
    void E26_hidden_필드_변조는_전체_재검증해서_일반_검증_오류와_같게_처리() throws Exception {
        mockMvc.perform(refine(Map.of("heightCm", "1.75", "refineActivityLevel", "ACTIVE")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "heightCm", "E-07"));
        mockMvc.perform(refine(Map.of("gender", "OTHER")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrors("reportForm", "gender"));
        mockMvc.perform(refine(Map.of("weightKg", "")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrors("reportForm", "weightKg"));
    }

    @Test
    void E26_hidden_필드의_나이를_미성년으로_바꾸면_미성년_안내() throws Exception {
        mockMvc.perform(refine(Map.of("age", "17")))
                .andExpect(status().isOk())
                .andExpect(view().name("minor"));
    }

    @Test
    void E26_hidden_필드의_도메인_검증_오류도_입력_화면() throws Exception {
        mockMvc.perform(refine(Map.of("targetWeightKg", "50")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrorCode("reportForm", "targetWeightKg", "E-13"));
    }

    @Test
    void E27_GET_report_직접_접근은_홈으로_리다이렉트() throws Exception {
        mockMvc.perform(get("/report"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void E28_없는_URL은_4xx_페이지() throws Exception {
        mockMvc.perform(get("/no-such-page"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/4xx"))
                .andExpect(content().string(containsString("홈으로 가기")));
    }

    @Test
    void E28_허용되지_않은_메서드는_4xx_페이지() throws Exception {
        mockMvc.perform(put("/report"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(view().name("error/4xx"))
                .andExpect(content().string(containsString("홈으로 가기")));
    }

    @Test
    void 응답에_CSP_헤더와_requestId() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().string("Content-Security-Policy", containsString(
                        "script-src 'self' https://cdnjs.cloudflare.com")))
                .andExpect(header().string("Content-Security-Policy", containsString(
                        "style-src 'self' https://fonts.googleapis.com")))
                .andExpect(header().string("Content-Security-Policy", containsString(
                        "font-src 'self' https://fonts.gstatic.com")))
                .andExpect(header().exists("X-Request-Id"));
    }
}
