# Step 3: simulation

## 목표
`core/domains/report/simulation/`을 구현한다. 알고리즘은 docs/ARCHITECTURE.md "시뮬레이션 모델 (Hall 단순화)"를 그대로 따른다.

## 작업
- `ScenarioFactory`: DIET_ONLY, EX3, EX4, EX5 시나리오 생성
- `WeightProjectionSimulator`
  - 하루 단위 364일, 7일마다 `ProjectionPoint`(주차, 체중, 지방량, 제지방량)
  - 매일 BMR 재계산(체중 또는 제지방량), 적응성 열발생 10%(가정), Forbes 분배, 에너지 밀도 9,440 / 1,816
  - 섭취량 고정. 변형: BMR × 0.9 / × 1.1 밴드, 현실 보정 80% (섭취 = 목표 + 20% × (소비량 − 목표))
  - 불변식: F > 0.5kg, L > 0, 유한값, LOSS 모드에서 체중 단조 비증가. 위반 시 `SimulationInvariantException` (E-29)
- `GoalEtaCalculator`: 체중 ≤ 목표 + 0.05kg인 첫 주. 밴드로 범위("18~23주"). 52주 미도달 → `Unreachable`(52주 체중, 평형 체중)
- `MilestoneCalculator`: −5%, −10%, 목표 (식단만 기준 예상 주차와 날짜, 날짜는 주입받은 `Clock`)
- `ChartWindowPolicy`: ETA ≤ 12주 → 12주 + 도달 마커, 토글 숨김 (E-18) / 13~26주 → "목표까지 약 N주" + 6개월 토글 (E-19) / 26주 초과 또는 미도달 → 토글 + 중간 목표 안내 (E-20)
- 차트 데이터는 0~26주 2주 간격

## 테스트 (먼저 작성)
- 속성 테스트: 단조성, 불변식, 같은 적자에서 7,700kcal 정적 규칙보다 감량이 느림, 밴드 하단 ≤ 기본 ≤ 상단 순서
- 운동 일수가 많을수록 도달이 빠르거나 같음
- `ChartWindowPolicy` 경계: 12 / 13 / 26 / 27주 / 미도달 (E-18, E-19, E-20)
- `E29_시뮬레이션_불변식_위반이면_SimulationInvariantException`
- 고정 `Clock`(Asia/Seoul)으로 날짜 검증

## AC
- `./gradlew test` 통과
