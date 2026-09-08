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

import java.time.LocalDateTime;

@Entity
@Table(name = "assignment")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dock_id", nullable = false)
    private Dock dock;

    private String driverName;

    private LocalDateTime scheduledTime;

    private String pinHash;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    protected Assignment() {
    }

    public Assignment(Dock dock, String driverName, LocalDateTime scheduledTime, String pinHash) {
        this.dock = dock;
        this.driverName = driverName;
        this.scheduledTime = scheduledTime;
        this.pinHash = pinHash;
        this.status = AssignmentStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Dock getDock() {
        return dock;
    }

    public String getDriverName() {
        return driverName;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public String getPinHash() {
        return pinHash;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void cancel() {
        this.status = AssignmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
}
