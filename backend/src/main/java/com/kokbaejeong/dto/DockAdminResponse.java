package com.kokbaejeong.dto;

import com.kokbaejeong.entity.Dock;
import com.kokbaejeong.entity.DockSize;
import com.kokbaejeong.entity.DockStatus;

public record DockAdminResponse(
        Long id,
        Long warehouseId,
        String name,
        DockSize size,
        DockStatus status,
        boolean hasLeveler,
        boolean hasDockSeal,
        boolean supportsColdChain,
        boolean supportsHazmat,
        boolean active
) {

    public static DockAdminResponse from(Dock dock) {
        return new DockAdminResponse(
                dock.getId(),
                dock.getWarehouse().getId(),
                dock.getName(),
                dock.getSize(),
                dock.getStatus(),
                dock.isHasLeveler(),
                dock.isHasDockSeal(),
                dock.isSupportsColdChain(),
                dock.isSupportsHazmat(),
                dock.isActive()
        );
    }
}
