package com.kokbaejeong.repository;

import com.kokbaejeong.entity.Dock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DockRepository extends JpaRepository<Dock, Long> {

    List<Dock> findByWarehouseId(Long warehouseId);
}
