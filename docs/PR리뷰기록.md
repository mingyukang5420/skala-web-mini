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

## 2026-09-09 - PR #17: feat: add warehouse admin CRUD endpoints

- 이슈: #4 [Feature] 창고 관리 CRUD API
- 브랜치: `feat/4-warehouse-admin-crud` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-006, `설계_참고자료.md`(DTO 표), `콕배정-API.yml`(`/admin/warehouses` 전체)

### 검토 범위
- `WarehouseAdminController`(GET/POST `/admin/warehouses`, GET/PUT `/admin/warehouses/{id}`, POST `/admin/warehouses/{id}/(de)activate`)
- `WarehouseService`(등록/전체조회/단건조회/수정/활성화/비활성화)
- `WarehouseCreateRequest`, `WarehouseAdminResponse` DTO

### 검토 결과 (승인)
- 6개 엔드포인트 모두 `콕배정-API.yml`의 경로/메서드/상태코드(등록 201, 나머지 200, 404, 400)와 정확히 일치.
- `WarehouseCreateRequest`를 POST/PUT 양쪽에 재사용하고 별도 `UpdateRequest`를 만들지 않은 것은 `설계_참고자료.md` DTO 표 명명과는 다르지만, API yml이 PUT 요청 바디에도 동일한 `$ref: WarehouseCreateRequest` 스키마를 명시하고 있어 API 스펙을 그대로 따른 타당한 선택으로 판단(yml이 우선 근거 문서).
- `WarehouseService.getAll()`/`getById()`가 `deletedAt` 기준 필터링을 하지 않아, 목록/단건 조회 모두 비활성 창고를 포함 - "비활성화 포함" 스펙과 "수정 폼 진입용 단건 조회"에서 재활성화 대상을 볼 수 있어야 하는 요구를 정확히 충족.
- `deactivate()`/`activate()`는 자기 자신의 `deletedAt`만 토글하고 도크 테이블을 전혀 참조하지 않음 - `콕배정-API.yml`의 "하위 도크는 함께 비활성화하지 않음" 명시사항과 일치(도크 엔티티가 아직 없어 카스케이딩 코드 자체가 존재하지 않음도 확인).
- 대상 없음 시 기존 `ErrorCode.WAREHOUSE_NOT_FOUND`(PR #15에서 정의)를 그대로 재사용, 신규 에러코드 없음.
- `@Transactional(readOnly = true)`를 클래스 레벨에 걸고 쓰기 메서드에만 `@Transactional`을 오버라이드하는 패턴은 통상적인 Spring 관례에 부합. `update()`가 명시적 `save()` 없이 변경 감지(dirty checking)로 반영되는 것도 트랜잭션 내 관리 엔티티라 정상 동작 확인.
- 생성자 주입 스타일이 `AuthService`(PR #16)와 일관됨.

### 독립 검증
- Docker로 Postgres를 임시로 띄우고(`.env.example`을 `.env`로 복사해 `POSTGRES_DB=kokbaejeong`/`POSTGRES_USER=kokbaejeong_user`/`POSTGRES_PASSWORD=change_me` 사용), 백엔드를 환경변수로 직접 기동, jshell + spring-security-crypto의 `BCryptPasswordEncoder`로 만든 해시를 admin 테이블에 심어 로그인 토큰 발급.
- PR 본문이 주장한 10가지 시나리오(등록 201/검증 실패 400/목록 200/단건 200/404/수정 200/비활성화 200 active:false/비활성화 후에도 목록·단건에 노출/재활성화 200 active:true/없는 창고 비활성화 404/무토큰 401)를 curl로 전부 재현해 모두 통과 확인.
- `./gradlew compileJava --no-daemon` 빌드 성공 확인.
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드 프로세스 종료, 임시로 만든 `.env` 삭제, `git status` clean 확인.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항 없음.

## 2026-09-09 - PR #18: feat: 도크 관리자 CRUD API 추가

- 이슈: #5 [Feature] 도크 관리 CRUD API
- 브랜치: `feat/5-dock-admin-crud` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-007, `설계_참고자료.md`(DTO 표), `콕배정-API.yml`(`/admin/docks` 전체), `콕배정-DB.dbml`(dock 엔티티)

### 검토 범위
- `DockAdminController`(GET `/admin/docks?warehouseId=`, POST `/admin/docks`, GET/PUT `/admin/docks/{id}`, POST `/admin/docks/{id}/(de)activate`)
- `DockService`(창고별 목록/단건조회/등록/수정/활성화/비활성화, ACTIVE 배정 존재 시 비활성화 차단)
- `DockCreateRequest`, `DockUpdateRequest`, `DockAdminResponse` DTO
- `DockRepository.findByWarehouseId`, `AssignmentRepository.existsByDockIdAndStatus` 추가

### 검토 결과 (승인)
- 6개 엔드포인트 모두 `콕배정-API.yml`의 경로/메서드/상태코드(등록 201, 나머지 200/404/400)와 정확히 일치. 목록 조회가 `warehouseId` 쿼리 파라미터를 필수로 받는 것도 yml과 일치.
- `DockCreateRequest`(warehouseId·name·size·status 필수)/`DockUpdateRequest`(warehouseId 없음)가 `설계_참고자료.md` DTO 표 및 yml 스키마와 정확히 일치 - "소속 창고는 수정 시 변경 불가" 요구사항을 필드 자체를 빼는 방식으로 구현.
- `DockAdminResponse`가 `콕배정-DB.dbml` dock 테이블의 모든 노출 대상 컬럼(4개 boolean 특성 포함) + `active`(deletedAt IS NULL)까지 빠짐없이 포함.
- 등록 시 `warehouseId`로 창고를 조회해 없으면 `WAREHOUSE_NOT_FOUND`(404)를 던지는 것은 API yml에 명시된 케이스는 아니지만, 이 체크가 없으면 FK 제약 위반이 원시 500으로 새어나가는 것을 막는 합리적인 방어 코드로 판단 - 기존 `ErrorCode`를 재사용했을 뿐 신규 코드 추가 없음.
- `assignmentRepository.existsByDockIdAndStatus(id, ACTIVE)`로 ACTIVE 배정만 비활성화를 막고 CANCELLED는 걸리지 않음을 코드와 실제 요청으로 확인.
- `getByWarehouse()`/`getById()`가 `deletedAt` 기준 필터링을 하지 않아 비활성 도크도 목록/단건 조회에 그대로 노출됨 - "비활성화 포함" 스펙 및 재활성화 대상을 관리자가 볼 수 있어야 하는 요구를 충족.
- `@Transactional(readOnly = true)` 클래스 레벨 + 쓰기 메서드 오버라이드, 생성자 주입 스타일이 `WarehouseService`/`WarehouseAdminController`(PR #17)와 일관됨.
- 커밋 4개(DTO -> Repository 쿼리 -> Service -> Controller)를 각각 `git checkout`해 `./gradlew compileJava --no-daemon`으로 개별 빌드 확인 - 4개 전부 독립적으로 컴파일 성공, 빌드 가능 우선 원칙을 지키면서 최대한 잘게 쪼갠 분리로 판단.

### 독립 검증
- `docker compose up -d db`(POSTGRES_DB/USER/PASSWORD를 kokbaejeong/kokbaejeong_user/change_me로 지정)로 Postgres를 띄우고 백엔드를 환경변수로 직접 기동, jshell + spring-security-crypto의 `BCryptPasswordEncoder`로 만든 해시를 admin 테이블에 심어 로그인 토큰 발급, 창고 1개를 SQL로 시딩.
- PR 본문이 주장한 13가지 시나리오(등록 201/없는 창고 404 WAREHOUSE_NOT_FOUND/빈 name 400/목록 200/단건 200/없는 도크 404/수정 200 필드 반영/raw SQL로 ACTIVE 배정 심고 비활성화 시도 400 DOCK_HAS_ACTIVE_ASSIGNMENT/배정 CANCELLED 처리 후 재시도 200 active:false/비활성 도크도 목록 노출/재활성화 200 active:true/없는 도크 비활성화 404/무토큰 401)를 curl로 전부 재현해 모두 통과 확인.
- `./gradlew compileJava --no-daemon` 빌드 성공 확인.
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드 프로세스 종료, `.env`는 생성하지 않음, `git status` clean 확인.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(필수 쿼리 파라미터 누락 시 응답 포맷이 앱 공통 에러 포맷과 다름)은 `docs/후속작업.md`에 기록.

## 2026-09-09 - PR #19: feat: 방문 기사 창고/도크 조회 API 추가

- 이슈: #6 [Feature] 방문 기사 창고/도크 조회 API
- 브랜치: `feat/6-visitor-warehouse-dock-query` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-001/002, REQ-NFR-001, `설계_참고자료.md`(DTO 표, 에러코드 표), `콕배정-API.yml`(`/warehouses/{id}`, `/warehouses/{id}/docks`)

### 검토 범위
- `WarehouseVisitorController`(GET `/warehouses/{id}`, GET `/warehouses/{id}/docks`)
- `VisitorWarehouseService`(창고 단건 조회, 도크 목록 조회 - 비활성 창고는 둘 다 404)
- `WarehouseResponse`, `DockResponse` DTO
- `DockRepository.findByWarehouseIdAndDeletedAtIsNull` 추가

### 검토 결과 (승인)
- 두 엔드포인트 모두 `콕배정-API.yml`의 경로/메서드/상태코드(200/404)와 정확히 일치.
- `WarehouseResponse`(id, name), `DockResponse`(id, name, size, status, hasLeveler, hasDockSeal, supportsColdChain, supportsHazmat)가 `설계_참고자료.md` DTO 표와 정확히 일치. 관리자용 DTO의 `active`/`warehouseId` 등 내부 정보가 전혀 노출되지 않음.
- `VisitorWarehouseService`가 `findActiveWarehouseOrThrow()`를 공유해 창고 단건 조회/도크 목록 조회 양쪽 모두에서 창고가 없거나 비활성(`deleted_at IS NOT NULL`)이면 동일하게 `WAREHOUSE_NOT_FOUND`(404) - 에러코드 표의 "창고 없음/비활성화" 요구사항을 정확히 충족. 관리자용 `WarehouseService`(PR #17)는 의도적으로 이 필터가 없어 재활성화 대상을 볼 수 있는 것과 대비되며, 두 서비스의 차이는 복붙 실수가 아니라 요구사항 차이에 따른 의도된 분리로 확인.
- 도크 목록은 `findByWarehouseIdAndDeletedAtIsNull`로 도크 자신의 `deleted_at` 기준으로만 필터링 - 창고 비활성화 연쇄가 아니라 도크 자체 소프트 딜리트 기준이라는 API yml 설명과 일치.
- `JwtAuthenticationFilter`는 `SecurityConfig`에서 `/admin/*` 패턴에만 등록되어 있어 `/warehouses/**`는 별도 코드 없이 자연히 무인증 - 실제 curl로 확인.
- 별도 `VisitorWarehouseService`로 관리자용 서비스와 분리한 것은 공유 서비스에 `if (isAdmin)` 분기를 넣는 것보다 계층을 깔끔하게 유지하는 선택으로 판단. 생성자 주입, `@Transactional(readOnly = true)` 클래스 레벨 패턴도 기존 서비스들과 일관됨.
- 커밋 4개(DTO -> Repository 쿼리 -> Service -> Controller)를 각각 checkout해 `./gradlew compileJava --no-daemon`으로 개별 빌드 확인 - 4개 전부 독립적으로 컴파일 성공.

### 독립 검증
- `docker compose up -d db`(POSTGRES_DB/USER/PASSWORD를 kokbaejeong/kokbaejeong_user/change_me로 커맨드라인 환경변수 직접 전달, `.env` 생성 없음)로 Postgres를 띄우고 백엔드를 환경변수로 직접 `bootRun`.
- 활성 창고 1개, 비활성 창고 1개(`deleted_at` 설정), 활성 창고 아래 활성 도크 1개 + 비활성 도크 1개를 SQL로 직접 시딩.
- PR 본문이 주장한 6가지 시나리오를 Authorization 헤더 없이 curl로 전부 재현해 통과 확인: 활성 창고 조회 200, 비활성 창고 조회 404 WAREHOUSE_NOT_FOUND, 없는 창고 조회 404, 활성 창고 도크 목록 200(활성 도크만 노출), 비활성 창고 도크 목록 404, 전 요청 무인증 통과.
- 도크 목록 응답 시간 6ms 수준으로 REQ-NFR-001(2초 이내) 충족.
- `./gradlew compileJava --no-daemon` 빌드 성공 확인.
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드 프로세스 종료, `.env`는 생성하지 않음, `git status` clean 확인.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(비숫자 `{id}` 경로 변수 처리 시 응답 포맷이 앱 공통 에러 포맷과 다름)은 `docs/후속작업.md`에 기록.

## 2026-09-09 - PR #20: feat: add dock assignment endpoint

- 이슈: #7 [Feature] 도크 배정 API (동시성 처리)
- 브랜치: `feat/7-assignment-create` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-003/REQ-NFR-002/REQ-NFR-004, `설계_참고자료.md`(에러코드 표, DTO 표), `콕배정-API.yml`(`/assignments` POST), `콕배정-DB.dbml`(assignment 부분 유니크 인덱스), `01_기획_DB`/`02_설계_DB`(요구사항_정의서_v1~v4, 설계_v1~v5)

### 검토 범위
- `AssignmentCreateRequest`/`AssignmentResponse` DTO
- `AssignmentService.create()`(도크 조회 -> 창고 활성 검증 -> 도크 AVAILABLE 검증 -> PIN 해시 -> 저장, `DataIntegrityViolationException` -> `ASSIGNMENT_CONFLICT` 변환)
- `AssignmentController`(`POST /assignments`)

### 판단이 필요했던 두 가지 결정에 대한 검토
1. **존재하지 않는 dockId -> `DOCK_NOT_AVAILABLE`(400)**: `콕배정-API.yml`의 `/assignments` POST는 응답으로 201/400/403/409만 정의하고 404가 없으며, 400 설명 자체가 "입력값 오류 또는 도크 사용 불가"로 되어 있어 "도크가 없음"과 "도크를 쓸 수 없음"을 같은 400 버킷으로 묶는 것이 문서상 의도와 일치함. `설계_참고자료.md`/`설계_v3`/`설계_v4` 에러코드 표에도 이 엔드포인트에 404 항목이 없음을 재확인. PR의 판단이 타당함.
2. **`dock.status`가 배정 생성 시 자동으로 OCCUPIED 전환되지 않음**: 반박 근거를 찾기 위해 `01_기획_DB`/`02_설계_DB`의 v1~v5 버전을 모두 확인함. 오히려 `설계_v2`의 "사용자 결정 사항"에 "도크 상태 갱신 방식: 자동 반영 없음. 배정 실패(409) 응답 수신 시에만 안내 문구로 처리"라는 명시적 결정 문장을 발견 - PR의 구현을 직접 뒷받침하는 근거임. 반박 근거는 발견되지 않음. `docs/후속작업.md`에 프론트 작업 전 재확인 권장 사항으로 기록.

### 검토 결과 (승인)
- `AssignmentCreateRequest`(dockId @NotNull, driverName @NotBlank, scheduledTime 선택, pin @NotBlank @Pattern 4자리 숫자), `AssignmentResponse`(id, dockId, status)가 `설계_참고자료.md`/`설계_v5` DTO 표와 정확히 일치.
- PIN은 `PasswordEncoder`(PR #16에서 정의된 BCrypt 빈)로 `encode()`한 해시만 저장, 평문 저장 없음 - REQ-NFR-002 충족.
- `scheduledTime`이 `null`이면 `LocalDateTime.now()`로 채우는 로직이 `설계_참고자료.md`/`요구사항_정의서_v1`(도착 예정 시각 기본값=현재)와 일치.
- `assignmentRepository.save()`를 감싼 `DataIntegrityViolationException` 캐치는 도크를 이미 `findById`로 로드해 FK 위반 가능성이 없고, 필수 컬럼은 컨트롤러 레벨 Bean Validation(`@NotNull`/`@NotBlank`)이 먼저 걸러 DB의 not-null 제약에 도달하기 전에 400으로 처리되므로, 실질적으로 이 catch 블록에 도달할 수 있는 유일한 제약은 `assignment(dock_id, status='ACTIVE')` 부분 유니크 인덱스로 판단 - 다른 무결성 오류를 잘못 삼킬 위험은 낮음.
- 창고 비활성 검증(`WAREHOUSE_INACTIVE`)을 도크 AVAILABLE 검증보다 먼저 수행 - `요구사항_정의서_v4`의 "창고가 비활성화 상태이면 하위 도크의 상태와 무관하게 신규 배정을 거부한다"와 순서까지 일치.
- 커밋 3개(DTO -> Service -> Controller)를 각각 `git checkout`해 `./gradlew compileJava --no-daemon`으로 개별 빌드 확인 - 3개 전부 독립적으로 컴파일 성공.

### 독립 검증
- `docker compose up -d db`(POSTGRES_DB/USER/PASSWORD를 kokbaejeong/kokbaejeong_user/change_me로 커맨드라인 환경변수 직접 전달)로 Postgres를 띄우고 백엔드를 환경변수로 직접 `bootRun`.
- 활성 창고 1개, 비활성 창고 1개, AVAILABLE 도크 2개(1개는 동시성 테스트 전용), MAINTENANCE 도크 1개, 비활성 창고 소속 AVAILABLE 도크 1개를 SQL로 시딩.
- 순차 시나리오 6종을 curl로 재현해 전부 통과 확인: 정상 배정 201 / 같은 도크 순차 재요청 409 ASSIGNMENT_CONFLICT / MAINTENANCE 도크 400 DOCK_NOT_AVAILABLE / 비활성 창고 도크 403 WAREHOUSE_INACTIVE / 존재하지 않는 dockId 400 DOCK_NOT_AVAILABLE / PIN 형식 오류 400 VALIDATION_ERROR.
- DB 직접 조회로 `scheduled_time`이 요청 처리 시각과 일치함, `pin_hash`가 BCrypt 형식(`$2a$10$...`)임, 배정 후에도 `dock.status`가 여전히 `AVAILABLE`로 남아있음(자동 전환 없음)을 확인.
- **동시성 재현(핵심)**: 새 AVAILABLE 도크 1개에 백그라운드 curl 두 건을 `&`+`wait`로 진짜 동시에 전송 - 하나는 201(`{"id":3,"dockId":4,"status":"ACTIVE"}`), 하나는 409 ASSIGNMENT_CONFLICT를 받았고, 이후 DB에서 해당 도크에 대한 ACTIVE 배정이 정확히 1건임을 `SELECT`로 확인.
- `./gradlew compileJava --no-daemon` 빌드 성공 확인.
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드 프로세스 종료, `.env`는 생성하지 않음, `git status` clean 확인, `main` 브랜치로 복귀.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(`dock.status` 자동 미전환은 설계_v2의 명시적 결정과 일치하나, Vue 배정 화면 구현 전 재확인 권장)은 `docs/후속작업.md`에 기록.

## 2026-09-09 - PR #21: feat: 배정 취소 API 추가

- 이슈: #8 [Feature] 배정 취소 API
- 브랜치: `feat/8-assignment-cancel` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-004/REQ-NFR-002, `설계_참고자료.md`(에러코드 표, DTO 표), `콕배정-API.yml`(`/assignments/{id}/cancel`), `02_설계_DB`(설계_v2~v5)

### 검토 범위
- `AssignmentCancelRequest`(pin, @NotBlank) DTO
- `AssignmentService.cancel(id, request)`(id 조회 실패 시 PIN_MISMATCH -> PIN 검증 -> ALREADY_CANCELLED 체크 -> `assignment.cancel()`)
- `AssignmentController`(`POST /assignments/{id}/cancel`)

### 판단이 필요했던 두 가지 결정에 대한 검토
1. **존재하지 않는 assignment id -> `PIN_MISMATCH`(401)**: `콕배정-API.yml`의 `/assignments/{id}/cancel`은 200/400/401만 정의하고 404가 없음. `설계_참고자료.md`와 `02_설계_DB`의 `설계_v3`/`설계_v4`(둘 다 상태: 확정) 에러코드 표에서도 이 엔드포인트에는 PIN_MISMATCH/ALREADY_CANCELLED 두 개만 있고 NOT_FOUND 계열이 전혀 없음을 확인 - 같은 문서에서 창고/도크 엔드포인트는 WAREHOUSE_NOT_FOUND/DOCK_NOT_FOUND를 명시적으로 갖고 있는 것과 대비되어, cancel 엔드포인트에 404가 없는 것은 누락이 아니라 의도된 설계로 판단됨. PR #20에서 존재하지 않는 dockId를 DOCK_NOT_AVAILABLE(400)로 처리한 것과 동일한 패턴. v1~v5 전체를 확인했으나 반박 근거는 발견되지 않음. PR의 판단이 타당함.
2. **PIN 검증을 ALREADY_CANCELLED 체크보다 먼저 수행**: `설계_참고자료.md`의 공통 원칙("401은 인증 실패, 403은 인증 후 권한/상태 거부, 400은 요청 자체 문제...")은 코드별 HTTP 상태 분류 기준이지 동일 엔드포인트 내 체크 순서를 규정하지 않아 이 판단과 모순되지 않음. 틀린 PIN으로 배정 상태(취소 여부)를 알아낼 수 없게 하는 순서는 보안 관점에서 합리적.

### 검토 결과 (승인)
- `AssignmentCancelRequest`(pin @NotBlank)가 `설계_참고자료.md`/`설계_v5` DTO 표와 정확히 일치.
- `AssignmentService.create()` 로직은 이번 PR에서 전혀 변경되지 않음 - `cancel()` 메서드만 추가된 clean addition으로 확인.
- PIN 비교는 `PasswordEncoder.matches()`(BCrypt, PR #16에서 정의된 빈)를 재사용 - 평문 비교 없음(REQ-NFR-002 충족), PR #20의 PIN 해시 저장 방식과 일관.
- 커밋 3개(DTO -> Service -> Controller)를 각각 `git checkout`해 `./gradlew compileJava --no-daemon`으로 개별 빌드 확인 - 3개 전부 독립적으로 컴파일 성공.

### 독립 검증
- `docker compose up -d db`(POSTGRES_DB/USER/PASSWORD를 커맨드라인 환경변수로 직접 전달)로 Postgres를 띄우고 백엔드를 환경변수로 직접 `bootRun`.
- 창고 1개, AVAILABLE 도크 1개를 SQL로 시딩 후 `POST /assignments`로 배정 생성(pin=1234, id=1).
- PR 본문이 주장한 5가지 시나리오를 curl로 전부 재현해 통과 확인: 틀린 PIN 401 PIN_MISMATCH / 올바른 PIN 200 CANCELLED / 재취소 400 ALREADY_CANCELLED / 없는 id(999) 401 PIN_MISMATCH / 취소 후 같은 도크에 새 배정 201 ACTIVE(부분 유니크 인덱스가 CANCELLED는 막지 않음을 재확인, PR #20 동작 훼손 없음).
- DB 직접 조회로 `pin_hash`가 BCrypt 형식(`$2a$10$...`)임과 `cancelled_at`이 취소 시점에 채워짐을 확인.
- `./gradlew compileJava --no-daemon` 빌드 성공 확인.
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드 프로세스 종료, `.env`는 생성하지 않음, `git status` clean 확인, `main` 브랜치로 복귀.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(존재하지 않는 assignment id의 401 처리는 PR #20의 유사 판단과 같은 패턴으로 문서상 근거는 있으나, 확정 근거는 아니므로 확인 참고용으로 `docs/후속작업.md`에 기록)은 해당 문서에 기록.

## 2026-09-09 - PR #22: feat: Vue 배정/취소 화면 구현

- 이슈: #9 [Feature] Vue 배정 화면 구현
- 브랜치: `feat/9-vue-assignment-screen` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-001/002/003/004, REQ-NFR-003/005, `설계_참고자료.md`(아키텍처/CORS), `콕배정-API.yml`, `docs/작업계획서.md` §2-4, `docs/후속작업.md`(PR #20 - dock.status 자동 미반영 결정)

### 검토 범위
- `AssignView.vue`(SCR-ASSIGN-001), `CancelView.vue`(SCR-ASSIGN-002), 라우터 추가, `style.css` 정리
- `CorsConfig`(신규 `WebMvcConfigurer`), `app.cors.allowed-origins`, `.env.example`/`docker-compose.yml`/`README_DEPLOY.md`

### 블로킹으로 발견되어 같은 PR에서 직접 수정
`docker-compose.yml`의 `CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}`(수정 전)은 이 PR 이전 버전의 `.env.example`로 만든 구버전 `.env`(해당 줄 없음)를 쓰는 로컬 환경에서, docker compose가 미정의 변수를 **빈 문자열로 치환**해 컨테이너에 전달함을 직접 재현 확인(`docker compose config` -> `CORS_ALLOWED_ORIGINS: ""`, 경고 로그 동반). Spring의 `${CORS_ALLOWED_ORIGINS:기본값}` 플레이스홀더는 변수가 "존재하되 비어있는" 경우 기본값을 적용하지 않아(완전 부재일 때만 적용) `app.cors.allowed-origins=""` -> `"".split(",")` -> `[""]`가 되고, 실제 컨테이너에 curl로 CORS preflight를 보내 **모든 origin이 403 "Invalid CORS request"로 차단됨**을 확인. 환경변수 자체를 컨테이너에 전달하지 않은 대조군에서는 Spring 기본값이 정상 적용되어 200이 나옴을 함께 확인해, 원인이 docker-compose의 미정의 변수 보간 방식임을 특정. 이 PR이 고치려던 CORS 미설정 문제를 기존 `.env`를 가진 개발자 환경에서 조용히 재발시킬 수 있는 실질적 블로킹 리스크로 판단.
- 수정: `${CORS_ALLOWED_ORIGINS}` -> `${CORS_ALLOWED_ORIGINS:-http://localhost:5173,http://localhost:3000}`(docker-compose 자체 기본값 문법, Spring 쪽 기본값과 동일하게 맞춤). 같은 스테일 `.env` 시나리오로 재검증 - 컨테이너 env 기본값 적용, CORS preflight 200 정상 확인. 커밋 `eafc47d`로 같은 PR 브랜치에 추가.

### CORS 설정 자체 검토
- `addCorsMappings("/**")`, `allowedMethods("GET","POST","PUT","DELETE")`, `allowedHeaders("*")` - `콕배정-API.yml` 전체 확인 결과 `DELETE`를 쓰는 엔드포인트는 없음(관리자 비활성화/활성화도 전부 POST). 미사용이나 위험 없는 사소한 군더더기로 판단, 블로킹 아님.
- `allowCredentials(true)` 미설정 - `api/client.js`에 쿠키/`credentials` 사용 없음, `AdminLoginView.vue`는 아직 placeholder(PR #16은 백엔드 JWT만 구현)라 현재 시점에 쿠키 기반 인증 경로 자체가 없음을 확인. `Authorization: Bearer` 헤더 방식이므로 credentials 모드 불필요하다는 판단이 유효함.
- CORS를 별도 이슈로 분리하지 않고 이 PR에 포함한 것: 직접 로컬 브라우저 테스트 중 발견됐고 다른 작업을 블로킹하지 않으며 이 화면 자체가 CORS 없이 동작 불가능한 관계라 타당하다고 판단. "13개 이슈 중 CORS 전용 이슈가 없었던 것은 기획 단계의 누락"이라는 점은 `docs/후속작업.md`에 별도 기록.

### 독립 UI 검증 (Playwright + 시스템 Chrome, 모바일 뷰포트 390x844)
`docker compose up -d db` + 백엔드 `bootRun` + 프론트 `npm run dev`(`VITE_API_BASE_URL=http://localhost:8080`, dev server 5173 -> 백엔드 8080 직접 크로스오리진 호출)로 전체 스택을 띄우고 실제 브라우저로 조작:
- `/w/1` 진입 - 창고명 + 도크 2개(AVAILABLE 1, MAINTENANCE 1) 정상 렌더링, MAINTENANCE 카드 `disabled` 확인, 실제 크로스오리진 fetch 성공으로 CORS 정상 동작 확인
- AVAILABLE 도크 선택 -> 폼 작성(`datetime-local` 값 `"2026-09-10T14:30"` 그대로 제출, 백엔드 `LocalDateTime` 역직렬화 문제없이 수락 확인) -> 배정 성공(배정 번호 1) + 취소 링크 노출 -> 배정 후에도 도크 목록의 A-01이 여전히 "배정 가능"으로 남아있음을 확인(자동 새로고침 없음, PR #20 기록과 일치, diff 상 숨은 재조회 호출 없음도 확인)
- 취소 화면 이동 -> 틀린 PIN -> 에러 노출 -> 올바른 PIN -> 취소 성공 메시지
- 존재하지 않는 창고(`/w/9999`) -> 에러 + "다시 시도" 버튼 -> 클릭 시 실제 재요청(네트워크 요청) 발생 확인 (REQ-NFR-005)
- 콘솔에는 의도된 401/404 네트워크 로그만 있고 처리되지 않은 예외/Vue 경고 없음

### 빌드/커밋 단위 검증
- `cd frontend && npm run build`, `cd backend && ./gradlew compileJava --no-daemon` 각각 성공
- 4개 커밋(스타일 정리 -> 배정 화면 -> 취소 화면 -> CORS) 각각 `git checkout` 후 프론트/백엔드 빌드 개별 성공 확인
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드/프론트 프로세스 종료, `.env`는 제거, `git status` clean 확인, `main` 브랜치로 복귀

### 최종 판정
승인. 블로킹으로 발견된 docker-compose 기본값 문제는 같은 PR(`eafc47d`)에서 직접 수정 후 재검증 완료. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(CORS 전용 이슈가 원래 13개 이슈 목록에 없었던 기획 단계 누락, `DELETE` 메서드 미사용)은 `docs/후속작업.md`에 기록.

## 2026-09-09 - PR #23: feat: 관리자 화면 구현

- 이슈: #10 [Feature] 관리자 화면 구현
- 브랜치: `feat/10-admin-screens` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-005/006/007, `콕배정-API.yml`(`/admin/login`, `/admin/warehouses`, `/admin/docks` 및 하위 경로), `docs/작업계획서.md` §3-1

### 검토 범위
- `adminClient.js`(토큰 저장/조회, `adminFetch` 헤더 자동 부착), `AdminLoginView.vue` 실 구현, `AdminWarehouseListView/FormView.vue`, `AdminDockListView/FormView.vue`, 라우터 6개 경로 + `router.beforeEach` 인증 가드
- `콕배정-API.yml` 스키마(`AdminLoginRequest`, `TokenResponse`, `WarehouseCreateRequest`/`WarehouseAdminResponse`, `DockCreateRequest`/`DockUpdateRequest`/`DockAdminResponse`) 대 실제 요청/응답 필드명 전부 대조 - 일치 확인. `GET /admin/docks`의 필수 `warehouseId` 쿼리 파라미터도 정확히 반영됨.

### 번들된 버그 수정 2건 독립 검증
1. **`JwtAuthenticationFilter`가 `OPTIONS` 요청을 인증 검사 없이 통과시키도록 수정**: curl로 직접 재현 - `OPTIONS /admin/warehouses`(Authorization 헤더 없음, CORS preflight 헤더 포함)는 200 + `Access-Control-*` 헤더만 반환하고 `Content-Length: 0`으로 실제 데이터가 없음을 확인. 같은 조건에서 `GET`/`POST`는 여전히 401 `AUTH_FAILED`로 막힘을 확인. 미인증 `POST /admin/warehouses`로 창고 생성을 시도한 뒤 DB를 직접 조회해 실제로 레코드가 생성되지 않았음을 확인. `JwtAuthenticationFilter`는 `FilterRegistrationBean`으로 등록된 순수 서블릿 필터이고, CORS는 `WebMvcConfigurer`(`CorsConfig`) 기반이라 `DispatcherServlet`의 `HandlerMapping` 단계에서 처리되므로, OPTIONS를 필터에서 통과시켜도 실제 컨트롤러 메서드(POST/PUT/GET 핸들러)가 호출되지 않는 구조임을 코드와 동작 양쪽으로 확인. 보안 홀 없음.
2. **`api/client.js`의 `apiFetch` 헤더 병합 순서를 `{ ...options, headers: {...} }`로 수정**: 기존 호출부(`AssignView.vue`, `CancelView.vue`) 전부를 확인한 결과 커스텀 헤더를 넘기는 곳이 없어 동작 영향 없음. `adminClient.js`의 `adminFetch`가 넘기는 `{ Authorization, ...options.headers }`가 `apiFetch` 내부에서 `{ 'Content-Type': 'application/json', ...options.headers }`로 다시 병합되어, 기본 `Content-Type`과 호출자가 넘긴 `Authorization`(혹은 명시적으로 재정의한 `Content-Type`)이 모두 살아남는 순서임을 확인.

### 실제 화면 독립 검증 (Playwright + 시스템 Chrome, 모바일 뷰포트 390x844)
`docker compose up -d db` + 백엔드 `bootRun`(환경변수 직접 전달) + 프론트 `npm run dev`(`VITE_API_BASE_URL=http://localhost:8080`, 5173→8080 직접 크로스오리진)로 전체 스택을 띄우고 bcrypt 해시로 관리자 계정을 직접 시딩 후 실제 브라우저로 전 과정 조작:
- 로그인 - 틀린 비밀번호 시 에러 문구 노출 및 로그인 화면 유지 확인, 올바른 비밀번호 시 `/admin/warehouses`로 리다이렉트 확인
- 창고 등록 → 목록 반영 확인, 창고 수정 → 목록에 변경된 이름 반영 확인
- 도크 관리 진입 → 도크 등록 → 목록 반영, 도크 수정 → 변경된 이름 반영
- 도크 비활성화 → 배지 "비활성" 전환 확인 → 재활성화 → 배지 "활성" 복귀 확인
- **ACTIVE 배정이 있는 도크의 비활성화 차단(PR 자체 테스트 목록에는 없던 경로, 별도 검증)**: 방문자 `POST /assignments`로 해당 도크에 ACTIVE 배정을 시딩한 뒤 관리자 화면에서 비활성화를 시도 - 요청이 400으로 거부되고 "활성 배정이 있는 도크는 비활성화할 수 없습니다."라는 `DOCK_HAS_ACTIVE_ASSIGNMENT` 에러 메시지가 화면에 실제로 노출됨을 확인(침묵 실패 아님), 도크는 계속 "활성" 상태로 남음
- 로그아웃 → 로그인 화면 이동 확인
- 로그아웃 상태에서 `/admin/warehouses` 직접 URL 접근 → `router.beforeEach` 가드가 `/admin`으로 리다이렉트함을 확인
- 전 과정 `page.on('console')`/`page.on('pageerror')` 확인 - 의도된 401/400 네트워크 로그 외 처리되지 않은 JS 에러 없음

### 빌드/커밋 단위 검증
- `frontend: npm run build`, `backend: ./gradlew compileJava --no-daemon` 각각 성공
- 커밋 7단계(토큰 헬퍼 → 공통 스타일 → 로그인 → 창고 화면 → 도크 화면 → CORS 필터 수정 → 헤더 병합 수정) 순서 확인
- `v-html` 사용 없음(전체 grep으로 확인), `AssignView.vue`/`CancelView.vue`(PR #22)와 동일한 `<script setup>` Composition API 스타일 일관성 확인

### 독립 검증 환경
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드/프론트 프로세스 종료, 임시로 만든 `.env`는 삭제, `git status` clean 확인, `main` 브랜치로 복귀

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(관리자 수정 폼의 비동기 초기값 로드가 빠른 사용자 입력을 덮어쓸 수 있는 이론적 레이스 컨디션)은 `docs/후속작업.md`에 기록.

## 2026-09-09 - PR #24: feat: 창고 혼잡도 AI 요약 기능 추가

- 이슈: #11 [Feature] 창고 혼잡도 AI 요약 기능 (REQ-FUNC-008, Should - 시간 부족 시 최우선 제외 대상이었으나 드롭되지 않고 구현됨)
- 브랜치: `feat/11-ai-congestion-summary` → `main`
- 근거 문서: `기능명세서.md` REQ-FUNC-008, `설계_참고자료.md`(외부 연동 아키텍처 - "별도 저장 없이 요청 시점에 집계 후 외부 LLM API 호출"), `콕배정-API.yml`(`/admin/warehouses/{id}/summary`), `docs/작업계획서.md` §3-2, `docs/후속작업.md`(PR #20 - dock.status 자동 미반영 결정)

### 검토 범위
- `WarehouseSummaryResponse`(warehouseId, occupancyRate, summary), `WarehouseSummaryService`, `WarehouseAdminController`(`GET /admin/warehouses/{id}/summary` 추가), `AdminWarehouseListView.vue`(혼잡도 요약 버튼/렌더링)

### occupancyRate 계산 기준 검토 (이번 PR의 핵심 판단)
`콕배정-API.yml`, `설계_참고자료.md`, `02_설계_DB`의 `설계_v1`~`설계_v5` 전체를 `textutil -convert txt -stdout`으로 변환해 확인했으나, occupancyRate을 dock.status 기준으로 계산할지 실제 ACTIVE 배정 기준으로 계산할지를 명시한 문서는 어디에도 없다 - `설계_v2`는 "점유 데이터 집계 후 요청"이라는 아키텍처 다이어그램 수준 서술만, `설계_v3`는 API 스펙(`200: { warehouseId, occupancyRate, summary }`, 도크 없으면 occupancyRate=0 + 기본 문구)만 있고 계산 방법 자체는 어느 버전에도 기재되어 있지 않다. 다만 PR #20 리뷰에서 확인되어 `docs/후속작업.md`에 기록된 `설계_v2` "사용자 결정 사항"("도크 상태 갱신 방식: 자동 반영 없음")에 따르면, dock.status 기준으로 계산할 경우 배정이 아무리 쌓여도 점유율이 항상 0으로 나오는 명백한 오류가 발생한다. 이 PR이 실제 ACTIVE 배정 수 / 활성 도크 수로 정의한 것은 문서상 반박 근거가 없고, 유일하게 정확한 값을 낼 수 있는 방법이라는 점에서 PR #20/#21과 같은 계열의 타당한 추론적 판단으로 확인했다. `AssignmentRepository.existsByDockIdAndStatus(dockId, ACTIVE)`를 도크마다 호출하는 N+1 스타일 쿼리이나, 이 프로젝트 규모(창고당 도크 수가 매우 적음)에서는 실질적 문제가 되지 않아 비차단으로 판단, `docs/후속작업.md`에 참고용으로만 기록.

### LLM 호출 실패 폴백 검토
`generateSummary()`가 `chatClient.prompt().user(prompt).call().content()`를 `catch (RuntimeException e)`로 감싸 집계값 기반 문장으로 대체하는 구조. 모든 `RuntimeException`을 잡는 것은 원론적으로 과도할 수 있어(예: 프롬프트 구성 과정의 코드 버그가 외부 API 장애로 오인될 위험), `spring-ai-retry:1.1.4`(이 프로젝트가 쓰는 `springAiVersion`) jar를 직접 열어 확인한 결과 `TransientAiException`/`NonTransientAiException`이 존재하나 둘 다 `RuntimeException`을 직접 상속하고 공통 상위 타입이 없어, 더 좁히려면 멀티캐치(`catch (TransientAiException | NonTransientAiException e)`)가 필요함을 확인했다. 3일짜리 프로젝트에서 블로킹할 정도는 아니라고 판단, `docs/후속작업.md`에 비차단 항목으로 기록.
실제 `OPENAI_API_KEY`가 없는 환경이라 정상 호출 경로는 이 리뷰에서도 검증하지 못했음 - 이는 PR 본문에도 정직하게 공개되어 있음을 확인. 폴백 경로는 아래 독립 검증에서 직접 재현해 정확히 동작함을 확인했다.

### `WarehouseAdminController`의 서비스 중복 주입 검토
`WarehouseAdminController`가 `WarehouseService`와 `WarehouseSummaryService`를 함께 주입받고, `WarehouseSummaryService`는 `WarehouseService.findWarehouseOrThrow()`(private)를 재사용하지 않고 `warehouseRepository.findById()`를 자체적으로 다시 호출한다. `WarehouseService`의 해당 메서드가 private이라 리팩터링 없이는 재사용이 불가능하고, 기존 코드베이스의 다른 서비스들도 각자 자신의 리포지토리 조회를 갖는 패턴이라 이번 PR만의 새로운 문제는 아님. 비차단으로 판단, `docs/후속작업.md`에 참고용으로 기록.

### 독립 검증
- `lsof -ti:8080,5173 | xargs -r kill -9` 후 `docker compose up -d db`(POSTGRES_DB/USER/PASSWORD 환경변수 직접 전달), 백엔드를 `SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD`/`OPENAI_API_KEY=sk-dummy`/`JWT_SECRET`(랜덤 생성) 환경변수로 직접 `bootRun`.
- 창고 3개를 시딩: 도크 0개(빈창고), 도크 2개/배정 0개(한산창고), 도크 2개/배정 1개(바쁜창고 - 방문자 `POST /assignments`로 실제 ACTIVE 배정 생성). 관리자 계정은 `spring-security-crypto`/`spring-jcl` jar + jshell로 bcrypt 해시 생성 후 SQL로 직접 시딩.
- `/admin/warehouses/{id}/summary` 5개 시나리오 curl로 재현:
  - 빈창고(도크 0개) → 200, `{occupancyRate:0.0, summary:"등록된 도크가 없어 혼잡도를 계산할 수 없습니다."}`, 응답 시간 **~26ms**(즉시 - LLM 호출 스킵 확인)
  - 한산창고(도크 2/배정 0) → 200, `{occupancyRate:0.0, summary:"전체 활성 도크 2개 중 0개가 배정되어 점유율은 0%입니다."}`
  - 바쁜창고(도크 2/배정 1) → 200, `{occupancyRate:0.5, summary:"전체 활성 도크 2개 중 1개가 배정되어 점유율은 50%입니다."}`, 응답 시간 **~658ms**(빈창고 대비 뚜렷이 느림 - 더미 키로 실제 OpenAI에 네트워크 호출을 시도한 뒤 실패해 폴백으로 전환됨을 응답 시간으로 간접 확인)
  - 없는 창고(999) → 404 `WAREHOUSE_NOT_FOUND`
  - 무토큰 → 401 `AUTH_FAILED`
- Playwright(`playwright-core`, 시스템 Chrome `channel:'chrome'`, headless, 모바일 뷰포트 390x844)로 `npm run dev`(`VITE_API_BASE_URL=http://localhost:8080`) 프론트에서 관리자 로그인 → 창고 목록에서 "혼잡도 요약" 버튼 클릭 → 빈창고 "점유율 0% - 등록된 도크가 없어..." / 바쁜창고 "점유율 50% - 전체 활성 도크 2개 중 1개가 배정되어..."가 화면에 정확히 렌더링됨을 스크린샷으로 확인, `page.on('console'/'pageerror')` 전 과정 에러 없음.

### 빌드/커밋 단위 검증
- `cd backend && ./gradlew compileJava --no-daemon`, `cd frontend && npm run build` 각각 성공
- 4개 커밋(DTO → Service → Controller → 프론트 렌더링) 각각 개별 `git checkout` 후 `./gradlew compileJava --no-daemon`으로 독립 빌드 성공 확인

### 독립 검증 환경
- 검증에 사용한 Postgres 컨테이너/볼륨은 `docker compose down -v`로 완전히 제거, 백엔드/프론트 프로세스 종료, `.env`는 생성하지 않음, `git status` clean 확인, `main` 브랜치로 복귀

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(LLM 호출 실패 시 `RuntimeException` 캐치 범위가 넓은 점, `existsByDockIdAndStatus` N+1 스타일 쿼리, `WarehouseSummaryService`의 별도 창고 조회, 실제 `OPENAI_API_KEY`로 정상 호출 경로 사전 검증 필요)은 `docs/후속작업.md`에 기록.

## 2026-09-09 - PR #25: chore: 수동 테스트 시나리오 전체 실행 및 결과 기록

- 이슈: #12 [Chore] 수동 테스트 시나리오 전체 실행
- 브랜치: `chore/12-manual-test-pass` → `main`
- 근거 문서: `03_개발 컨벤션.html`의 "테스트 전략(수동 시나리오)" 표, `docs/작업계획서.md` §3-3

### 검토 범위
- `docs/수동테스트결과.md` 신규 추가(문서 전용 PR, `git diff origin/main...HEAD --stat`로 이 파일 1개만 변경됐음을 확인). 이슈 #1~#11이 모두 머지된 `main` 기준으로 6개 시나리오를 하나의 연속 세션에서 재검증했다는 주장을 검증.

### 독립 재현 (문서를 신뢰하지 않고 처음부터 재구성)
`lsof -ti:8080,5173 | xargs -r kill -9` → `docker compose up -d db` → 백엔드 `bootRun`(`SPRING_DATASOURCE_*`, `OPENAI_API_KEY=sk-dummy`, `JWT_SECRET` 직접 전달) → 프론트 `npm run dev`(`VITE_API_BASE_URL=http://localhost:8080`). 활성 창고(서울1센터, 도크 A1/A2 AVAILABLE), 비활성 창고(비활성창고, 도크 B1), 도크 없는 창고(도크없는창고), 관리자 계정을 `spring-security-crypto`+`spring-jcl` jar와 jshell로 bcrypt 해시 생성 후 직접 시딩.

6개 시나리오 전부 재현, 상태 코드/에러 코드/메시지 문구가 문서와 정확히 일치, 회귀 없음:
1. **동시 배정 충돌**: A1에 백그라운드 curl 2건 동시 전송 → 201/409 정확히 하나씩, `assignment` 테이블에 ACTIVE 행 정확히 1건(DB 직접 조회로 확인).
2. **PIN 불일치**: 성공한 배정을 잘못된 PIN으로 취소 → `401 {"code":"PIN_MISMATCH"}`.
3. **비활성 창고 신규 배정 차단**: 비활성 창고 소속 도크(B1)에 배정 시도 → `403 {"code":"WAREHOUSE_INACTIVE"}`.
4. **ACTIVE 배정 있는 도크 비활성화 차단**: Playwright(`playwright-core`, 시스템 Chrome, 모바일 뷰포트 390x844)로 관리자 로그인 → A1 비활성화 클릭 → `400 DOCK_HAS_ACTIVE_ASSIGNMENT`, 화면에 "활성 배정이 있는 도크는 비활성화할 수 없습니다." 에러 문구가 실제로 렌더링됨을 스크린샷으로 확인, A1은 "활성" 배지 유지.
5. **비활성 도크 노출 여부(교차 기능)**: 같은 세션에서 이어서 A2(배정 없음) 비활성화 → 성공 → 방문 기사 화면(`/w/1`) 재조회 → A1만 노출, A2 제외됨을 스크린샷으로 확인. 문서가 "기능 간 상호작용까지 이어서 확인"했다고 주장한 부분이 실제로 재현됨(별도 세션이 아니라 관리자 조작 직후 같은 브라우저 컨텍스트로 검증).
6. **AI 요약 데이터 없음**: 도크 없는 창고 조회 → `occupancyRate:0.0`, `"등록된 도크가 없어 혼잡도를 계산할 수 없습니다."`, 응답 ~13ms. 비교군(도크 있는 창고)은 ~914ms(더미 키로 실제 OpenAI 호출 시도 후 폴백) - 응답 시간 차이로 LLM 호출 스킵 여부를 간접 확인.

### 문서 정확성 검토
- 표의 6개 항목이 `03_개발 컨벤션.html`의 "테스트 전략(수동 시나리오)" 표와 1:1 대응됨을 확인.
- DoD 대조 체크리스트가 "이슈 #1~#11 각 PR 리뷰에서 개별 확인"과 "이번 PR에서 재확인"을 구분해 표기 - 이전 PR에서 검증된 항목을 이번 PR의 성과로 과대 포장하지 않음. `./gradlew compileJava --no-daemon`, `npm run build` 모두 성공해 "빌드가 깨지지 않는다" claim을 별도로 검증.
- "남은 주의사항" 섹션이 `docs/후속작업.md`의 기존 항목(린터 미도입, 예외 타입 미처리 2건, AI 요약 실제 키 미검증)을 "이번 회귀 테스트 범위 밖"이라고 정확히 표기 - 해결된 것으로 오도하지 않음.

### 독립 검증 환경
- `docker compose down -v`로 컨테이너/볼륨 제거, 백엔드/프론트 프로세스 종료, 임시 `.env` 생성하지 않음, `git status` clean 확인, `main` 브랜치로 복귀.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제). 문서 전용 PR이며 코드 드리프트/회귀 없음.

## 2026-09-10 - PR #27: feat: 도크별 점유 현황 대시보드 및 관리자 사이드바 추가

- 이슈: #26 [Feature] 도크별 실시간 점유 현황 대시보드
- 브랜치: `feat/13-dock-occupancy-dashboard` → `main`
- 근거 문서: `기능명세서.md`(REQ-FUNC-008 추적성), `콕배정-API.yml`(`WarehouseSummaryResponse` 스키마), `콕배정_와이어프레임.zip`의 SCR-ADMIN-003, 이슈 #26, `docs/작업계획서.md` §3-2/§6

### 검토 범위
- `WarehouseSummaryResponse`에 `docks: DockOccupancy[]`(dockId, name, status, occupied) 필드 추가, `WarehouseSummaryService.getSummary()`가 도크별 `assignmentRepository.existsByDockIdAndStatus(ACTIVE)` 결과로 `occupied`를 계산
- `AdminSummaryView.vue`(신규, `/admin/summary`), `AdminLayout.vue`(신규, 사이드바 공통 레이아웃), 기존 5개 관리자 화면(`AdminWarehouseListView`/`AdminWarehouseFormView`/`AdminDockListView`/`AdminDockFormView`)에 `AdminLayout` 일괄 적용
- `AdminWarehouseListView.vue`의 각 창고 행에 `{ name: 'admin-summary', query: { warehouseId: w.id } }` 링크 추가(요약 페이지 진입 시 해당 창고 사전 선택)

### SCR-ADMIN-003 대조 (와이어프레임 이미지 직접 확인)
`SCR-ADMIN-003.png`를 압축 해제해 직접 열어 확인. AI 분석 요약 박스 + 도크별 리스트(도크명/바/퍼센트/상태 배지: 사용중·사용가능·점검중) 구성이 이 PR의 `AdminSummaryView.vue`와 핵심 구조가 일치함을 확인했다. 두 가지 차이를 발견해 비차단으로 `docs/후속작업.md`에 기록:
1. 와이어프레임은 도크별 바가 개별 이용률(%)을 표시하나, 구현은 `occupied` 여부에 따른 0%/100% 이진 바 - 현재 데이터 모델이 도크별 과거 이용률을 집계/저장하지 않아 스코프 밖.
2. 와이어프레임 사이드바는 "창고 관리/도크 관리/혼잡도 요약" 3항목이나 구현은 2항목("도크 관리" 없음) - 도크 관리 라우트가 이 PR 이전부터 창고 종속(`/admin/warehouses/:id/docks`)이라 전역 링크를 둘 수 없는 기존 아키텍처 제약이며, 창고 행의 "도크 관리" 링크로 접근은 여전히 가능. `작업계획서.md` §6(이슈 #28)에서 와이어프레임 기준 사이드바 전면 재구성이 예정돼 있음.

PR 본문이 스스로 밝힌 "삭제 확인 팝업(하드 삭제) 미반영"에 대해서도 `콕배정-API.yml`을 재확인 - `/admin/warehouses/{id}`, `/admin/docks/{id}`에 DELETE 메서드가 정의되어 있지 않고 `(de)activate`(소프트 삭제)만 존재함을 확인. PR의 판단이 API 설계와 일치함을 검증했다.

### `occupied` 계산 방식이 `dock.status`와 독립적인 점 검토
`DockOccupancy.occupied`는 `dock.status` 필드가 아니라 `AssignmentRepository.existsByDockIdAndStatus(dockId, ACTIVE)`로 별도 계산된다. 즉 관리자가 도크 상태를 수동으로 "배정 가능"으로 유지해도 실제 ACTIVE 배정이 있으면 `occupied=true`가 나온다 - PR #20/#24 리뷰에서 확정된 "도크 상태 자동 반영 없음" 설계 결정과 일치하는 올바른 구현임을 실제 데이터로 재현해 확인(아래 독립 검증의 A1 사례).

### 독립 재현
- `git worktree add`로 `feat/13-dock-occupancy-dashboard`를 별도 경로에 체크아웃(현재 작업 디렉터리의 `feat/28-admin-ui-vuetify-redesign` 체크아웃은 건드리지 않음).
- `initdb`+`pg_ctl`로 유닉스 소켓 `/tmp` 기반 임시 Postgres(포트 5544) 기동, `SPRING_DATASOURCE_*`/`OPENAI_API_KEY=sk-dummy`/`JWT_SECRET`(랜덤) 환경변수로 `bootRun`.
- `htpasswd -bnBC 10`으로 bcrypt 해시 생성 후 관리자 계정 SQL 직접 시딩, 이후 전부 실제 API로 시딩: 창고 2개(서울1센터/부산2센터), 서울1센터에 도크 3개(A1 AVAILABLE, A2 AVAILABLE, A3 MAINTENANCE), `POST /assignments`로 A1에 실제 ACTIVE 배정 생성.
- curl로 `GET /admin/warehouses/1/summary` 확인: A1 `{status:AVAILABLE, occupied:true}`, A2 `{status:AVAILABLE, occupied:false}`, A3 `{status:MAINTENANCE, occupied:false}` - 도크 상태와 무관하게 실제 배정 여부가 정확히 반영됨. 도크 없는 창고(부산2센터)는 `docs:[]` 빈 배열 확인.
- `npm run dev`(포트 5173, `VITE_API_BASE_URL=http://localhost:8080`) 기동 후 Playwright(`playwright-core`, 시스템 Chrome, headless)로 실제 브라우저 시나리오 재현:
  1. 관리자 로그인 -> 사이드바 "혼잡도 요약" 클릭 -> `/admin/summary` 이동 확인
  2. 드롭다운에서 서울1센터 선택 -> 도크 리스트 `[{A1, 사용중}, {A2, 사용가능}, {A3, 점검중}]` DOM에서 직접 추출해 백엔드 값과 정확히 일치함을 확인
  3. 창고 목록의 부산2센터 행 "혼잡도 요약" 링크 클릭 -> `/admin/summary?warehouseId=2`로 이동, 드롭다운이 "부산2센터"로 자동 선택됨을 확인(사전 선택 기능 검증)
  4. 창고 목록 -> "도크 관리" 링크, `/admin/warehouses/1/docks/new` 폼 화면 모두 사이드바가 깨지지 않고 정상 렌더링됨을 스크린샷으로 확인
  5. `page.on('console'/'pageerror')`로 전 시나리오 콘솔/페이지 에러 0건 확인

### 독립 검증 환경
- 백엔드/프론트 프로세스 종료, 임시 Postgres `pg_ctl stop` 후 데이터 디렉터리 삭제, `git worktree remove --force`로 임시 워크트리 제거, `git status` clean 확인, 원래 브랜치(`feat/28-admin-ui-vuetify-redesign`)는 처음부터 건드리지 않음(worktree 격리로 접근하지 않았고 커밋도 하지 않음).

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 참고사항(도크별 점유 바가 이용률이 아닌 이진값 표시, 사이드바에 "도크 관리" 단독 항목 부재)은 `docs/후속작업.md`에 기록.

## 2026-09-10 - PR #30: feat: 관리자/방문 화면 UI 개선 (Vuetify 도입 + 와이어프레임/브랜드 자산 반영)

- 이슈: #28 [Feature] 관리자/방문 화면 UI 개선 (Vuetify 도입 + 와이어프레임/브랜드 자산 반영)
- 브랜치: `feat/28-admin-ui-vuetify-redesign-v2` → `main` (PR #27 머지로 base가 삭제되며 자동 closed된 #29를 대체, 커밋 내용은 동일하고 main 기준으로 재구성만 함)
- 근거 문서: `../web-draft/콕배정_와이어프레임.zip`(SCR-ADMIN-001/002/003, SCR-AUTH-001, SCR-ASSIGN-001/002, SCR-ASSIGN-PIN-POPUP, POPUPS/POPUPS-1), `../web-draft/favicon.png`/`logo.png`, `../web-draft/기능명세서.md`, 이슈 #28, `docs/작업계획서.md` §6, `docs/후속작업.md`의 PR #23/#27 항목

### 검토 범위
- `frontend/package.json`에 `vuetify@4.2.1`/`vite-plugin-vuetify`/`@mdi/font` 추가, `main.js`에 브랜드 커스텀 테마 2종(`kokbaejeongLight`/`kokbaejeongDark`) 구성, `App.vue`를 `v-app`으로 감쌈
- `favicon.png`/`logo.png` 반영, `favicon.svg`/`icons.svg`/`vite.svg` 스캐폴드 잔재 제거, `index.html` 타이틀을 "콕배정 관리자"로 변경
- `AdminLayout.vue`: `v-navigation-drawer` 기반 사이드바로 재구성, 메뉴 3개(창고 관리/도크 관리/혼잡도 요약) - PR #27에서 비차단으로 기록된 "도크 관리 메뉴 누락" 해소
- `AdminWarehouseFormView.vue`/`AdminDockFormView.vue`(라우팅 폼 페이지)와 `/new`·`/:id/edit` 라우트를 삭제하고, `WarehouseFormDialog.vue`/`DockFormDialog.vue`/`ConfirmDialog.vue`(전부 신규) 다이얼로그로 전환
- `AdminDockListView.vue`: 창고 종속 라우트(`/admin/warehouses/:id/docks`)에서 독립 라우트(`/admin/docks`, `v-select`로 대상 창고 전환)로 변경
- `AdminSummaryView.vue`: AI 요약 콜아웃 + 도크별 `v-progress-linear`/`v-chip` 재구성. 도크별 이진(0%/100%) 점유 표시는 PR #27에서 이미 비차단으로 기록된 사안과 동일한 이유(백엔드가 도크별 숫자 점유율을 제공하지 않음)로 변화 없음
- `AssignView.vue`/`CancelView.vue`: SCR-ASSIGN-001/002/PIN-POPUP 기준 카드/칩/알림 재구성
- `backend/src/main/resources/data.sql`(신규): 창고 5/도크 18/배정 5/관리자 1 시드, `ON CONFLICT DO NOTHING` + `setval`로 재기동 멱등성 확보

### PR #23 후속작업 항목(비동기 초기값 로드 레이스 컨디션) 반영 여부 직접 확인
코드로 직접 확인. `WarehouseFormDialog.vue`/`DockFormDialog.vue`는 `loadingExisting` ref를 두고, `v-text-field`/`v-select`/`v-checkbox`에 전부 `:disabled="loadingExisting"`를 걸어 비동기 초기값 로드(`loadExisting()`)가 끝나기 전까지 모든 입력 필드를 잠근다. 저장 버튼도 `:disabled="loadingExisting"`으로 동일하게 막혀 있어, PR 본문의 주장(`후속작업.md`의 PR #23 항목 해결)이 실제 코드와 일치함을 확인했다.

### 와이어프레임 대조
9개 PNG(SCR-ADMIN-001/002/003, SCR-AUTH-001, SCR-ASSIGN-001/002/PIN-POPUP, POPUPS/POPUPS-1)를 직접 압축 해제해 열어 확인. 로그인 화면(아이디/비밀번호+눈 아이콘 토글+에러 알럿), 창고/도크 관리 테이블(식별 코드/등록일자/상태 칩/관리 작업 버튼 열), 혼잡도 요약(AI 분석 요약 콜아웃 + 도크별 바/배지), 배정/취소 카드(도크 카드 리스트 → 배정 폼 → 완료 카드 → PIN 확인)가 실제 구현과 레이아웃/구성 요소 단위로 대응됨을 확인. 로그인 화면은 와이어프레임엔 없던 `logo.png` 워드마크("콕배정" 텍스트)를 favicon 아이콘과 함께 추가로 보여주는데, 이슈 #28이 명시한 `favicon.png`/`logo.png` 두 자산을 모두 반영하라는 요구와 일치하는 합리적 확장이라 판단해 비차단/문제없음으로 처리.

### 독립 재현
- 현재 체크아웃이 이미 `feat/28-admin-ui-vuetify-redesign-v2`였으므로 별도 워크트리 없이 그 자리에서 검증(작업 종료 후 `git status` clean 확인).
- `initdb`+`pg_ctl`로 유닉스 소켓(`/tmp`) 임시 Postgres(포트 5433) 기동, `SPRING_DATASOURCE_*`/`OPENAI_API_KEY=sk-dummy`/`JWT_SECRET`(더미) 환경변수로 `./gradlew bootRun`, `VITE_API_BASE_URL=http://localhost:8080`로 `npm run dev`.
- Playwright(시스템 Chrome, headless)로 실제 브라우저 시나리오 재현 및 스크린샷 확보:
  1. `/admin` 진입 시 브라우저 탭 타이틀 "콕배정 관리자", `<link rel="icon">`가 `favicon.png`로 로드됨을 확인(구 Vite 스캐폴드 흔적 없음)
  2. 오답 로그인(`admin`/`wrongpass`) → 에러 알럿 노출, 정답 로그인(`admin`/`admin1234`, `data.sql` 시드 계정) → `/admin/warehouses`로 리다이렉트
  3. 창고 목록 테이블(시드 5개: 물류센터 A/B, 냉동창고 C, 영남 물류센터, 호남 스마트허브) 렌더링 확인, 창고 등록 다이얼로그 오픈/닫기, 기존 창고 수정 다이얼로그에서 로딩 중 필드 비활성화 → 로드 완료 후 기존값("물류센터 A")으로 채워짐을 확인
  4. `/admin/docks`에서 창고 선택 드롭다운으로 대상 창고 전환, 도크 목록 테이블(규격/가동상태 칩/보유특성 칩) 확인, 도크 등록 다이얼로그 오픈 확인
  5. `/admin/summary?warehouseId=1`에서 AI 분석 요약 문구("전체 활성 도크 5개 중 1개가 배정되어 점유율은 20%입니다" - OPENAI_API_KEY 더미라 폴백 문장 경로) + 도크별 바/배지(A-2만 "사용중" 파란 바, 나머지 회색) 렌더링 확인
  6. `/w/1`(방문 기사 배정 화면) 진입 → AVAILABLE 도크(A-3) 선택 → 기사명/PIN 입력 → 배정 완료 카드(배정 번호 1006) 확인 → "배정 취소하러 가기" → PIN(1234) 입력 → "배정이 취소됐습니다" 확인
  7. 전 시나리오에서 `page.on('console'/'pageerror')`로 콘솔 에러 0건 확인(로그인 실패 시의 401 응답 로그 1건은 의도된 정상 동작)
- **`data.sql` 멱등성 검증**: 위 시나리오 실행 후(배정 1006을 취소 상태로 만든 채) 백엔드 프로세스를 완전히 종료하고 동일 DB에 대해 재기동. 재기동 로그에 SQL 에러/중복 삽입 없음을 확인, 재기동 후 `assignment` 테이블을 직접 조회해 시드 5건(1001~1005, ACTIVE)은 그대로이고 테스트로 취소한 1006도 `CANCELLED` 상태로 유지됨(재시딩으로 덮어써지지 않음)을 확인. 이어서 `POST /admin/warehouses`로 신규 창고를 만들어 `id=6`이 정상 발급됨을 확인해(시드 최대값 5와 충돌 없음) `setval` 처리가 실제로 동작함을 검증.
- 검증 후 백엔드/프론트 프로세스와 임시 Postgres(`pg_ctl stop`) 모두 종료, `git status` clean 확인.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

새로 발견된 비차단 항목 없음. PR #27에서 이미 기록된 "도크별 점유 바 이진 표시" 항목은 이번 PR에서도 동일한 이유(백엔드 API 제약)로 유지되며, "사이드바 도크 관리 메뉴 누락" 항목은 이번 PR로 해소되었다.

## 2026-09-10 - PR #32: fix: 관리자 사이드바 테마 버그, 창고 정보 구조 개선, 방문 기사 배정 흐름 재구성

- 이슈: #31 [Fix] 관리자 사이드바 테마 버그 및 정보 구조 개선, 방문 기사 배정 흐름 재구성
- 브랜치: `feat/31-admin-ia-and-assign-flow-fix` → `main`
- 근거 문서: `../web-draft/콕배정_와이어프레임.zip`(SCR-ADMIN-001/002/003, SCR-ASSIGN-001/002, SCR-ASSIGN-PIN-POPUP), 이슈 #31, PR #30 리뷰 기록(2026-09-10 항목)

### 검토 범위
- `AdminLayout.vue`: `v-navigation-drawer`의 `theme="kokbaejeongDark" color="primary"` 제거, `theme="dark"` + `.admin-sidebar { background: #0f172a !important; }`로 교체해 커스텀 다크 테마의 `primary`(`#3B82F6`, 밝은 파랑)에 대한 의존을 끊음. 사이드바 메뉴도 "창고 관리" 단일 항목으로 축소.
- `AdminWarehouseListView.vue`/`AdminDockListView.vue`: `v-data-table`을 감싸던 `<v-card border>`를 제거하고 `hide-default-footer` + `.flat-table` 스코프 스타일(헤더 밑줄 + 행 구분선)로 교체. 창고 목록의 상태(활성/비활성) 컬럼과 행별 관리 작업 버튼 4개(도크 관리/혼잡도 요약/수정/비활성화)는 마크업상 그대로 유지됨을 diff로 확인.
- 라우트 재구성: `/admin/docks`, `/admin/summary`(둘 다 쿼리 파라미터로 창고를 전달하던 flat 라우트)를 `/admin/warehouses/:id/docks`, `/admin/warehouses/:id/summary`로 교체. 신규 `AdminWarehouseDetailLayout.vue`가 `props.warehouseId`(라우트 param)로 `adminFetch(/admin/warehouses/{id})`를 호출해 창고명을 헤더에 표시하고, `v-tabs`로 두 하위 화면을 전환. 드롭다운 재선택 UI 없음을 코드로 확인(기존 `AdminDockListView`/`AdminSummaryView`의 `loadWarehouses`/`v-select`/`onWarehouseChange` 로직이 통째로 삭제되고 `route.params.id` 직접 사용으로 대체됨).
- `AssignView.vue`: 도크 목록을 `v-card` 나열에서 `<table>`(`dock-table`)로 변경(SCR-ASSIGN-001과 배치 일치). 배정 완료 시 `assignmentResult`가 있으면 `selectedDock`/`form`/`scheduledTimeLabel`/`specLabel`(전부 이미 클라이언트가 들고 있던 상태에서 계산되는 computed)로 지정 도크명/도착 예정 시각/담당 기사/보유 규격을 렌더링하는 전용 카드(SCR-ASSIGN-002)로 전환하고, 기존 도크 목록 `<table>`은 `v-else-if`로 완전히 대체되어 동시에 보이지 않음. "배정 취소"는 `router.push`/`:to` 없이 `cancelDialog` ref를 여는 같은 컴포넌트의 `v-dialog`(SCR-ASSIGN-PIN-POPUP과 배치 일치)로 처리, 확인 시 `apiFetch(POST /assignments/{id}/cancel)` 후 `cancelled.value = true`로 같은 카드 안에서 취소 상태를 보여줌(라우트 이동 없음).
- `router/index.js`: 라우트별 `meta.title`을 추가하고 `router.afterEach`에서 `document.title = to.meta.title`로 반영. 방문 기사 라우트(`assign`, `cancel`)는 "콕배정 - ...", 관리자 라우트는 "콕배정 관리자 - ..."로 분리됨을 확인. `index.html`의 정적 `<title>`도 "콕배정 관리자" → "콕배정"으로 변경(SPA 초기 로드 시점에 방문 기사에게 "관리자" 문구가 잠깐이라도 보이는 것을 방지).

### 독립 재현
- 이미 `feat/31-admin-ia-and-assign-flow-fix`가 체크아웃된 메인 워크트리에서 그대로 검증(별도 워크트리 불필요, 작업 종료 후 `git status` clean 확인).
- `initdb`+`pg_ctl`로 유닉스 소켓(`/tmp/pgsock32`) + TCP(포트 5433) 겸용 임시 Postgres 기동, `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/kokbaejeong`/`OPENAI_API_KEY=sk-dummy`/`JWT_SECRET`(랜덤)로 `./gradlew bootRun`(PR #30의 `data.sql`이 창고 5/도크 18/배정 5/관리자 1을 그대로 시딩), `VITE_API_BASE_URL=http://localhost:8080`로 `npm run dev`.
- Playwright(`playwright`, `channel: 'chrome'`, 시스템 Chrome, headless)로 실제 브라우저 시나리오를 스크립트 하나로 재현하고 매 단계 스크린샷 확보:
  1. 관리자 로그인(`admin`/`admin1234`) 후 `/admin/warehouses`에서 `getComputedStyle(사이드바).backgroundColor`를 직접 읽어 `rgb(15, 23, 42)`(`#0f172a`)임을 확인(밝은 파랑 아님). 테이블에 `.v-data-table-footer` 없음, 상태 칩(`활성`) 존재, 두 번째 행 "물류센터 B"(첫 번째 행이 아님)에 버튼 4개(`["도크 관리","혼잡도 요약","수정","비활성화"]`)가 정확히 있음을 DOM에서 추출해 확인.
  2. "물류센터 B" 행의 "도크 관리" 클릭 → URL이 `/admin/warehouses/2/docks`로 이동, 문서 타이틀 "콕배정 관리자 - 도크 관리", 헤더 "물류센터 B" 확인. 이어서 "혼잡도 요약" 탭 클릭 → 재선택 UI 없이 URL만 `/admin/warehouses/2/summary`로 바뀌고 헤더가 계속 "물류센터 B"로 유지됨을 확인(동일 창고 id, 동일 세션 안에서 탭 전환만 일어남을 검증).
  3. `/w/1` 진입 → 문서 타이틀 "콕배정 - 도크 배정"(`관리자` 문구 없음), 도크 목록이 `<table>`로 렌더링됨을 확인. "배정 가능" 도크(A-1) 행 클릭 → 기사명/PIN 입력 후 "도크 배정 완료" 제출 → 배정 완료 카드에서 "지정 도크명"/"담당 기사"/"보유 규격" 라벨과 실제 값(`A-1`/`홍길동`/`대형 (레벨러, 도크씰 지원)`)이 렌더링됨을 텍스트로 확인, `document.querySelector('.dock-table')`이 `null`(기존 도크 표가 더 이상 DOM에 남아있지 않음)임을 확인, URL은 여전히 `/w/1`(라우트 이동 없음).
  4. "배정 취소" 클릭 → URL이 그대로 `/w/1`인 상태에서 PIN 확인 모달(`v-dialog`)이 열림을 스크린샷으로 확인(와이어프레임 SCR-ASSIGN-PIN-POPUP과 배치 일치) → PIN `1234` 입력 후 "확인" → "배정이 취소됐습니다" 문구로 전환, URL은 계속 `/w/1`.
  5. 관리자/방문 두 페이지 모두 `page.on('console'|'pageerror')`로 콘솔 에러 0건 확인.
- 검증 후 프론트/백엔드 프로세스 종료, `pg_ctl stop`으로 임시 Postgres 중지 및 데이터 디렉터리/스크래치 파일 정리, `git status` clean 확인.

### 최종 판정
승인. `main`에 머지 완료 (squash merge, 브랜치 삭제).

비차단 항목 2건을 `docs/후속작업.md`에 기록: (1) PR #27에서 기록된 "사이드바 도크 관리 메뉴 부재" 항목이 이번 PR의 드릴다운 재구성으로 해소되었음을 추가 기재, (2) 이번 PR로 새로 생긴 항목 - "배정 취소"가 인라인 PIN 모달로 바뀌면서 기존 `CancelView.vue`(`/assignments/:id/cancel`)로 가는 UI 경로가 사라져 URL 직접 접근으로만 도달 가능한 고아 화면이 됨(기능은 정상 동작, 접근 경로만 없어짐).
