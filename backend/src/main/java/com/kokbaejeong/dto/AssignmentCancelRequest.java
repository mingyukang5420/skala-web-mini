package com.kokbaejeong.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignmentCancelRequest(
        @NotBlank String pin
) {
}
