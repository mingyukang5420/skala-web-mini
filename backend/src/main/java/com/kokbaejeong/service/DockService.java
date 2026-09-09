package com.kokbaejeong.service;

import com.kokbaejeong.dto.DockAdminResponse;
import com.kokbaejeong.dto.DockCreateRequest;
import com.kokbaejeong.dto.DockUpdateRequest;
import com.kokbaejeong.entity.AssignmentStatus;
import com.kokbaejeong.entity.Dock;
import com.kokbaejeong.entity.Warehouse;
import com.kokbaejeong.exception.BusinessException;
import com.kokbaejeong.exception.ErrorCode;
import com.kokbaejeong.repository.AssignmentRepository;
import com.kokbaejeong.repository.DockRepository;
import com.kokbaejeong.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DockService {

    private final DockRepository dockRepository;
    private final WarehouseRepository warehouseRepository;
    private final AssignmentRepository assignmentRepository;

    public DockService(DockRepository dockRepository, WarehouseRepository warehouseRepository,
                        AssignmentRepository assignmentRepository) {
        this.dockRepository = dockRepository;
        this.warehouseRepository = warehouseRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<DockAdminResponse> getByWarehouse(Long warehouseId) {
        return dockRepository.findByWarehouseId(warehouseId).stream()
                .map(DockAdminResponse::from)
                .toList();
    }

    public DockAdminResponse getById(Long id) {
        return DockAdminResponse.from(findDockOrThrow(id));
    }

    @Transactional
    public DockAdminResponse create(DockCreateRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Dock dock = new Dock(warehouse, request.name(), request.size(), request.status(),
                request.hasLeveler(), request.hasDockSeal(), request.supportsColdChain(), request.supportsHazmat());
        return DockAdminResponse.from(dockRepository.save(dock));
    }

    @Transactional
    public DockAdminResponse update(Long id, DockUpdateRequest request) {
        Dock dock = findDockOrThrow(id);
        dock.setName(request.name());
        dock.setSize(request.size());
        dock.setStatus(request.status());
        dock.setHasLeveler(request.hasLeveler());
        dock.setHasDockSeal(request.hasDockSeal());
        dock.setSupportsColdChain(request.supportsColdChain());
        dock.setSupportsHazmat(request.supportsHazmat());
        return DockAdminResponse.from(dock);
    }

    @Transactional
    public DockAdminResponse deactivate(Long id) {
        Dock dock = findDockOrThrow(id);
        if (assignmentRepository.existsByDockIdAndStatus(id, AssignmentStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.DOCK_HAS_ACTIVE_ASSIGNMENT);
        }
        dock.deactivate();
        return DockAdminResponse.from(dock);
    }

    @Transactional
    public DockAdminResponse activate(Long id) {
        Dock dock = findDockOrThrow(id);
        dock.activate();
        return DockAdminResponse.from(dock);
    }

    private Dock findDockOrThrow(Long id) {
        return dockRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCK_NOT_FOUND));
    }
}
