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
