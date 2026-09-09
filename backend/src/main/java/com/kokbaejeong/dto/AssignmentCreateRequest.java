package com.kokbaejeong.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record AssignmentCreateRequest(
        @NotNull Long dockId,
        @NotBlank String driverName,
        LocalDateTime scheduledTime,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$", message = "4자리 숫자여야 합니다") String pin
) {
}
