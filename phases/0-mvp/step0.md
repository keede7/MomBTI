# Step 0: setup-model

## 목표
빌드 의존성, 도메인 모델, `Evidence` enum, `ProfileDefaults`, `Clock` 설정을 만든다. 이후 모든 step의 기반이다.

## 작업
1. `build.gradle`
   - Spring Boot 4.1.2 기준 starter를 추가한다: Web MVC, Thymeleaf, Validation. Boot 4에서 바뀐 starter 이름(예: `spring-boot-starter-webmvc`)과 테스트 starter(예: `spring-boot-starter-webmvc-test`)를 확인해서 쓴다. 빌드가 안 되면 실제 존재하는 좌표로 고친다.
   - 기존 `spring-boot-starter`는 Web MVC starter가 포함하므로 정리해도 된다.
2. `src/main/resources/application.yml`
   - `server.error.include-stacktrace: never`, `include-message: never`, `whitelabel.enabled: false`
   - Thymeleaf 기본 설정, 메시지 인코딩 UTF-8
3. 모델 (`core/domains/report/model/`, docs/ARCHITECTURE.md 패키지 구조 그대로)
   - `Gender`(MALE, FEMALE), `ActivityLevel`(5단계, PAL 1.40/1.55/1.70/1.85/2.00과 걸음 수 설명 보유)
   - `BodyProfile`(필수 4개 + `Optional` 선택 3개: 활동량, 목표 체중, 체지방률)
   - `Goal`(sealed: `TargetWeight` | `DefaultHealthyWeight`), `Scenario`(DIET_ONLY, EX3, EX4, EX5, 주당 횟수 보유)
   - `ReportMode`(LOSS, MAINTAIN, UNDERWEIGHT_MAINTAIN, MINOR_BLOCKED)
   - `Estimated<T>`(값, 추정 여부, `Evidence`)
   - `ProjectionPoint`, `Projection`, `GoalEta`(sealed: Reached | Unreachable | NotApplicable)는 필드만 정의 (계산은 step3)
   - `Report`는 아직 만들지 않는다 (step5)
4. `core/domains/report/evidence/Evidence` enum
   - docs/EVIDENCE.md의 key 전부를 1:1로 정의한다 (key, 저자, 연도, 제목, PMID/식별자, 상태 VERIFIED | ASSUMPTION)
   - EVIDENCE.md에서 "가정"인 항목은 ASSUMPTION
5. `core/domains/report/defaults/ProfileDefaults` → `ResolvedProfile`
   - PRD "선택 항목 기본값" 표를 그대로 구현: 활동량 없음 → 좌식 PAL 1.40 (밴드 상단 1.60), 목표 없음 → BMI ≥ 23이면 BMI 22 체중 / 18.5~22.9면 유지, 체지방률 없음 → Deurenberg 1991 추정
   - 기본값을 쓴 값은 `Estimated`의 추정 여부 true
   - Deurenberg 계산식은 step1의 `BodyCompositionEstimator`에 둘 것이므로, 이 step에서 `BodyCompositionEstimator`도 함께 최소 구현해도 된다 (테스트 포함)
6. `core/config/ClockConfig`: `Clock` Bean (Asia/Seoul)
7. 도메인 패키지(model, defaults, evidence)는 Spring에 의존하지 않는다

## 테스트 (먼저 작성)
- `ProfileDefaultsTest`: 선택 3개 각각 있음/없음, 추정 여부 플래그, BMI 22 기본 목표 계산, BMI 18.5~22.9 유지 판단
- `EvidenceTest`: EVIDENCE.md의 key 목록과 enum 이름이 일치하는지 (문서 파싱 또는 고정 목록)
- 기존 `contextLoads` 유지

## AC
- `./gradlew test` 통과
- `./gradlew build` 성공
