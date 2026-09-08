# 배포 안내 (로컬 Docker Compose vs Railway)

## 로컬 개발 환경

```bash
cp .env.example .env
# .env 파일에 실제 값 입력 후
docker compose up --build
```

- 프론트엔드: http://localhost:3000
- 백엔드: http://localhost:8080
- DB: localhost:5432 (컨테이너 내부에서는 backend가 `db` 호스트명으로 접근)

## Railway 배포 시 주의할 점

로컬 Compose와 Railway 배포는 구조가 다릅니다.

1. **DB는 컨테이너로 올리지 않습니다.** Railway 프로젝트에 PostgreSQL 플러그인을 추가하면 Railway가 관리형 DB를 제공합니다. 이 DB의 접속 정보(URL, 계정, 비밀번호)를 Railway가 자동으로 환경변수(`DATABASE_URL` 등)로 주입해줍니다. 이 값을 백엔드의 `SPRING_DATASOURCE_URL` 등에 매핑해야 합니다.

2. **백엔드와 프론트엔드는 각각 별도 서비스로 배포**합니다. 이 저장소의 `backend/Dockerfile`, `frontend/Dockerfile`을 각각 Railway 서비스에 연결하면 됩니다.

3. **프론트엔드의 `VITE_API_BASE_URL`은 빌드 시점에 고정**됩니다. Railway에서 백엔드 서비스의 실제 배포 URL을 확인한 후, 프론트엔드 서비스의 빌드 환경변수로 그 URL을 넣어줘야 합니다. 로컬처럼 nginx 프록시(`/api`)에 의존할 수 없습니다.

4. **환경변수는 Railway 대시보드에서 서비스별로 설정**합니다. `.env` 파일은 로컬 전용이며 Railway에는 올라가지 않습니다.

## 필요한 환경변수 정리

| 변수명 | 로컬 (.env) | Railway |
|---|---|---|
| POSTGRES_DB / USER / PASSWORD | 직접 지정 | Railway Postgres 플러그인이 자동 생성 |
| SPRING_DATASOURCE_URL 등 | db 컨테이너 기준 자동 조립 | Railway가 제공하는 DB 접속 정보로 매핑 |
| OPENAI_API_KEY | 직접 입력 | Railway 백엔드 서비스 환경변수로 등록 |
| JWT_SECRET | 직접 입력 | Railway 백엔드 서비스 환경변수로 등록 |
| VITE_API_BASE_URL | 비워둠 (nginx 프록시 사용) | 배포된 백엔드 URL 입력 후 프론트 재빌드 |
