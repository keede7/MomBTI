package io.keede.mombti.core.domains.report.simulation;

import io.keede.mombti.core.domains.report.calculator.BmrCalculator;
import io.keede.mombti.core.domains.report.calculator.BodyCompositionEstimator;
import io.keede.mombti.core.domains.report.calculator.EnergyTargetCalculator;
import io.keede.mombti.core.domains.report.calculator.ExerciseEnergyCalculator;
import io.keede.mombti.core.domains.report.calculator.TdeeCalculator;
import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.exception.SimulationInvariantException;
import io.keede.mombti.core.domains.report.model.EnergyTarget;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.ProjectionPoint;
import io.keede.mombti.core.domains.report.model.ReportMode;
import io.keede.mombti.core.domains.report.model.Scenario;
import io.keede.mombti.core.domains.report.model.ScenarioProjections;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Hall 단순화 동적 체중모델 ({@link Evidence#HALL_2011}, docs/ARCHITECTURE.md "시뮬레이션 모델").
 * 하루 단위로 364일 계산하고 7일마다 기록한다. 섭취량은 시작 시점 값으로 고정한다.
 * <ol>
 *   <li>W = F + L, BMR은 매일 다시 계산 (Cunningham(L) 또는 Mifflin(W))</li>
 *   <li>소비 E = BMR × PAL + 운동 하루 평균(W)</li>
 *   <li>적응성 열발생 A = max(0, E − 섭취) × 0.10 ({@link Evidence#ADAPTIVE_THERMOGENESIS}, 가정)</li>
 *   <li>ΔE = 섭취 − (E − A), Forbes p = 10.4 / (10.4 + F) ({@link Evidence#HALL_2007_FORBES})</li>
 *   <li>ΔW = ΔE / (p × 1,816 + (1 − p) × 9,440) ({@link Evidence#HALL_2008})</li>
 * </ol>
 */
public class WeightProjectionSimulator {

    /** 예측 그래프·표에 쓰는 출처 (개인차 밴드 FRANKENFIELD_2005, 현실 보정 ADHERENCE_80 포함). */
    public static final List<Evidence> EVIDENCE = List.of(Evidence.HALL_2011, Evidence.HALL_2007_FORBES,
            Evidence.HALL_2008, Evidence.ADAPTIVE_THERMOGENESIS, Evidence.FRANKENFIELD_2005, Evidence.ADHERENCE_80);

    static final int SIMULATION_DAYS = 364;
    static final int DAYS_PER_POINT = 7;
    /** {@link Evidence#ADAPTIVE_THERMOGENESIS} 적자의 10% (가정). */
    static final BigDecimal ADAPTIVE_THERMOGENESIS_RATIO = new BigDecimal("0.10");
    /** {@link Evidence#ADHERENCE_80} 소비량과 목표 차이의 20%를 더 먹는다고 가정. */
    static final BigDecimal NON_ADHERENCE_RATIO = new BigDecimal("0.20");
    /** 불변식: 지방량은 이 값보다 커야 한다. */
    static final BigDecimal MIN_FAT_MASS_KG = new BigDecimal("0.5");

    /** 평형 체중은 같은 모델을 28일 간격으로 이어 계산해 체중 변화가 멈출 때의 값이다. */
    static final int EQUILIBRIUM_STEP_DAYS = 28;
    private static final int EQUILIBRIUM_MAX_STEPS = 1300;
    private static final BigDecimal EQUILIBRIUM_TOLERANCE_KG = new BigDecimal("0.001");

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final int POINT_SCALE = 3;

    private final BmrCalculator bmrCalculator;
    private final TdeeCalculator tdeeCalculator;
    private final ExerciseEnergyCalculator exerciseEnergyCalculator;
    private final EnergyTargetCalculator energyTargetCalculator;
    private final BodyCompositionEstimator bodyCompositionEstimator;

    public WeightProjectionSimulator(BmrCalculator bmrCalculator,
                                     TdeeCalculator tdeeCalculator,
                                     ExerciseEnergyCalculator exerciseEnergyCalculator,
                                     EnergyTargetCalculator energyTargetCalculator,
                                     BodyCompositionEstimator bodyCompositionEstimator) {
        this.bmrCalculator = bmrCalculator;
        this.tdeeCalculator = tdeeCalculator;
        this.exerciseEnergyCalculator = exerciseEnergyCalculator;
        this.energyTargetCalculator = energyTargetCalculator;
        this.bodyCompositionEstimator = bodyCompositionEstimator;
    }

    /**
     * 시작 상태. F = 체중 × 체지방률, L = 체중 − F.
     * 식단만으로 적자가 없으면(E-17, 유지 모드) 섭취를 시작 소비량으로 두어 식단만 라인을 유지선으로 만든다.
     */
    public SimulationInput input(ResolvedProfile profile, EnergyTarget target, ReportMode mode) {
        BigDecimal fatMass = bodyCompositionEstimator.fatMassKg(profile.weightKg(), profile.bodyFatPercent().value());
        BigDecimal intake = target.dietOnlyMaintains() ? target.tdeeKcal() : target.intakeKcal();
        return new SimulationInput(profile.gender(), profile.age(), profile.heightCm(), profile.pal(),
                !profile.bodyFatPercent().estimated(), fatMass, profile.weightKg().subtract(fatMass),
                intake, target.tdeeKcal(), mode);
    }

    public ScenarioProjections simulate(SimulationInput input, Scenario scenario) {
        return new ScenarioProjections(scenario,
                project(input, scenario, SimulationVariant.BASE),
                project(input, scenario, SimulationVariant.BMR_LOW),
                project(input, scenario, SimulationVariant.BMR_HIGH),
                project(input, scenario, SimulationVariant.ADHERENCE_80));
    }

    /** 0~52주 예측. 불변식을 어기면 {@link SimulationInvariantException} (E-29). */
    public Projection project(SimulationInput input, Scenario scenario, SimulationVariant variant) {
        BigDecimal intake = intakeKcal(input, variant);
        boolean guardMonotonic = input.mode() == ReportMode.LOSS && variant.monotonicGuarded();

        BodyState state = new BodyState(input.initialFatMassKg(), input.initialLeanMassKg());
        checkInvariants(state, 0);
        List<ProjectionPoint> points = new ArrayList<>();
        points.add(state.toPoint(0));
        for (int day = 1; day <= SIMULATION_DAYS; day++) {
            BodyState next = step(input, scenario, variant, state, intake, 1, day);
            checkInvariants(next, day);
            if (guardMonotonic && next.weight().compareTo(state.weight()) > 0) {
                throw new SimulationInvariantException("MONOTONIC_WEIGHT", day);
            }
            state = next;
            if (day % DAYS_PER_POINT == 0) {
                points.add(state.toPoint(day / DAYS_PER_POINT));
            }
        }
        return new Projection(scenario, points);
    }

    /** 섭취가 소비량과 같아져 체중 변화가 멈추는 체중 (E-20 "평형 체중"). 0.001kg 단위. */
    public BigDecimal equilibriumWeightKg(SimulationInput input, Scenario scenario, SimulationVariant variant) {
        BigDecimal intake = intakeKcal(input, variant);
        BodyState state = new BodyState(input.initialFatMassKg(), input.initialLeanMassKg());
        checkInvariants(state, 0);
        for (int i = 1; i <= EQUILIBRIUM_MAX_STEPS; i++) {
            int day = i * EQUILIBRIUM_STEP_DAYS;
            BodyState next = step(input, scenario, variant, state, intake, EQUILIBRIUM_STEP_DAYS, day);
            checkInvariants(next, day);
            boolean settled = next.weight().subtract(state.weight()).abs().compareTo(EQUILIBRIUM_TOLERANCE_KG) < 0;
            state = next;
            if (settled) {
                break;
            }
        }
        return state.weight().setScale(POINT_SCALE, RoundingMode.HALF_UP);
    }

    /** 변형별 하루 섭취. 현실 보정은 목표 + 20% × (시작 소비량 − 목표). */
    public BigDecimal intakeKcal(SimulationInput input, SimulationVariant variant) {
        if (!variant.partialAdherence()) {
            return input.intakeKcal();
        }
        BigDecimal gap = input.tdeeKcal().subtract(input.intakeKcal()).max(BigDecimal.ZERO);
        return input.intakeKcal().add(gap.multiply(NON_ADHERENCE_RATIO));
    }

    private BodyState step(SimulationInput input, Scenario scenario, SimulationVariant variant,
                           BodyState state, BigDecimal intake, int days, int day) {
        try {
            BigDecimal weight = state.weight();
            BigDecimal bmr = (input.useLeanMassBmr()
                    ? bmrCalculator.cunningham(state.lean())
                    : bmrCalculator.mifflinStJeor(input.gender(), input.age(), input.heightCm(), weight))
                    .multiply(variant.bmrFactor(), MC);
            BigDecimal expenditure = tdeeCalculator.tdee(bmr, input.pal())
                    .add(exerciseEnergyCalculator.dailyAverageKcal(weight, scenario.sessionsPerWeek()), MC);
            BigDecimal adaptive = expenditure.subtract(intake).max(BigDecimal.ZERO)
                    .multiply(ADAPTIVE_THERMOGENESIS_RATIO, MC);
            BigDecimal energyBalance = intake.subtract(expenditure.subtract(adaptive), MC);

            BigDecimal leanFraction = energyTargetCalculator.leanFraction(state.fat());
            BigDecimal density = energyTargetCalculator.energyDensityKcalPerKg(state.fat());
            BigDecimal weightChange = energyBalance.multiply(BigDecimal.valueOf(days), MC).divide(density, MC);

            return new BodyState(
                    state.fat().add(BigDecimal.ONE.subtract(leanFraction).multiply(weightChange, MC), MC),
                    state.lean().add(leanFraction.multiply(weightChange, MC), MC));
        } catch (ArithmeticException e) {
            throw new SimulationInvariantException("FINITE_VALUE", day);
        }
    }

    private static void checkInvariants(BodyState state, int day) {
        if (state.fat().compareTo(MIN_FAT_MASS_KG) <= 0) {
            throw new SimulationInvariantException("FAT_MASS_MIN", day);
        }
        if (state.lean().signum() <= 0) {
            throw new SimulationInvariantException("LEAN_MASS_POSITIVE", day);
        }
    }

    private record BodyState(BigDecimal fat, BigDecimal lean) {

        BigDecimal weight() {
            return fat.add(lean, MC);
        }

        ProjectionPoint toPoint(int week) {
            return new ProjectionPoint(week,
                    weight().setScale(POINT_SCALE, RoundingMode.HALF_UP),
                    fat.setScale(POINT_SCALE, RoundingMode.HALF_UP),
                    lean.setScale(POINT_SCALE, RoundingMode.HALF_UP));
        }
    }
}
