-- 데모/개발용 목데이터. spring.sql.init.mode=always 라 기동마다 실행되므로
-- ON CONFLICT DO NOTHING으로 재실행 시 중복 삽입을 막는다.
-- 데모 로그인: username=admin / password=admin1234
-- 데모 PIN(모든 시드 배정 공통): 1234

INSERT INTO admin (id, username, password_hash, name, created_at) VALUES
    (1, 'admin', '$2y$10$dYu0f4pX0HWcyglDD358bOmyWGmiII4m73GI40tlBPShO6MnxpBqO', '관리자', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO warehouse (id, name, created_at, deleted_at) VALUES
    (1, '물류센터 A', now() - interval '30 days', NULL),
    (2, '물류센터 B', now() - interval '28 days', NULL),
    (3, '냉동창고 C', now() - interval '20 days', NULL),
    (4, '영남 물류센터', now() - interval '14 days', NULL),
    (5, '호남 스마트허브', now() - interval '7 days', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO dock (id, warehouse_id, name, size, status, has_leveler, has_dock_seal, supports_cold_chain, supports_hazmat, created_at, deleted_at) VALUES
    (101, 1, 'A-1', 'LARGE', 'AVAILABLE', true, true, false, false, now() - interval '30 days', NULL),
    (102, 1, 'A-2', 'LARGE', 'OCCUPIED', true, false, false, false, now() - interval '30 days', NULL),
    (103, 1, 'A-3', 'MEDIUM', 'AVAILABLE', false, true, false, false, now() - interval '30 days', NULL),
    (104, 1, 'A-4', 'MEDIUM', 'MAINTENANCE', false, false, false, true, now() - interval '30 days', NULL),
    (105, 1, 'A-5', 'SMALL', 'AVAILABLE', false, false, false, false, now() - interval '30 days', NULL),

    (201, 2, 'B-1', 'LARGE', 'OCCUPIED', true, true, false, false, now() - interval '28 days', NULL),
    (202, 2, 'B-2', 'MEDIUM', 'AVAILABLE', true, false, false, false, now() - interval '28 days', NULL),
    (203, 2, 'B-3', 'SMALL', 'AVAILABLE', false, false, false, false, now() - interval '28 days', NULL),
    (204, 2, 'B-4', 'SMALL', 'MAINTENANCE', false, false, false, false, now() - interval '28 days', NULL),

    (301, 3, 'C-1', 'LARGE', 'OCCUPIED', true, true, true, false, now() - interval '20 days', NULL),
    (302, 3, 'C-2', 'LARGE', 'AVAILABLE', true, true, true, false, now() - interval '20 days', NULL),
    (303, 3, 'C-3', 'MEDIUM', 'AVAILABLE', false, true, true, false, now() - interval '20 days', NULL),

    (401, 4, 'D-1', 'LARGE', 'AVAILABLE', true, false, false, true, now() - interval '14 days', NULL),
    (402, 4, 'D-2', 'MEDIUM', 'OCCUPIED', false, false, false, false, now() - interval '14 days', NULL),
    (403, 4, 'D-3', 'SMALL', 'AVAILABLE', false, false, false, false, now() - interval '14 days', NULL),

    (501, 5, 'E-1', 'LARGE', 'OCCUPIED', true, true, false, false, now() - interval '7 days', NULL),
    (502, 5, 'E-2', 'MEDIUM', 'AVAILABLE', false, false, false, false, now() - interval '7 days', NULL),
    (503, 5, 'E-3', 'SMALL', 'MAINTENANCE', false, false, false, false, now() - interval '7 days', NULL)
ON CONFLICT (id) DO NOTHING;

-- pin_hash는 전부 데모 PIN '1234'의 BCrypt 해시. dock_id는 위 OCCUPIED 도크와 1:1 매칭.
INSERT INTO assignment (id, dock_id, driver_name, scheduled_time, pin_hash, status, created_at, cancelled_at) VALUES
    (1001, 102, '김민준', now() + interval '1 hour', '$2y$10$lKHgaxALyH9ywJKQFWrQr.Nw3kenoiQnNBAXAjbHeVB9aQIZx45Um', 'ACTIVE', now() - interval '1 hour', NULL),
    (1002, 201, '이서연', now() + interval '2 hour', '$2y$10$lKHgaxALyH9ywJKQFWrQr.Nw3kenoiQnNBAXAjbHeVB9aQIZx45Um', 'ACTIVE', now() - interval '40 minutes', NULL),
    (1003, 301, '박도윤', now() + interval '30 minutes', '$2y$10$lKHgaxALyH9ywJKQFWrQr.Nw3kenoiQnNBAXAjbHeVB9aQIZx45Um', 'ACTIVE', now() - interval '20 minutes', NULL),
    (1004, 402, '최지우', now() + interval '3 hour', '$2y$10$lKHgaxALyH9ywJKQFWrQr.Nw3kenoiQnNBAXAjbHeVB9aQIZx45Um', 'ACTIVE', now() - interval '10 minutes', NULL),
    (1005, 501, '정하윤', now() + interval '90 minutes', '$2y$10$lKHgaxALyH9ywJKQFWrQr.Nw3kenoiQnNBAXAjbHeVB9aQIZx45Um', 'ACTIVE', now() - interval '5 minutes', NULL)
ON CONFLICT (id) DO NOTHING;

-- 명시적 id로 삽입했으므로 identity 시퀀스를 시드 데이터의 최대값 이후로 맞춰
-- 다음 관리자 등록(API를 통한 auto-increment) 시 id 충돌이 나지 않게 한다.
SELECT setval(pg_get_serial_sequence('admin', 'id'), (SELECT MAX(id) FROM admin));
SELECT setval(pg_get_serial_sequence('warehouse', 'id'), (SELECT MAX(id) FROM warehouse));
SELECT setval(pg_get_serial_sequence('dock', 'id'), (SELECT MAX(id) FROM dock));
SELECT setval(pg_get_serial_sequence('assignment', 'id'), (SELECT MAX(id) FROM assignment));
