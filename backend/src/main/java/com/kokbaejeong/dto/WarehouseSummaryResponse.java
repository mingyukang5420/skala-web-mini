package com.kokbaejeong.dto;

public record WarehouseSummaryResponse(
        Long warehouseId,
        double occupancyRate,
        String summary
) {
}
