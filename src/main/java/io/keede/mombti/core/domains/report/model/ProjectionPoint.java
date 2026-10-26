package io.keede.mombti.core.domains.report.model;

import java.math.BigDecimal;

/** 주차별 체중과 체성분. */
public record ProjectionPoint(int week, BigDecimal weightKg, BigDecimal fatMassKg, BigDecimal leanMassKg) {
}
