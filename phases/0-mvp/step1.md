# Step 1: calculators

## 목표
`core/domains/report/calculator/`의 계산기를 TDD로 구현한다. 공식과 상수는 docs/EVIDENCE.md와 docs/PRD.md "계산 규칙" 표를 그대로 따른다.

## 작업
- `BodyIndexCalculator`: BMI, KSSO 2022 구간 (<18.5 / 18.5~22.9 / 23~24.9 / 25~29.9 / ≥30)
- `BodyCompositionEstimator`: Deurenberg 1991 (이미 있으면 보강). BMI ≥ 41이면 "오차 큼" 플래그
- `BmrCalculator`: Mifflin-St Jeor(체중 기반) / Cunningham 1991 `370 + 21.6 × 제지방량`(체지방률 입력 시)
- `TdeeCalculator`: BMR × PAL
- `ExerciseEnergyCalculator`: `(4.8 − 1) × 체중 × 0.75h × 주 N회 ÷ 7` (HERRMANN_2024, EXERCISE_SESSION_45)
- `MacroCalculator`: 단백질 1.6g/kg (BMI ≥ 30이면 목표 체중 기준, 65세 이상 최소 1.2g/kg), 지방 25%, 탄수화물 나머지. 반올림 오차는 탄수화물로 보정 (E-21)
- 모든 상수는 이름 있는 상수로 두고, 해당 `Evidence`를 주석 또는 필드로 연결한다
- 순수 Java, Spring 의존 금지

## 테스트 (먼저 작성, 허용 오차 명시)
- Mifflin: 남 30세 180cm 80kg → 1,780kcal (±1). 여 예제 1개 이상
- Cunningham: 제지방량 60kg → 1,666kcal
- Deurenberg: 수기 계산 예제 2개 (남·여)
- 운동: 82kg, 주 4일 → 하루 약 134kcal (±1)
- BMI 구간 경계값 18.5, 23, 25, 30
- `E21_탄단지_반올림_합계가_목표와_다르면_탄수화물로_보정`

## AC
- `./gradlew test` 통과
