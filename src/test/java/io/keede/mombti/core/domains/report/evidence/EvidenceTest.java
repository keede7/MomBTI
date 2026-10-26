package io.keede.mombti.core.domains.report.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EvidenceTest {

    private static final Path EVIDENCE_DOC = Path.of("docs/EVIDENCE.md");
    private static final Pattern ROW = Pattern.compile("^\\|\\s*`([A-Z0-9_]+)`\\s*\\|.*\\|\\s*(검증됨|가정)\\s*\\|\\s*$");

    @Test
    void EVIDENCE_md의_key와_enum_이름이_1대1로_일치() throws IOException {
        Map<String, String> documented = documentedStatuses();

        assertThat(documented).isNotEmpty();
        assertThat(Arrays.stream(Evidence.values()).map(Enum::name))
                .containsExactlyInAnyOrderElementsOf(documented.keySet());
    }

    @Test
    void EVIDENCE_md의_상태와_enum_상태가_일치() throws IOException {
        Map<String, String> documented = documentedStatuses();

        for (Evidence evidence : Evidence.values()) {
            Evidence.Status expected = documented.get(evidence.name()).equals("검증됨")
                    ? Evidence.Status.VERIFIED
                    : Evidence.Status.ASSUMPTION;
            assertThat(evidence.status()).as(evidence.name()).isEqualTo(expected);
        }
    }

    @Test
    void 모든_출처는_저자_제목_식별자를_가짐() {
        for (Evidence evidence : Evidence.values()) {
            assertThat(evidence.key()).isEqualTo(evidence.name());
            assertThat(evidence.authors()).as(evidence.name()).isNotBlank();
            assertThat(evidence.title()).as(evidence.name()).isNotBlank();
            assertThat(evidence.identifier()).as(evidence.name()).isNotBlank();
        }
    }

    @Test
    void 가정_항목은_ASSUMPTION() {
        assertThat(Arrays.stream(Evidence.values()).filter(e -> e.status() == Evidence.Status.ASSUMPTION))
                .containsExactlyInAnyOrder(
                        Evidence.ADAPTIVE_THERMOGENESIS,
                        Evidence.ADHERENCE_80,
                        Evidence.EXERCISE_SESSION_45,
                        Evidence.ESSENTIAL_FAT);
    }

    private static Map<String, String> documentedStatuses() throws IOException {
        return Files.readAllLines(EVIDENCE_DOC).stream()
                .map(ROW::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.toMap(m -> m.group(1), m -> m.group(2), (a, b) -> a, LinkedHashMap::new));
    }
}
