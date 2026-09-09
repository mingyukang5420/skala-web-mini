# PR 리뷰 기록

PR 리뷰 결과를 남기는 문서. 승인/변경요청 이력과 최종 판정을 기록한다.

GitHub 정책상 PR 작성자와 리뷰 실행 계정이 동일하면 정식 Approve/Request changes 상태를 쓸 수 없어, 모든 리뷰는 `gh pr review --comment` 댓글로 남기고 본문에 실제 판정(승인/변경요청)을 명시하는 방식으로 진행한다.

## 2026-09-09 - PR #14: feat: add domain entities and repositories with concurrency-safe assignment index

- 이슈: #1 [Feature] DB 스키마 및 Entity/Repository 반영
- 브랜치: `feat/1-db-schema-entity-repository` → `main`
- 근거 문서: `콕배정-DB.dbml`

### 검토 범위
- warehouse/dock/assignment/admin 4개 Entity
- Repository 4종
- `assignment(dock_id, status='ACTIVE')` 부분 유니크 인덱스 (schema.sql + `spring.jpa.defer-datasource-initialization` + `spring.sql.init.mode=always`)

### 1차 리뷰 (변경 요청)
- `Assignment.java`의 `driverName`, `scheduledTime`, `pinHash`, `status` 필드에 `@Column(nullable = false)`가 빠져 있음을 확인. DBML에는 4개 컬럼 모두 `not null`로 명시되어 있으나 Java 엔티티에는 반영되지 않아, `ddl-auto: update`로 테이블 생성 시 DBML과 실제 스키마가 어긋나는 문제였음.
- warehouse/dock/admin 엔티티, 부분 유니크 인덱스, Repository 4종은 문제 없음으로 확인.

### 조치
- 커밋 `21ef403` (fix: enforce not-null constraints on assignment columns per DB schema)에서 4개 필드에 `@Column(nullable = false)` 추가.

### 2차 확인 (최종 승인)
- `git diff main...feat/1-db-schema-entity-repository` 재확인: 4개 필드 모두 `@Column(nullable = false)` 반영됨, `dock`은 기존대로 `@JoinColumn(..., nullable = false)` 유지.
- 변경 범위는 1차 리뷰와 동일한 13개 파일이며, 추가 변경이나 회귀 없음.
- PR head(`21ef403`)와 로컬 브랜치 head 일치 확인.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(id 타입 Long vs int, lint/포매터 도입 시점)은 `docs/후속작업.md`에 기록.

## 2026-09-09 - PR #15: feat: add common exception handling with error code enum

- 이슈: #2 [Feature] 공통 에러 처리 골격 구현
- 브랜치: `feat/2-common-error-handling` → `main`
- 근거 문서: `설계_참고자료.md` §2 (에러 처리 설계), `콕배정-API.yml`

### 검토 범위
- `ErrorCode` enum (10종), `BusinessException`, `GlobalExceptionHandler`(`@RestControllerAdvice`), `ErrorResponse` record

### 검토 결과 (승인)
- ErrorCode enum 10종 전부 방문기사/관리자 에러코드 표와 코드·HTTP상태 정확히 일치. `VALIDATION_ERROR`, `WAREHOUSE_NOT_FOUND`는 두 표에 중복 등장하지만 enum에서는 단일 값으로 공유 처리됨. 누락/추가 코드 없음.
- `ASSIGNMENT_CONFLICT` 기본 메시지가 `콕배정-API.yml` 예제 문구와 완전히 일치.
- `GlobalExceptionHandler`가 `BusinessException` -> `{code, message}` + `errorCode.getStatus()`, `MethodArgumentNotValidException` -> 400 `VALIDATION_ERROR`(+필드별 메시지)로 정확히 변환함을 확인.
- `BusinessException`은 기본 메시지/커스텀 메시지 생성자를 모두 제공.
- `ErrorResponse`를 record로 구현한 것은 Java 21 DTO에 적합한 선택으로 판단.
- 패키지 배치(`com.kokbaejeong.exception`, `com.kokbaejeong.dto`)는 기존 컨벤션과 일치.
- `./gradlew compileJava --no-daemon` 빌드 성공 확인.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(PR 본문의 에러코드 개수 오타, 린터/포매터 도입 시점 재보류)은 `docs/후속작업.md` 판단 기준과 동일하게 유지 - 별도 신규 항목 없음.

## 2026-09-09 - PR #16: feat: add JWT-based admin authentication

- 이슈: #3 [Feature] 관리자 인증(JWT) 구현
- 브랜치: `feat/3-admin-jwt-auth` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-005, `설계_참고자료.md`(아키텍처/DTO 표), `콕배정-API.yml`

### 검토 범위
- `SecurityConfig`(BCryptPasswordEncoder 빈, `FilterRegistrationBean`으로 `/admin/*`에 필터 등록)
- `JwtTokenProvider`(jjwt 기반 HS256 발급/검증), `JwtAuthenticationFilter`(`/admin/login` 예외, 그 외 `/admin/*` 토큰 검사)
- `AuthService`/`AdminAuthController`(`POST /admin/login`), `AdminLoginRequest`/`TokenResponse` DTO
- `AdminRepository.findByUsername` 추가

### 검토 결과 (승인)
- `AdminLoginRequest`(username, password 둘 다 `@NotBlank`), `TokenResponse`(accessToken)가 설계_참고자료.md DTO 표와 필드명·타입 정확히 일치.
- `AUTH_FAILED`는 PR #15에서 정의된 `ErrorCode`를 그대로 재사용, 중복 정의 없음.
- 서블릿 필터가 `DispatcherServlet`보다 먼저 실행되어 `GlobalExceptionHandler`를 못 타는 구조적 제약을 인지하고, `ErrorResponse.of(ErrorCode.AUTH_FAILED)`를 직접 JSON 직렬화해 컨트롤러 경로와 동일한 `{code, message}` 형태로 401 응답 - 타당한 우회로 판단.
- BCrypt는 `PasswordEncoder.matches(rawPassword, hash)`로만 비교, 평문 비교 없음. JWT 키는 `Keys.hmacShaKeyFor`로 `jwt.secret` 환경변수에서 생성, 시크릿 미커밋.
- `AdminRepository.findByUsername`은 이번 이슈가 실제로 필요로 하는 시점에 추가된 것으로, PR #14 리뷰의 YAGNI 원칙에 부합하는 판단.
- 패키지 배치(`security`/`config`/`service`/`controller`)가 Controller -> Service -> Repository 3계층 구조에 맞고, 생성자 주입 스타일도 기존 코드와 일관됨.

### 독립 검증
- Docker로 Postgres를 임시로 띄우고(POSTGRES_DB/USER/PASSWORD를 kokbaejeong/kokbaejeong_user/change_me로 지정) 백엔드를 직접 기동, jshell + spring-security-crypto의 `BCrypt.hashpw`로 만든 해시를 admin 테이블에 심어 PR 본문이 주장한 7가지 시나리오(로그인 성공/실패 3종, 공백 검증, 무토큰 차단, 유효토큰 통과 후 404, 위변조 토큰 차단)를 curl로 전부 재현해 통과 확인.
- 추가로 `/Admin/warehouses`(대소문자), `/admin//warehouses`(이중 슬래시), `/admin/login/`(trailing slash) 등 필터 우회 가능성도 점검했으나 우회 없음.
- `./gradlew compileJava --no-daemon` 빌드 성공 확인.
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드 프로세스 종료. `.env`나 임시 소스 파일은 생성하지 않음(해시는 jshell 즉석 생성, 컨테이너 환경변수는 커맨드라인 직접 전달).

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항 없음.
