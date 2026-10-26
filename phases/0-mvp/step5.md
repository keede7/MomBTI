# Step 5: report-service

## 목표
`Report` 모델과 `ReportQueryService`로 전체 흐름을 조립한다 (docs/ARCHITECTURE.md 데이터 흐름).

## 작업
- `model/Report`: 리포트 11개 섹션에 필요한 값 전부 (요약 카드 4개, 시나리오별 Projection과 밴드·현실 보정, ETA, 마일스톤, ChartWindow, 현재 상태, 매크로, 식단, 미리 알아둘 것/FAQ, 개선점, 근거 목록, ReportMode, 추정 여부)
- `service/ReportQueryService`: SafetyPolicy → ProfileDefaults → 지표 → Goal/ReportMode → EnergyTarget → ScenarioFactory → Simulator(기본, ±10%, 80%) → GoalEta, Milestone, ChartWindowPolicy → Macro, MealPlan, Improvement → Narrator. 조립과 순서만 담당
- `core/config/DomainBeanConfig`: 순수 도메인 클래스를 Bean으로 등록
- 리포트에 쓰인 `Evidence`만 근거 목록에 모은다

## 테스트 (먼저 작성)
- `E01_필수_4개만_입력해도_모든_섹션이_채워짐`: 모든 섹션이 null/빈 값이 아님, 기본값 사용 항목은 추정 true
- `E02_선택_항목_조합_8가지`: `@ParameterizedTest`로 2³ 조합
- 페르소나 3개(docs/PRD.md) 결과가 수기 계산과 ±1% 이내
- 미성년자는 MINOR_BLOCKED이고 감량 수치가 없음

## AC
- `./gradlew test` 통과
