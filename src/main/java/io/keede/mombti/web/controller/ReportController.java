package io.keede.mombti.web.controller;

import io.keede.mombti.core.domains.report.exception.DomainValidationException;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Report;
import io.keede.mombti.core.domains.report.model.ReportResult;
import io.keede.mombti.core.domains.report.service.ReportQueryService;
import io.keede.mombti.web.dto.report.ChartDataView;
import io.keede.mombti.web.dto.report.RefineForm;
import io.keede.mombti.web.dto.report.ReportForm;
import io.keede.mombti.web.dto.report.ReportFormMapper;
import io.keede.mombti.web.dto.report.ReportView;
import io.keede.mombti.web.support.NumberNormalizer;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.json.JsonMapper;

/**
 * 입력 → 리포트 (docs/ARCHITECTURE.md 데이터 흐름). 저장하지 않으므로 POST 결과를 바로 렌더한다 (ADR-017).
 * 계산은 {@link ReportQueryService}에만 맡긴다.
 */
@Controller
public class ReportController {

    static final String INDEX = "index";
    static final String REPORT = "report";
    static final String MINOR = "minor";

    /** E-09는 키와 몸무게 중 어느 쪽이 틀렸는지 모르므로 두 필드에 모두 표시한다. */
    private static final String BMI_UNIT_CODE = "E-09";
    private static final List<String> BMI_UNIT_FIELDS = List.of("heightCm", "weightKg");

    private final ReportQueryService reportQueryService;
    private final JsonMapper jsonMapper;

    public ReportController(ReportQueryService reportQueryService, JsonMapper jsonMapper) {
        this.reportQueryService = reportQueryService;
        this.jsonMapper = jsonMapper;
    }

    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, NumberNormalizer.bigDecimalEditor());
        binder.registerCustomEditor(Integer.class, NumberNormalizer.integerEditor());
    }

    /** 활동량 5단계 선택지 (입력 화면, 더 정확하게 보기). */
    @ModelAttribute("activityLevels")
    public ActivityLevel[] activityLevels() {
        return ActivityLevel.values();
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("reportForm", new ReportForm());
        return INDEX;
    }

    /** E-27: 저장된 리포트가 없으므로 입력 화면으로. */
    @GetMapping("/report")
    public String reportRedirect() {
        return "redirect:/";
    }

    @PostMapping("/report")
    public ModelAndView report(@Valid @ModelAttribute("reportForm") ReportForm form, BindingResult errors,
                               Model model) {
        form.validateHeight(errors);
        if (errors.hasErrors()) {
            return badRequest(INDEX);
        }
        Optional<ReportResult> result = generate(form, errors);
        if (result.isEmpty()) {
            return badRequest(INDEX);
        }
        model.addAttribute("refineForm", new RefineForm());
        return render(result.get(), model, HttpStatus.OK);
    }

    /**
     * hidden 기존 값 + 패널 입력을 병합해 재계산한다. hidden 값도 매번 전체 재검증한다 (E-26).
     * 패널 입력 오류는 패널 안에 표시하고 기존 리포트를 다시 렌더한다 (E-25).
     */
    @PostMapping("/report/refine")
    public ModelAndView refine(@Valid @ModelAttribute("reportForm") ReportForm form, BindingResult errors,
                               @Valid @ModelAttribute("refineForm") RefineForm refine, BindingResult refineErrors,
                               Model model) {
        form.validateHeight(errors);
        if (errors.hasErrors()) {
            return badRequest(INDEX);
        }
        if (refineErrors.hasErrors()) {
            return previousReport(form, errors, model);
        }

        ReportForm merged = ReportFormMapper.merge(form, refine);
        ReportResult result;
        try {
            result = reportQueryService.generate(ReportFormMapper.toProfile(merged));
        } catch (DomainValidationException e) {
            Optional<String> panelField = refine.panelFieldOf(e.field());
            if (panelField.isEmpty()) {
                reject(errors, e);
                return badRequest(INDEX);
            }
            refineErrors.rejectValue(panelField.get(), e.code(), e.arguments().toArray(), null);
            return previousReport(form, errors, model);
        }
        model.addAttribute("reportForm", merged);
        model.addAttribute("refineForm", new RefineForm());
        return render(result, model, HttpStatus.OK);
    }

    private ModelAndView previousReport(ReportForm form, BindingResult errors, Model model) {
        Optional<ReportResult> previous = generate(form, errors);
        if (previous.isEmpty()) {
            return badRequest(INDEX);
        }
        return render(previous.get(), model, HttpStatus.BAD_REQUEST);
    }

    /** 도메인 이중 검증 오류는 BindingResult로 바꾼다 (docs/ARCHITECTURE.md 에러 처리). */
    private Optional<ReportResult> generate(ReportForm form, BindingResult errors) {
        try {
            return Optional.of(reportQueryService.generate(ReportFormMapper.toProfile(form)));
        } catch (DomainValidationException e) {
            reject(errors, e);
            return Optional.empty();
        }
    }

    private static void reject(BindingResult errors, DomainValidationException e) {
        List<String> fields = BMI_UNIT_CODE.equals(e.code()) ? BMI_UNIT_FIELDS : List.of(e.field());
        fields.forEach(field -> errors.rejectValue(field, e.code(), e.arguments().toArray(), null));
    }

    private ModelAndView render(ReportResult result, Model model, HttpStatus status) {
        return switch (result) {
            case ReportResult.MinorBlocked minor -> new ModelAndView(MINOR, HttpStatus.OK);
            case Report report -> {
                ChartDataView chart = ChartDataView.from(report);
                model.addAttribute("report", ReportView.from(report));
                // 표(서버 렌더)와 차트(JS)가 같은 데이터를 쓴다 (ADR-014)
                model.addAttribute("chart", chart);
                model.addAttribute("chartJson", jsonMapper.writeValueAsString(chart));
                yield new ModelAndView(REPORT, status);
            }
        };
    }

    private static ModelAndView badRequest(String view) {
        return new ModelAndView(view, HttpStatus.BAD_REQUEST);
    }
}
