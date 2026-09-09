package com.kokbaejeong.dto;

import com.kokbaejeong.entity.DockStatus;

import java.util.List;

public record WarehouseSummaryResponse(
        Long warehouseId,
        double occupancyRate,
        String summary,
        List<DockOccupancy> docks
) {

    public record DockOccupancy(
            Long dockId,
            String name,
            DockStatus status,
            boolean occupied
    ) {
    }
}
