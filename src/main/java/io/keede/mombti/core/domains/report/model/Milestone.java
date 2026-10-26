package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** 단계별 목표 하나. 예상 시점은 식단만 기본 예측 기준. */
public record Milestone(MilestoneType type, BigDecimal weightKg, Eta eta) {

    public Milestone {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(weightKg, "weightKg");
        Objects.requireNonNull(eta, "eta");
    }

    public sealed interface Eta {

        record Reached(int week, LocalDate date) implements Eta {
            public Reached {
                Objects.requireNonNull(date, "date");
            }
        }

        /** 52주 안에 도달하지 않음. */
        record NotReached() implements Eta {
        }
    }
}
