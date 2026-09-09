package com.kokbaejeong.dto;

import com.kokbaejeong.entity.Dock;
import com.kokbaejeong.entity.DockSize;
import com.kokbaejeong.entity.DockStatus;

public record DockResponse(
        Long id,
        String name,
        DockSize size,
        DockStatus status,
        boolean hasLeveler,
        boolean hasDockSeal,
        boolean supportsColdChain,
        boolean supportsHazmat
) {

    public static DockResponse from(Dock dock) {
        return new DockResponse(
                dock.getId(),
                dock.getName(),
                dock.getSize(),
                dock.getStatus(),
                dock.isHasLeveler(),
                dock.isHasDockSeal(),
                dock.isSupportsColdChain(),
                dock.isSupportsHazmat()
        );
    }
}
