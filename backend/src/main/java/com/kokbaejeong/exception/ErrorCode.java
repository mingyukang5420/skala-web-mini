package com.kokbaejeong.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    AUTH_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    WAREHOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 창고입니다."),
    WAREHOUSE_INACTIVE(HttpStatus.FORBIDDEN, "비활성화된 창고입니다."),
    DOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 도크입니다."),
    DOCK_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "사용할 수 없는 도크입니다."),
    DOCK_HAS_ACTIVE_ASSIGNMENT(HttpStatus.BAD_REQUEST, "활성 배정이 있는 도크는 비활성화할 수 없습니다."),
    ASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "다른 기사가 먼저 배정했습니다. 새로고침 후 다시 시도해주세요."),
    INVALID_SCHEDULED_TIME(HttpStatus.BAD_REQUEST, "도착 예정 시각은 현재 이후여야 합니다."),
    PIN_MISMATCH(HttpStatus.UNAUTHORIZED, "PIN이 일치하지 않습니다."),
    ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "이미 취소된 배정입니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
