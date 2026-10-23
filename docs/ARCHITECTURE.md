# 아키텍처

## 디렉토리 구조

기존 규칙(core/domains, config, exception, web)을 따른다. MVP에는 DB와 인증이 없으므로 `entity/`, `config/security/`, `web/auth/`, `external/`은 만들지 않는다.

```
src/main/java/io/keede/mombti
├── core/
├   └── domains/
├       └── report/                              다이어트 리포트 도메인 (순수 Java, Spring 의존 금지)
├           └── model/                           입력과 결과 모델 (record, sealed interface)
├               └── BodyProfile                  필수 4개 + Optional 선택 3개
├               └── Gender, ActivityLevel        enum (ActivityLevel은 PAL 값 보유)
├               └── Goal                         sealed: TargetWeight | DefaultHealthyWeight
├               └── Scenario                     DIET_ONLY, EX3, EX4, EX5 (주당 운동 횟수 보유)
├               └── ProjectionPoint, Projection  주차별 체중·지방량
├               └── GoalEta                      sealed: Reached(week) | Unreachable(week52Weight, equilibrium) | NotApplicable
├               └── ReportMode                   LOSS | MAINTAIN | UNDERWEIGHT_MAINTAIN | MINOR_BLOCKED
├               └── Estimated<T>                 값 + 추정 여부 + Evidence
├               └── Report                       리포트 전체 결과
├           └── defaults/
├               └── ProfileDefaults              선택 항목 기본값의 유일한 구현 위치 → ResolvedProfile
├           └── evidence/
├               └── Evidence                     출처 enum (key, 저자, 연도, 제목, PMID/DOI)
├           └── safety/
├               └── SafetyPolicy                 입력값 기반 안전 규칙 → ReportMode, 적자 상한, 단백질 하한
├           └── calculator/
├               └── BodyIndexCalculator          BMI, BMI 구간
├               └── BodyCompositionEstimator     체지방률 추정 (Deurenberg)
├               └── BmrCalculator                Mifflin-St Jeor / Katch-McArdle
├               └── TdeeCalculator               BMR × PAL
├               └── ExerciseEnergyCalculator     운동 1회 가정(MET 4.3, 45분) → 하루 평균 kcal
├               └── EnergyTargetCalculator       적자, 하한, 하루 섭취 목표
├               └── MacroCalculator              탄단지 (반올림 보정)
├           └── simulation/
├               └── ScenarioFactory              시나리오 4개 생성
├               └── WeightProjectionSimulator    Hall 단순화 모델, 하루 단위 364일
├               └── GoalEtaCalculator            도달 주차와 범위
├               └── MilestoneCalculator          −5%, −10%, 목표
├               └── ChartWindowPolicy            12주 / 26주 표시 규칙
├           └── advice/
├               └── MealPlanCatalog              칼로리 구간별 한식 템플릿, 비율 조정
├               └── ImprovementRuleEngine        개선할 부분 규칙 (Rule 인터페이스)
├               └── AdviceNarrator               문장 생성 인터페이스 (2차에 LLM 구현체)
├               └── TemplateAdviceNarrator       MVP 구현체 (messages 기반 템플릿)
├           └── exception/
├               └── DomainValidationException    code + field
├               └── SimulationInvariantException
├           └── service/
├               └── ReportQueryService           읽기 전용. 조립과 순서만 담당
├   └── config/
├       └── ClockConfig                          Clock(Asia/Seoul) Bean
├       └── DomainBeanConfig                     순수 도메인 클래스를 Bean으로 등록
├   └── exception/
├       └── GlobalExceptionHandler               @ControllerAdvice
├       └── ErrorCode
├   └── web/
├       └── controller/
├           └── ReportController                 GET /, POST /report, POST /report/refine, GET /report
├       └── dto/report/
├           └── ReportForm                       @Valid. 필수 @NotNull, 선택 null 허용 + 범위
├           └── ReportFormMapper                 ReportForm → BodyProfile
├           └── ReportView                       화면용 포맷 값 (반올림, 날짜 문자열)
├           └── ChartDataView                    차트 JSON (2주 간격 26주)
├       └── support/
├           └── NumberNormalizer                 "72,5", "175cm" 정규화
├           └── RequestIdFilter                  MDC requestId

src/main/resources
├── templates/
├   └── index.html                               입력 화면
├   └── report.html                              리포트
├   └── minor.html                               미성년 안내
├   └── error/4xx.html, error/5xx.html
├   └── fragments/                               summary-cards, chart, chart-table, milestones, eta-table,
├                                                diagnosis, meal-plan, expectation, refine-panel, improvements, evidence, field
├── static/
├   └── css/app.css
├   └── js/form.js                               인라인 안내, BMI 미리보기 (표시 전용)
├   └── js/report-chart.js                       Chart.js 렌더, 범례·기간·현실 보정 토글
├── messages.properties                          화면 문구, 오류 메시지 (E-코드와 매핑)
└── application.yml
```

```
src/test/java/io/keede/mombti
├── core/
├   └── domains/
├       └── report/
├           └── fixture/                         페르소나 3개, 경계값 프로필
├           └── calculator/                      논문 예제값 단위 테스트
├           └── simulation/                      속성 테스트, ChartWindowPolicy 경계
├           └── safety/, defaults/, advice/
├           └── service/                         E-01, E-02 통합 테스트
├   └── config/
├   └── web/
├       └── controller/                          @WebMvcTest (검증, 뷰 선택, 에러)
├       └── e2e/                                 @SpringBootTest + MockMvc 페르소나 시나리오
```

## 패턴
- **계층**: `web` → `service` → 도메인 컴포넌트. `web`은 `service`만 호출한다.
- **순수 도메인**: calculator, simulation, advice, safety, defaults는 Spring 어노테이션 없이 생성자 주입만 사용한다. Bean 등록은 `DomainBeanConfig`에서 한다.
- **불변 모델**: 모든 모델은 record. 선택값은 도메인 경계에서 `Optional`로 받고, `ProfileDefaults.resolve()`가 `ResolvedProfile`(모든 값 확정 + 추정 여부)로 바꾼다. 그 뒤 계층에서는 null과 Optional을 다루지 않는다.
- **출처 추적**: 계산 결과 중 화면에 나가는 값은 `Estimated<T>`로 감싸 `Evidence` 목록을 함께 가진다. 템플릿은 이것으로 각주 번호를 매긴다.
- **규칙 객체**: 개선할 부분은 `Rule` 인터페이스 구현체 목록으로 만든다. 규칙 하나가 클래스 하나이고, 우선순위를 갖는다.
- **확장점**: `AdviceNarrator`는 계산된 `Report`를 받아 문장만 만든다. 숫자를 바꾸지 않는다. 2차 LLM 구현체도 이 계약을 지킨다 (docs/REPORT_STRATEGY.md).
- **점진적 향상**: 서버가 완성된 HTML(표 포함)을 렌더한다. JS는 차트, 토글, 인라인 안내만 더한다.

## 데이터 흐름
```
GET  /                → index.html (빈 ReportForm)

POST /report          → NumberNormalizer → @Valid ReportForm
                        ├ 오류 → index.html 재렌더 (400, 값 유지, 오류 요약)
                        └ ReportFormMapper → BodyProfile
                          → ReportQueryService.generate(profile)
                             1. SafetyPolicy.evaluate        → MINOR_BLOCKED면 minor.html
                             2. ProfileDefaults.resolve      → ResolvedProfile
                             3. BodyIndex, BodyComposition, Bmr, Tdee
                             4. Goal 해석 + SafetyPolicy     → ReportMode (E-11~E-15)
                             5. EnergyTargetCalculator       → 하루 섭취 목표 (E-16, E-17)
                             6. ScenarioFactory(4개) → WeightProjectionSimulator
                                × (기본, BMR 0.9, BMR 1.1, 현실 보정 80%), 각 364일
                             7. GoalEtaCalculator, MilestoneCalculator, ChartWindowPolicy
                             8. MacroCalculator, MealPlanCatalog, ImprovementRuleEngine
                             9. AdviceNarrator               → 문장
                          → Report → ReportView + ChartDataView(JSON, th:inline) → report.html

POST /report/refine   → hidden 기존 필수·선택 값 + 패널 입력 병합 → /report와 같은 처리
                        ├ 오류 → 기존 값으로 report.html 재렌더 + 패널 안 오류 (400)
                        └ report.html (#refine 앵커로 이동)

GET  /report          → 302 /
```

## 시뮬레이션 모델 (Hall 단순화)
- 상태: 지방량 F, 제지방량 L. 초기값은 F = 체중 × 체지방률, L = 체중 − F.
- 하루 단위로 364일 반복하고, 7일마다 `ProjectionPoint`를 기록한다 (0~52주).
- 하루 계산:
  1. W = F + L
  2. BMR = 체지방률 입력 시 Katch-McArdle(L), 아니면 Mifflin-St Jeor(W). 밴드 계산에서는 × 0.9 / × 1.1
  3. 소비 E = BMR × PAL + 운동 하루 평균 kcal(W)
  4. 적응성 열발생 A = max(0, E − 섭취) × 0.10 (가정, docs/EVIDENCE.md)
  5. 에너지 차이 ΔE = 섭취 − (E − A)
  6. Forbes 분배 p = 10.4 / (10.4 + F)
  7. ΔW = ΔE / (p × 1,816 + (1 − p) × 9,440), F += (1 − p) × ΔW, L += p × ΔW
- 섭취량은 시작 시점 목표로 고정한다 (보수적 가정). 리포트에서 "4주마다 다시 계산"을 안내한다.
- 불변식: F > 0.5kg, L > 0, 모든 값이 유한값, LOSS 모드에서 체중이 단조 비증가. 위반하면 `SimulationInvariantException`.
- 도달 판정: 기록된 주차 중 체중 ≤ 목표 + 0.05kg인 첫 주. 52주까지 없으면 `Unreachable`.
- 차트 데이터: 0~26주를 2주 간격으로 서버가 만든다. 화면은 12주/26주만 전환한다 (`ChartWindowPolicy`).

## 에러 처리
| 상황 | 발생 위치 | 처리 | HTTP |
|---|---|---|---|
| 필수 누락, 범위 밖, 파싱 실패 | `@Valid` | index.html 재렌더, 필드 오류 + 상단 요약, 값 유지 | 400 |
| refine 입력 오류 | `@Valid` | report.html 재렌더 (기존 값 기준), 패널 안 오류 | 400 |
| `DomainValidationException` | 도메인 이중 검증 (웹 검증 우회 입력) | BindingResult로 변환해 위와 같이 처리 | 400 |
| 미성년자 | `SafetyPolicy` (예외 아님) | minor.html | 200 |
| `NoResourceFoundException`, `HttpRequestMethodNotSupportedException` | 라우팅 | error/4xx.html | 404 / 405 |
| `SimulationInvariantException`, 그 외 예외 | 계산 | error/5xx.html + requestId. 로그는 requestId와 예외 타입만 | 500 |

- `server.error.include-stacktrace=never`, `server.error.include-message=never`, `server.error.whitelabel.enabled=false`
- 요청 크기 제한, CSP 헤더(script는 self + cdnjs.cloudflare.com, style은 self + fonts)
- CSRF: 인증과 상태 변경이 없으므로 MVP에서는 적용하지 않는다 (ADR-017과 함께 재검토).

## 상태 관리
- 서버는 무상태다. DB, 세션, 쿠키를 쓰지 않는다.
- 리포트에서 재계산할 때 필요한 기존 입력값은 report.html의 hidden 필드로 왕복한다. 서버는 매번 전체를 다시 검증한다 (E-26).
- 클라이언트 상태는 차트 토글(시나리오 표시, 기간, 현실 보정)뿐이며 페이지 안 변수로만 관리한다.

## 테스트 전략
1. **P0 최소 입력**: 필수 4개만 넣은 E2E, 선택 3개 조합 8가지 파라미터화 테스트 (E-01, E-02)
2. **계산기 단위 테스트**: 논문 예제값과 경계값 (예: 남 30세 180cm 80kg → Mifflin 1,780kcal)
3. **E-코드 테스트**: PRD 엣지 케이스 표의 30개. 테스트 이름에 E-코드 포함
4. **시뮬레이터 속성 테스트**: 단조성, 불변식, 7,700kcal 정적 규칙보다 감량이 느린지
5. **ChartWindowPolicy 경계**: 12주 / 13주 / 26주 / 27주 / 미도달
6. **웹 슬라이스**: `@WebMvcTest`로 검증, 뷰 선택, 에러 페이지
7. **E2E**: `@SpringBootTest` + MockMvc로 페르소나 3개
8. **시간 고정**: 테스트용 고정 `Clock`
