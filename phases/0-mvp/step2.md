# Step 2: safety-energy-target

## 목표
안전 규칙(`SafetyPolicy`), 목표·모드 결정, 하루 섭취 목표(`EnergyTargetCalculator`)를 구현한다.

## 작업
- `core/domains/report/safety/SafetyPolicy` (안전 규칙은 이 클래스에만, ADR-010)
  - 19세 미만 → MINOR_BLOCKED (E-04). 80세 초과는 도메인 검증 오류 (E-05)
  - 65~80세: 적자 상한 20%, 단백질 하한 1.2g/kg (E-06)
  - BMI < 18.5 → UNDERWEIGHT_MAINTAIN (E-11)
  - 목표 BMI < 18.5 → `DomainValidationException`(목표 필드, 건강 범위 최저 체중 안내 값 포함) (E-13)
  - 섭취 하한 여 1,200 / 남 1,500 (AHA_ACC_TOS_2013) (E-16)
- Goal/ReportMode 결정
  - 목표 ≥ 현재 − 1kg → MAINTAIN (E-12)
  - 목표 없음 + BMI ≥ 23 → BMI 22 목표 (E-14), 목표 없음 + BMI 18.5~22.9 → MAINTAIN (E-15)
- `calculator/EnergyTargetCalculator`
  - 적자 = min(소비량 × 25%, 주당 체중 1% × ρ ÷ 7), 65세 이상은 20%
  - ρ = p × 1,816 + (1 − p) × 9,440, p = 10.4 / (10.4 + 지방량). **7,700kcal/kg 금지**
  - 섭취 = max(하한, 소비량 − 적자), 10kcal 단위 반올림
  - 하한 적용 후 적자 ≤ 0이면 식단만 라인은 유지 (E-17 표시용 플래그)

## 테스트 (먼저 작성, 메서드 이름에 E-코드)
- E-04, E-05, E-06, E-11, E-12, E-13, E-14, E-15, E-16, E-17
- 적자 환산 예제: 남 82kg, 지방량 20.5kg → ρ ≈ 6,870, 1% 상한 ≈ 805kcal/일 (±5)

## AC
- `./gradlew test` 통과
- `grep -rn "7700\|7,700" src/main` 결과 없음
