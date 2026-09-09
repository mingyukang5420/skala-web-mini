package com.kokbaejeong.service;

import com.kokbaejeong.dto.WarehouseAdminResponse;
import com.kokbaejeong.dto.WarehouseCreateRequest;
import com.kokbaejeong.entity.Warehouse;
import com.kokbaejeong.exception.BusinessException;
import com.kokbaejeong.exception.ErrorCode;
import com.kokbaejeong.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public List<WarehouseAdminResponse> getAll() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseAdminResponse::from)
                .toList();
    }

    public WarehouseAdminResponse getById(Long id) {
        return WarehouseAdminResponse.from(findWarehouseOrThrow(id));
    }

    @Transactional
    public WarehouseAdminResponse create(WarehouseCreateRequest request) {
        Warehouse warehouse = new Warehouse(request.name());
        return WarehouseAdminResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    public WarehouseAdminResponse update(Long id, WarehouseCreateRequest request) {
        Warehouse warehouse = findWarehouseOrThrow(id);
        warehouse.setName(request.name());
        return WarehouseAdminResponse.from(warehouse);
    }

    @Transactional
    public WarehouseAdminResponse deactivate(Long id) {
        Warehouse warehouse = findWarehouseOrThrow(id);
        warehouse.deactivate();
        return WarehouseAdminResponse.from(warehouse);
    }

    @Transactional
    public WarehouseAdminResponse activate(Long id) {
        Warehouse warehouse = findWarehouseOrThrow(id);
        warehouse.activate();
        return WarehouseAdminResponse.from(warehouse);
    }

    private Warehouse findWarehouseOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));
    }
}
