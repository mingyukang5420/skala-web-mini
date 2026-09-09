package com.kokbaejeong.repository;

import com.kokbaejeong.entity.Assignment;
import com.kokbaejeong.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    boolean existsByDockIdAndStatus(Long dockId, AssignmentStatus status);
}
