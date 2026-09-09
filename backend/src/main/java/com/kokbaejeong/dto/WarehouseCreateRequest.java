package com.kokbaejeong.dto;

import jakarta.validation.constraints.NotBlank;

public record WarehouseCreateRequest(
        @NotBlank String name
) {
}
