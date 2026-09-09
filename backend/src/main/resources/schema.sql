-- DBML 표기 한계로 JPA/Hibernate로 표현할 수 없는 부분 유니크 인덱스.
-- 같은 dock_id에 대해 status='ACTIVE'인 배정은 하나만 허용한다 (REQ-NFR-004, 동시 배정 방지).
CREATE UNIQUE INDEX IF NOT EXISTS ux_assignment_dock_active
    ON assignment (dock_id)
    WHERE status = 'ACTIVE';
