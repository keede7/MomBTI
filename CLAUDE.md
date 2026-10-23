# 프로젝트: MomBTI

성별·나이·키·몸무게만으로 하루 섭취 목표와 체중 변화 예측을 보여주는 다이어트 리포트 웹앱 (1차 MVP, 로그인·저장 없음).
상세 요구사항은 docs/PRD.md, 구조는 docs/ARCHITECTURE.md, 결정 근거는 docs/ADR.md, 공식과 논문은 docs/EVIDENCE.md를 따른다.

## 기술 스택
- Spring Boot 4.1.2 (webmvc, thymeleaf, validation)
- Java 21 (record, sealed interface 적극 사용)
- Thymeleaf SSR + Chart.js 4.4.1 (cdnjs, 점진적 향상)
- 테스트: JUnit 5, AssertJ, Spring MockMvc
- 저장소 없음 (DB, 세션, 쿠키 미사용)

## 아키텍처 규칙
- CRITICAL: 모든 계산 공식과 상수는 `Evidence` enum의 출처와 연결할 것. docs/EVIDENCE.md에서 "검증됨"이 아닌 공식은 코드에서 사용하지 말 것
- CRITICAL: 필수 입력 4개(성별, 나이, 키, 몸무게)만으로 리포트의 모든 섹션이 빠짐없이 렌더되어야 함. 빈 섹션, null, "N/A" 출력 금지
- CRITICAL: 선택 항목(활동량, 목표 체중, 체지방률)의 기본값 적용은 `ProfileDefaults`에서만 할 것. 기본값으로 계산한 값은 `Estimated<T>`로 표시하고 화면에 "추정" 태그를 붙일 것
- CRITICAL: 계산 로직은 도메인(`core/domains/report`)에만 둘 것. 컨트롤러, DTO, 템플릿, JS에서 계산 금지 (JS의 BMI 미리보기는 표시 전용)
- CRITICAL: 사용자 입력값을 로그, 예외 메시지, 메트릭에 남기지 말 것. 에러 로그에는 requestId와 예외 타입만 기록
- CRITICAL: 템플릿 출력은 `th:text`만 사용 (`th:utext` 금지)
- 도메인 계층(calculator, simulation, advice, safety, defaults)은 Spring에 의존하지 않는 순수 Java로 작성
- 안전 규칙(미성년자 차단, BMI 18.5 미만 감량 금지, 목표 BMI 하한, 칼로리 하한, 적자 상한)은 `SafetyPolicy`에만 둘 것
- 차트는 항상 같은 데이터의 `<table>`과 함께 렌더 (JS가 꺼져 있어도 정보 손실 없음)
- 날짜 계산은 주입받은 `Clock`(Asia/Seoul) 사용. `LocalDate.now()` 직접 호출 금지
- 패키지 구조는 docs/ARCHITECTURE.md를 따를 것
- MVP 제외 기능(docs/BACKLOG.md)은 구현하지 말 것

## 개발 프로세스
- CRITICAL: 새 기능 구현 시 반드시 테스트를 먼저 작성하고, 테스트가 통과하는 구현을 작성할 것 (TDD)
- CRITICAL: PRD의 엣지 케이스(E-코드)는 테스트 메서드 이름에 코드를 포함할 것 (예: `E07_키를_m로_입력하면_cm_안내_오류`). E-코드를 바꾸면 PRD와 테스트를 함께 수정
- 계산기 테스트는 논문 또는 수기 계산 예제값으로 검증 (허용 오차 명시)
- 커밋 메시지는 conventional commits 형식을 따를 것 (feat:, fix:, docs:, refactor:, test:)

## 명령어
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun` → http://localhost:8080
- 빌드: `./gradlew build`
- phase 실행: `python3 scripts/execute.py <phase-dir>`
