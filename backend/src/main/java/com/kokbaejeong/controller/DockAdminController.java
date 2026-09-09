package com.kokbaejeong.controller;

import com.kokbaejeong.dto.DockAdminResponse;
import com.kokbaejeong.dto.DockCreateRequest;
import com.kokbaejeong.dto.DockUpdateRequest;
import com.kokbaejeong.service.DockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/docks")
public class DockAdminController {

    private final DockService dockService;

    public DockAdminController(DockService dockService) {
        this.dockService = dockService;
    }

    @GetMapping
    public List<DockAdminResponse> getByWarehouse(@RequestParam Long warehouseId) {
        return dockService.getByWarehouse(warehouseId);
    }

    @PostMapping
    public ResponseEntity<DockAdminResponse> create(@Valid @RequestBody DockCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dockService.create(request));
    }

    @GetMapping("/{id}")
    public DockAdminResponse getById(@PathVariable Long id) {
        return dockService.getById(id);
    }

    @PutMapping("/{id}")
    public DockAdminResponse update(@PathVariable Long id, @Valid @RequestBody DockUpdateRequest request) {
        return dockService.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public DockAdminResponse deactivate(@PathVariable Long id) {
        return dockService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    public DockAdminResponse activate(@PathVariable Long id) {
        return dockService.activate(id);
    }
}
