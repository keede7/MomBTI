package io.keede.mombti.web.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 페르소나 3개 리포트 화면 E2E (docs/PRD.md 리포트 구성 1~11, ADR-014). */
@SpringBootTest
@AutoConfigureMockMvc
class ReportPageE2ETest {

    /** docs/PRD.md 리포트 구성 1~11 순서. */
    private static final List<String> SECTION_IDS = List.of(
            "id=\"topbar\"", "id=\"summary\"", "id=\"chart\"", "id=\"milestones\"", "id=\"eta\"",
            "id=\"diagnosis\"", "id=\"meal-plan\"", "id=\"expectation\"", "id=\"refine\"",
            "id=\"improvements\"", "id=\"evidence\"");

    @Autowired
    private MockMvc mockMvc;

    static Stream<Arguments> personas() {
        return Stream.of(
                Arguments.of("① 처음 다이어트(필수 4개만)",
                        Map.of("gender", "FEMALE", "age", "27", "heightCm", "163", "weightKg", "68")),
                Arguments.of("② 정체기 운동인(+체지방률)",
                        Map.of("gender", "MALE", "age", "32", "heightCm", "176", "weightKg", "80",
                                "bodyFatPercent", "18")),
                Arguments.of("③ 바쁜 직장인(+활동량·목표)",
                        Map.of("gender", "MALE", "age", "41", "heightCm", "172", "weightKg", "85",
                                "activityLevel", "LIGHT", "targetWeightKg", "75")));
    }

    private String render(Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder request = post("/report");
        params.forEach(request::param);
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("report"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("personas")
    void 페르소나_리포트에_11개_섹션이_순서대로_있고_null_NA가_없음(String persona, Map<String, String> params)
            throws Exception {
        String html = render(params);

        int previous = -1;
        for (String id : SECTION_IDS) {
            int index = html.indexOf(id);
            assertThat(index).as(id).isGreaterThan(previous);
            previous = index;
        }
        assertThat(html).doesNotContain("null").doesNotContain("N/A");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("personas")
    void 차트_표는_JS_없이_서버가_렌더(String persona, Map<String, String> params) throws Exception {
        String html = render(params);

        assertThat(html).contains("<table id=\"chart-table\"");
        // 0~26주 2주 간격 14행 (기본 12주 밖 행도 표에는 모두 있다)
        assertThat(html.split("<tr class=\"chart-row").length - 1).isEqualTo(14);
        assertThat(html).contains("data-chart=");
        assertThat(html).contains("https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js");
    }

    @Test
    void E01_필수_4개만이면_추정_태그와_기본_목표_라벨() throws Exception {
        String html = render(Map.of("gender", "FEMALE", "age", "27", "heightCm", "163", "weightKg", "68"));

        assertThat(html).contains("class=\"tag tag-estimated\"").contains("기본 목표(건강 체중)로 계산했어요");
        assertThat(html).contains("class=\"tag tag-assumed\"");
        assertThat(html).contains("href=\"#ref1\"").contains("id=\"ref1\"");
    }

    @Test
    void E15_유지_모드도_모든_섹션이_빈칸_없이_렌더() throws Exception {
        String html = render(Map.of("gender", "FEMALE", "age", "30", "heightCm", "165", "weightKg", "55"));

        SECTION_IDS.forEach(id -> assertThat(html).contains(id));
        assertThat(html).doesNotContain("null").doesNotContain("N/A");
    }

    @Test
    void E11_저체중도_모든_섹션이_빈칸_없이_렌더() throws Exception {
        String html = render(Map.of("gender", "FEMALE", "age", "30", "heightCm", "165", "weightKg", "48"));

        SECTION_IDS.forEach(id -> assertThat(html).contains(id));
        assertThat(html).doesNotContain("null").doesNotContain("N/A");
    }

    @Test
    void E20_6개월_안에_미도달이면_6개월_토글과_중간_목표_안내() throws Exception {
        String html = render(Map.of("gender", "MALE", "age", "40", "heightCm", "170", "weightKg", "120",
                "targetWeightKg", "65"));

        assertThat(html).contains("id=\"toggle-six-months\"").contains("6개월 안에는 어려워요");
    }

    @Test
    void E18_12주_안에_도달하면_6개월_토글_없음() throws Exception {
        String html = render(Map.of("gender", "MALE", "age", "30", "heightCm", "175", "weightKg", "80",
                "targetWeightKg", "76"));

        assertThat(html).doesNotContain("id=\"toggle-six-months\"").contains("목표 도달");
    }

    @Test
    void 입력_화면은_필수_4개와_선택_3개_인라인_안내() throws Exception {
        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("더 맞춤형으로").contains("id=\"optional-hint\"").contains("/js/form.js");
        assertThat(html.split("class=\"tag tag-optional\"").length - 1).isEqualTo(3);
        assertThat(html).doesNotContain("null");
    }

    @Test
    void 템플릿에_th_utext가_없음() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/templates"))) {
            List<Path> offenders = files.filter(Files::isRegularFile)
                    .filter(file -> read(file).contains("th:utext"))
                    .toList();
            assertThat(offenders).isEmpty();
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
