package com.kokbaejeong.controller;

import com.kokbaejeong.dto.DockResponse;
import com.kokbaejeong.dto.WarehouseResponse;
import com.kokbaejeong.service.VisitorWarehouseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
public class WarehouseVisitorController {

    private final VisitorWarehouseService visitorWarehouseService;

    public WarehouseVisitorController(VisitorWarehouseService visitorWarehouseService) {
        this.visitorWarehouseService = visitorWarehouseService;
    }

    @GetMapping("/{id}")
    public WarehouseResponse getWarehouse(@PathVariable Long id) {
        return visitorWarehouseService.getWarehouse(id);
    }

    @GetMapping("/{id}/docks")
    public List<DockResponse> getDocks(@PathVariable Long id) {
        return visitorWarehouseService.getDocks(id);
    }
}
