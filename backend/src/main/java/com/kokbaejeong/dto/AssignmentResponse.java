package com.kokbaejeong.dto;

import com.kokbaejeong.entity.Assignment;
import com.kokbaejeong.entity.AssignmentStatus;

public record AssignmentResponse(
        Long id,
        Long dockId,
        AssignmentStatus status
) {

    public static AssignmentResponse from(Assignment assignment) {
        return new AssignmentResponse(assignment.getId(), assignment.getDock().getId(), assignment.getStatus());
    }
}
