package com.kokbaejeong.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dock")
public class Dock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    private String name;

    @Enumerated(EnumType.STRING)
    private DockSize size;

    @Enumerated(EnumType.STRING)
    private DockStatus status;

    private boolean hasLeveler;
    private boolean hasDockSeal;
    private boolean supportsColdChain;
    private boolean supportsHazmat;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    protected Dock() {
    }

    public Dock(Warehouse warehouse, String name, DockSize size, DockStatus status,
                boolean hasLeveler, boolean hasDockSeal, boolean supportsColdChain, boolean supportsHazmat) {
        this.warehouse = warehouse;
        this.name = name;
        this.size = size;
        this.status = status;
        this.hasLeveler = hasLeveler;
        this.hasDockSeal = hasDockSeal;
        this.supportsColdChain = supportsColdChain;
        this.supportsHazmat = supportsHazmat;
    }

    public Long getId() {
        return id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DockSize getSize() {
        return size;
    }

    public void setSize(DockSize size) {
        this.size = size;
    }

    public DockStatus getStatus() {
        return status;
    }

    public void setStatus(DockStatus status) {
        this.status = status;
    }

    public boolean isHasLeveler() {
        return hasLeveler;
    }

    public void setHasLeveler(boolean hasLeveler) {
        this.hasLeveler = hasLeveler;
    }

    public boolean isHasDockSeal() {
        return hasDockSeal;
    }

    public void setHasDockSeal(boolean hasDockSeal) {
        this.hasDockSeal = hasDockSeal;
    }

    public boolean isSupportsColdChain() {
        return supportsColdChain;
    }

    public void setSupportsColdChain(boolean supportsColdChain) {
        this.supportsColdChain = supportsColdChain;
    }

    public boolean isSupportsHazmat() {
        return supportsHazmat;
    }

    public void setSupportsHazmat(boolean supportsHazmat) {
        this.supportsHazmat = supportsHazmat;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public void deactivate() {
        this.deletedAt = LocalDateTime.now();
    }

    public void activate() {
        this.deletedAt = null;
    }
}
