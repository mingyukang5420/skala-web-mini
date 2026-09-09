package com.kokbaejeong.service;

import com.kokbaejeong.dto.DockResponse;
import com.kokbaejeong.dto.WarehouseResponse;
import com.kokbaejeong.entity.Warehouse;
import com.kokbaejeong.exception.BusinessException;
import com.kokbaejeong.exception.ErrorCode;
import com.kokbaejeong.repository.DockRepository;
import com.kokbaejeong.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VisitorWarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final DockRepository dockRepository;

    public VisitorWarehouseService(WarehouseRepository warehouseRepository, DockRepository dockRepository) {
        this.warehouseRepository = warehouseRepository;
        this.dockRepository = dockRepository;
    }

    public WarehouseResponse getWarehouse(Long id) {
        return WarehouseResponse.from(findActiveWarehouseOrThrow(id));
    }

    public List<DockResponse> getDocks(Long warehouseId) {
        findActiveWarehouseOrThrow(warehouseId);
        return dockRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId).stream()
                .map(DockResponse::from)
                .toList();
    }

    private Warehouse findActiveWarehouseOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .filter(Warehouse::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));
    }
}
