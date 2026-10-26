package io.keede.mombti.web.dto.report;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.CurrentStatus;
import io.keede.mombti.core.domains.report.model.Faq;
import io.keede.mombti.core.domains.report.model.GoalEta;
import io.keede.mombti.core.domains.report.model.GoalEtaRange;
import io.keede.mombti.core.domains.report.model.Improvement;
import io.keede.mombti.core.domains.report.model.MacroPlan;
import io.keede.mombti.core.domains.report.model.MealPlan;
import io.keede.mombti.core.domains.report.model.Milestone;
import io.keede.mombti.core.domains.report.model.MilestoneType;
import io.keede.mombti.core.domains.report.model.Notice;
import io.keede.mombti.core.domains.report.model.Report;
import io.keede.mombti.core.domains.report.model.Scenario;
import io.keede.mombti.core.domains.report.model.ScenarioEta;
import io.keede.mombti.core.domains.report.model.SummaryCards;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 리포트 화면용 값. 도메인이 계산한 값을 표시 단위(kcal 10 단위, 체중 0.1kg, "2027년 3월 둘째 주")로 바꾸기만 한다.
 * 모든 문자열은 비어 있지 않다 (E-01: null, "N/A" 출력 금지).
 *
 * @param dietOnlyUnreachable  식단만 기준 52주 안에 목표에 닿지 않음 (E-20: 52주 예상 체중과 평형 체중 표시)
 * @param footnotes            Evidence key → 각주 번호 (근거 목록 #refN 앵커)
 * @param refineActivityLevel  "더 정확하게 보기"에 활동량 입력칸을 보여 줄지 (미입력이라 추정한 경우)
 */
public record ReportView(
        String mode,
        String headline,
        boolean positiveEmptyState,
        String badge,
        String startDate,
        String goalWeightKg,
        boolean defaultGoal,
        boolean floorApplied,
        boolean dietOnlyMaintains,
        boolean dietOnlyUnreachable,
        String week52WeightKg,
        String equilibriumWeightKg,
        Summary summary,
        Status status,
        List<EtaRow> etaRows,
        List<MilestoneRow> milestones,
        MacroPlan macroPlan,
        MealPlan mealPlan,
        List<Notice> notices,
        List<Faq> faqs,
        List<Improvement> improvements,
        List<EvidenceRow> evidence,
        Map<String, Integer> footnotes,
        boolean refineActivityLevel,
        boolean refineTargetWeightKg,
        boolean refineBodyFatPercent
) {

    private static final String[] WEEK_OF_MONTH = {"첫째", "둘째", "셋째", "넷째", "다섯째"};
    private static final BigDecimal KCAL_STEP = BigDecimal.TEN;
    private static final String MINUS = "−";

    /** 요약 카드 4개. 예측은 식단만 기준. */
    public record Summary(String etaRange, String etaDate, String intakeKcal, String tdeeKcal, boolean tdeeEstimated,
                          String deficitKcal, String weightAt12WeeksKg, String changeAt12WeeksKg,
                          String weeklyLossKg, boolean weeklyLossSafe) {
    }

    /** 현재 상태. */
    public record Status(String bmi, String bmiCategory, String bodyFatPercent, boolean bodyFatEstimated,
                         String fatMassKg, boolean bodyFatLargeError, boolean bodyFatMismatch,
                         String bmrKcal, boolean bmrEstimated, String tdeeKcal, boolean tdeeEstimated,
                         String tdeeLowerKcal, String tdeeUpperKcal) {
    }

    /** 목표 도달 예측 표의 한 행. */
    public record EtaRow(String scenario, String etaRange, String etaDate, String weeklyLossKg, boolean safe) {
    }

    /** 단계별 목표 한 행. */
    public record MilestoneRow(String label, String weightKg, String eta, String date) {
    }

    /** 근거 목록 한 줄. {@code number}는 본문 각주 번호. */
    public record EvidenceRow(int number, String key, String authors, int year, String title, String identifier,
                              boolean assumption) {
    }

    public static ReportView from(Report report) {
        SummaryCards summary = report.summary();
        CurrentStatus status = report.currentStatus();
        int provided = report.providedOptionalCount();
        List<EvidenceRow> evidence = evidenceRows(report.evidence());
        GoalEta dietOnlyEta = summary.dietOnlyEta().expected();
        return new ReportView(
                report.mode().name(),
                report.narration().headline(),
                report.narration().positiveEmptyState(),
                provided == 0 ? "기본 분석" : "맞춤 분석 +" + provided,
                formatWeekOfMonth(report.startDate()),
                kg(report.profile().goal().value().weightKg()),
                report.profile().goal().estimated(),
                report.energyTarget().floorApplied(),
                report.energyTarget().dietOnlyMaintains(),
                dietOnlyEta instanceof GoalEta.Unreachable,
                dietOnlyEta instanceof GoalEta.Unreachable u ? kg(u.week52WeightKg()) : "",
                dietOnlyEta instanceof GoalEta.Unreachable u ? kg(u.equilibriumWeightKg()) : "",
                new Summary(
                        etaRange(summary.dietOnlyEta()),
                        etaDate(report, summary.dietOnlyEta()),
                        kcal(summary.intakeKcal()),
                        kcal(summary.tdeeKcal().value()),
                        summary.tdeeKcal().estimated(),
                        kcal(summary.deficitKcal()),
                        kg(summary.weightAt12WeeksKg()),
                        signedKg(summary.changeAt12WeeksKg()),
                        kg(summary.weeklyLossKg()),
                        summary.weeklyLossWithinSafeRange()),
                new Status(
                        status.bmi().setScale(1, RoundingMode.HALF_UP).toPlainString(),
                        status.bmiCategory().label(),
                        kg(status.bodyFatPercent().value()),
                        status.bodyFatPercent().estimated(),
                        kg(status.fatMassKg().value()),
                        status.bodyFatLargeError(),
                        status.bodyFatMismatch(),
                        kcal(status.bmrKcal().value()),
                        status.bmrKcal().estimated(),
                        kcal(status.tdeeKcal().value()),
                        status.tdeeKcal().estimated(),
                        kcal(status.tdeeLowerKcal()),
                        kcal(status.tdeeUpperKcal())),
                report.etas().stream().map(eta -> etaRow(report, eta)).toList(),
                report.milestones().stream().map(ReportView::milestoneRow).toList(),
                report.macroPlan(),
                report.mealPlan(),
                report.narration().notices(),
                report.narration().faqs(),
                report.improvements(),
                evidence,
                evidence.stream().collect(Collectors.toUnmodifiableMap(EvidenceRow::key, EvidenceRow::number)),
                report.profile().activityLevel().estimated(),
                report.profile().goal().estimated(),
                report.profile().bodyFatPercent().estimated());
    }

    /** 본문 숫자의 각주: 주어진 key 중 이 리포트에 쓰인 출처만, 번호 순서로. */
    public List<EvidenceRow> refs(String... keys) {
        List<String> wanted = List.of(keys);
        return evidence.stream().filter(row -> wanted.contains(row.key())).toList();
    }

    /** "2027년 3월 둘째 주". 그 달의 1~7일이 첫째 주. */
    public static String formatWeekOfMonth(LocalDate date) {
        return date.getYear() + "년 " + date.getMonthValue() + "월 "
                + WEEK_OF_MONTH[(date.getDayOfMonth() - 1) / 7] + " 주";
    }

    public static String scenarioLabel(Scenario scenario) {
        return scenario == Scenario.DIET_ONLY ? "식단만" : "식단 + 주 " + scenario.sessionsPerWeek() + "일 운동";
    }

    /** 10kcal 단위, 천 단위 쉼표. */
    static String kcal(BigDecimal value) {
        BigDecimal rounded = value.divide(KCAL_STEP, 0, RoundingMode.HALF_UP).multiply(KCAL_STEP);
        return new DecimalFormat("#,##0").format(rounded);
    }

    /** 0.1 단위. */
    static String kg(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    /** 부호를 붙인 0.1 단위 ("−3.6", "+0.4", "0.0"). */
    static String signedKg(BigDecimal value) {
        BigDecimal rounded = value.setScale(1, RoundingMode.HALF_UP);
        return switch (rounded.signum()) {
            case -1 -> MINUS + rounded.negate().toPlainString();
            case 1 -> "+" + rounded.toPlainString();
            default -> rounded.abs().toPlainString();
        };
    }

    /** 도달 주차 범위 ("18~23주", "약 9주"). */
    static String etaRange(GoalEtaRange range) {
        return switch (range.expected()) {
            case GoalEta.NotApplicable notApplicable -> "지금 체중 유지";
            case GoalEta.Unreachable unreachable -> "52주 안에는 어려워요";
            case GoalEta.Reached reached -> {
                int earliest = range.earliest() instanceof GoalEta.Reached e ? e.week() : reached.week();
                if (!(range.latest() instanceof GoalEta.Reached latest)) {
                    yield earliest + "주 이상";
                }
                yield earliest == latest.week() ? "약 " + earliest + "주" : earliest + "~" + latest.week() + "주";
            }
        };
    }

    static String etaDate(Report report, GoalEtaRange range) {
        return switch (range.expected()) {
            case GoalEta.NotApplicable notApplicable -> "목표 체중이 지금과 가까워요";
            case GoalEta.Unreachable unreachable -> "52주 뒤 약 " + kg(unreachable.week52WeightKg()) + "kg";
            case GoalEta.Reached reached -> formatWeekOfMonth(report.dateAfterWeeks(reached.week()));
        };
    }

    private static EtaRow etaRow(Report report, ScenarioEta eta) {
        return new EtaRow(scenarioLabel(eta.scenario()), etaRange(eta.eta()), etaDate(report, eta.eta()),
                kg(eta.weeklyLossKg()), eta.withinSafeRange());
    }

    private static MilestoneRow milestoneRow(Milestone milestone) {
        String label = switch (milestone.type()) {
            case MilestoneType.CURRENT -> "지금";
            case MilestoneType.LOSS_5_PERCENT -> MINUS + "5%";
            case MilestoneType.LOSS_10_PERCENT -> MINUS + "10%";
            case MilestoneType.TARGET -> "목표";
        };
        return switch (milestone.eta()) {
            case Milestone.Eta.Reached reached -> new MilestoneRow(label, kg(milestone.weightKg()),
                    reached.week() == 0 ? "오늘" : reached.week() + "주 뒤", formatWeekOfMonth(reached.date()));
            case Milestone.Eta.NotReached notReached -> new MilestoneRow(label, kg(milestone.weightKg()),
                    "52주 안에는 어려워요", "4주마다 다시 계산해 보세요");
        };
    }

    private static List<EvidenceRow> evidenceRows(List<Evidence> evidence) {
        return IntStream.range(0, evidence.size())
                .mapToObj(i -> {
                    Evidence e = evidence.get(i);
                    return new EvidenceRow(i + 1, e.key(), e.authors(), e.year(), e.title(), e.identifier(),
                            e.isAssumption());
                })
                .toList();
    }
}
