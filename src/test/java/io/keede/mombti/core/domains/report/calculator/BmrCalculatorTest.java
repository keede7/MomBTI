package io.keede.mombti.core.domains.report.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.keede.mombti.core.domains.report.defaults.ResolvedProfile;
import io.keede.mombti.core.domains.report.evidence.Evidence;
import io.keede.mombti.core.domains.report.model.ActivityLevel;
import io.keede.mombti.core.domains.report.model.Estimated;
import io.keede.mombti.core.domains.report.model.Gender;
import io.keede.mombti.core.domains.report.model.Goal;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BmrCalculatorTest {

    /** kcal 허용 오차 ±1. */
    private static final BigDecimal TOLERANCE = BigDecimal.ONE;

    private final BmrCalculator calculator = new BmrCalculator(new BodyCompositionEstimator());

    @Test
    void Mifflin_남자_30세_180cm_80kg는_1780kcal() {
        // 10 × 80 + 6.25 × 180 − 5 × 30 + 5 = 800 + 1125 − 150 + 5 = 1780
        BigDecimal bmr = calculator.mifflinStJeor(Gender.MALE, 30, new BigDecimal("180.0"), new BigDecimal("80.0"));

        assertThat(bmr).isCloseTo(new BigDecimal("1780"), within(TOLERANCE));
    }

    @Test
    void Mifflin_여자_25세_165cm_60kg는_1345kcal() {
        // 10 × 60 + 6.25 × 165 − 5 × 25 − 161 = 600 + 1031.25 − 125 − 161 = 1345.25
        BigDecimal bmr = calculator.mifflinStJeor(Gender.FEMALE, 25, new BigDecimal("165.0"), new BigDecimal("60.0"));

        assertThat(bmr).isCloseTo(new BigDecimal("1345.25"), within(TOLERANCE));
    }

    @Test
    void Cunningham_제지방량_60kg는_1666kcal() {
        // 370 + 21.6 × 60 = 1666
        BigDecimal bmr = calculator.cunningham(new BigDecimal("60"));

        assertThat(bmr).isCloseTo(new BigDecimal("1666"), within(TOLERANCE));
    }

    @Test
    void 체지방률_미입력이면_Mifflin() {
        ResolvedProfile profile = profile(Estimated.estimated(new BigDecimal("20.7"), Evidence.DEURENBERG_1991));

        Estimated<BigDecimal> bmr = calculator.bmr(profile);

        assertThat(bmr.value()).isCloseTo(new BigDecimal("1780"), within(TOLERANCE));
        assertThat(bmr.evidence()).contains(Evidence.MIFFLIN_1990);
        assertThat(bmr.estimated()).isFalse();
    }

    @Test
    void 체지방률_입력이면_Cunningham() {
        // 제지방량 = 80 × (1 − 0.25) = 60 → 1666
        ResolvedProfile profile = profile(Estimated.provided(new BigDecimal("25.0")));

        Estimated<BigDecimal> bmr = calculator.bmr(profile);

        assertThat(bmr.value()).isCloseTo(new BigDecimal("1666"), within(TOLERANCE));
        assertThat(bmr.evidence()).contains(Evidence.CUNNINGHAM_1991);
    }

    private static ResolvedProfile profile(Estimated<BigDecimal> bodyFat) {
        return new ResolvedProfile(Gender.MALE, 30, new BigDecimal("180.0"), new BigDecimal("80.0"),
                Estimated.estimated(ActivityLevel.SEDENTARY, Evidence.FAO_WHO_UNU_2004),
                new BigDecimal("1.60"),
                Estimated.estimated(new Goal.DefaultHealthyWeight(new BigDecimal("71.3"), false), Evidence.KSSO_2022),
                bodyFat);
    }
}
