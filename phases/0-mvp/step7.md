# Step 7: templates-chart

## 목표
입력 화면과 리포트 화면을 완성한다. docs/UI_GUIDE.md(색상 토큰, 컴포넌트, 그래프, 문구 톤, 접근성)와 docs/PRD.md "리포트 구성" 순서를 그대로 따른다.

## 작업
- `templates/index.html`: 필수 4개(진하게) + "더 맞춤형으로" 선택 3개(선택 라벨, 항상 보임). 분석 버튼 위 인라인 안내 한 줄. 오류 요약 + 첫 오류 필드 포커스
- `templates/report.html` + `fragments/`: summary-cards, chart, chart-table, milestones, eta-table, diagnosis, meal-plan, expectation, refine-panel, improvements, evidence (순서는 PRD 리포트 구성 1~11)
  - 추정 값에 "추정" 태그, 가정 값(운동 45분, 현실 보정 80%, 적응성 열발생)에 "가정" 태그
  - 숫자마다 출처 각주 → 근거 목록 앵커
  - `th:text`만 사용 (`th:utext` 금지)
- `templates/minor.html`, `error/4xx.html`, `error/5xx.html`(requestId 표시)
- `static/css/app.css`: UI_GUIDE 토큰, 라이트/다크, 360/768/1024 반응형, 760px 이하 카드 2×2
- `static/js/report-chart.js`: Chart.js 4.4.1 (cdnjs). 기본 12주, "6개월까지 보기" 토글(ChartWindowPolicy 결과대로 표시/숨김), 시나리오 범례 토글(기본: 식단만 + 주 4일), 밴드, 목표선, 현실 보정 토글, 애니메이션 끔. 차트 아래 표는 서버가 항상 렌더
- `static/js/form.js`: BMI 미리보기 (표시 전용, 계산 결과를 서버로 보내지 않음)
- JS에서 계산 금지 (서버가 준 데이터만 그린다)

## 테스트
- 페르소나 3개 E2E (`@SpringBootTest` + MockMvc): 리포트 HTML에 11개 섹션이 모두 있고 "N/A", "null"이 없음
- 차트 표(`<table>`)가 JS 없이 렌더됨
- 템플릿에 `th:utext`가 없음 (grep 테스트 또는 AC)

## AC
- `./gradlew test` 통과
- `grep -rn "th:utext" src/main/resources` 결과 없음
- `./gradlew build` 성공
