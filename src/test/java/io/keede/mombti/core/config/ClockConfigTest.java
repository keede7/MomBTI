package io.keede.mombti.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ClockConfigTest {

    @Test
    void Clock은_Asia_Seoul_기준() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
