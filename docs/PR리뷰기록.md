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
