# Step 6: web

## 목표
컨트롤러, 폼 검증, refine, 에러 처리를 구현한다 (docs/ARCHITECTURE.md 데이터 흐름, 에러 처리 표).

## 작업
- `web/support/NumberNormalizer`: 쉼표·공백·단위 문자 정규화 ("72,5" → 72.5, "175cm" → 175). 실패하면 바인딩 오류 (E-10)
- `web/support/RequestIdFilter`: MDC requestId
- `web/dto/report/ReportForm`(@Valid, 필수 @NotNull, 선택 null 허용 + 범위), `ReportFormMapper`, `ReportView`(화면용 포맷), `ChartDataView`(2주 간격 26주 JSON)
- `web/controller/ReportController`
  - GET / → index
  - POST /report → 오류면 index 400 (값 유지), 미성년 → minor, 정상 → report
  - POST /report/refine → hidden 기존 값 + 선택 3개 병합, **전체 재검증** (E-26). 오류면 report 400 + 패널 오류 (E-25)
  - GET /report → 302 / (E-27)
- `core/exception/GlobalExceptionHandler`, `ErrorCode`: 404/405 → error/4xx (E-28), 계산 예외 → error/5xx + requestId (E-29). **로그에는 requestId와 예외 타입만**, 입력값 금지
- CSP 헤더(script: self + cdnjs.cloudflare.com, style: self + fonts.googleapis.com, font: fonts.gstatic.com), 요청 크기 제한
- 이 step의 템플릿은 테스트가 통과할 최소 골격만 만든다 (본격 화면은 step7)

## 테스트 (먼저 작성, `@WebMvcTest` 또는 MockMvc)
- E-03 필수 누락, E-07 키 < 3("cm로 입력" 안내), E-08 범위 밖, E-09 BMI < 12 또는 > 70, E-10 정규화
- E-23 체지방률 < 필수지방(남 5 / 여 12) 또는 > 60, E-24 BMI 추정과 15%p 이상 차이 → 경고만
- E-25, E-26, E-27, E-28
- 에러 로그에 입력값이 남지 않음 (로그 캡처로 검증)

## AC
- `./gradlew test` 통과
