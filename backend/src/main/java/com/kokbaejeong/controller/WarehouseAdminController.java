package com.kokbaejeong.controller;

import com.kokbaejeong.dto.WarehouseAdminResponse;
import com.kokbaejeong.dto.WarehouseCreateRequest;
import com.kokbaejeong.dto.WarehouseSummaryResponse;
import com.kokbaejeong.service.WarehouseService;
import com.kokbaejeong.service.WarehouseSummaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/warehouses")
public class WarehouseAdminController {

    private final WarehouseService warehouseService;
    private final WarehouseSummaryService warehouseSummaryService;

    public WarehouseAdminController(WarehouseService warehouseService, WarehouseSummaryService warehouseSummaryService) {
        this.warehouseService = warehouseService;
        this.warehouseSummaryService = warehouseSummaryService;
    }

    @GetMapping
    public List<WarehouseAdminResponse> getAll() {
        return warehouseService.getAll();
    }

    @PostMapping
    public ResponseEntity<WarehouseAdminResponse> create(@Valid @RequestBody WarehouseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(request));
    }

    @GetMapping("/{id}")
    public WarehouseAdminResponse getById(@PathVariable Long id) {
        return warehouseService.getById(id);
    }

    @PutMapping("/{id}")
    public WarehouseAdminResponse update(@PathVariable Long id, @Valid @RequestBody WarehouseCreateRequest request) {
        return warehouseService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public WarehouseAdminResponse deactivate(@PathVariable Long id) {
        return warehouseService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public WarehouseAdminResponse activate(@PathVariable Long id) {
        return warehouseService.activate(id);
    }

    @GetMapping("/{id}/summary")
    public WarehouseSummaryResponse getSummary(@PathVariable Long id) {
        return warehouseSummaryService.getSummary(id);
    }
}
