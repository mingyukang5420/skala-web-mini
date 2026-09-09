package com.kokbaejeong.dto;

import com.kokbaejeong.entity.DockSize;
import com.kokbaejeong.entity.DockStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DockUpdateRequest(
        @NotBlank String name,
        @NotNull DockSize size,
        @NotNull DockStatus status,
        boolean hasLeveler,
        boolean hasDockSeal,
        boolean supportsColdChain,
        boolean supportsHazmat
) {
}
