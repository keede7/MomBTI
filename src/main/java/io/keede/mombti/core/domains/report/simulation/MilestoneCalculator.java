package io.keede.mombti.core.domains.report.simulation;

import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.Milestone;
import io.keede.mombti.core.domains.report.model.MilestoneType;
import io.keede.mombti.core.domains.report.model.Projection;
import io.keede.mombti.core.domains.report.model.ReportMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * 단계별 목표: 지금 → −5% ({@link Evidence#WING_2011}) → −10% → 목표.
 * 예상 시점은 식단만 기본 예측 기준이고, 날짜는 주입받은 {@link Clock}(Asia/Seoul)으로 계산한다.
 * 목표보다 가볍거나 같은 단계는 생략한다. 감량 모드가 아니면 단계가 없다.
 */
public class MilestoneCalculator {

    static final BigDecimal LOSS_5_RATIO = new BigDecimal("0.95");
    static final BigDecimal LOSS_10_RATIO = new BigDecimal("0.90");

    private final GoalEtaCalculator goalEtaCalculator;
    private final Clock clock;

    public MilestoneCalculator(GoalEtaCalculator goalEtaCalculator, Clock clock) {
        this.goalEtaCalculator = goalEtaCalculator;
        this.clock = clock;
    }

    public List<Milestone> milestones(SimulationInput input, Projection dietOnly, BigDecimal targetKg) {
        if (input.mode() != ReportMode.LOSS) {
            return List.of();
        }
        BigDecimal current = dietOnly.points().getFirst().weightKg().setScale(1, RoundingMode.HALF_UP);
        List<Milestone> milestones = new ArrayList<>();
        milestones.add(new Milestone(MilestoneType.CURRENT, current, new Milestone.Eta.Reached(0, dateAfterWeeks(0))));
        addIfAboveTarget(milestones, MilestoneType.LOSS_5_PERCENT, current.multiply(LOSS_5_RATIO), dietOnly, targetKg);
        addIfAboveTarget(milestones, MilestoneType.LOSS_10_PERCENT, current.multiply(LOSS_10_RATIO), dietOnly, targetKg);
        milestones.add(milestone(MilestoneType.TARGET, targetKg, dietOnly));
        return List.copyOf(milestones);
    }

    /** 오늘(서울)부터 N주 뒤 날짜. */
    public LocalDate dateAfterWeeks(int weeks) {
        return LocalDate.now(clock).plusWeeks(weeks);
    }

    private void addIfAboveTarget(List<Milestone> milestones, MilestoneType type, BigDecimal weightKg,
                                  Projection dietOnly, BigDecimal targetKg) {
        BigDecimal rounded = weightKg.setScale(1, RoundingMode.HALF_UP);
        if (rounded.compareTo(targetKg) > 0) {
            milestones.add(milestone(type, rounded, dietOnly));
        }
    }

    private Milestone milestone(MilestoneType type, BigDecimal weightKg, Projection dietOnly) {
        OptionalInt week = goalEtaCalculator.firstWeekAtOrBelow(dietOnly, weightKg);
        Milestone.Eta eta = week.isPresent()
                ? new Milestone.Eta.Reached(week.getAsInt(), dateAfterWeeks(week.getAsInt()))
                : new Milestone.Eta.NotReached();
        return new Milestone(type, weightKg, eta);
    }
}
