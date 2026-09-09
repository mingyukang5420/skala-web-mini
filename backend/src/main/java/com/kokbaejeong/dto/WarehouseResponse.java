package com.kokbaejeong.dto;

import com.kokbaejeong.entity.Warehouse;

public record WarehouseResponse(
        Long id,
        String name
) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getName());
    }
}
