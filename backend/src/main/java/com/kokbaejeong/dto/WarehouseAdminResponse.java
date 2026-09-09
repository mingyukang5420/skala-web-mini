package com.kokbaejeong.dto;

import com.kokbaejeong.entity.Warehouse;

import java.time.LocalDateTime;

public record WarehouseAdminResponse(
        Long id,
        String name,
        boolean active,
        LocalDateTime createdAt
) {

    public static WarehouseAdminResponse from(Warehouse warehouse) {
        return new WarehouseAdminResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.isActive(),
                warehouse.getCreatedAt()
        );
    }
}
