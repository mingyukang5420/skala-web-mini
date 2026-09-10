package com.kokbaejeong.service;

import com.kokbaejeong.dto.AssignmentCancelRequest;
import com.kokbaejeong.dto.AssignmentCreateRequest;
import com.kokbaejeong.dto.AssignmentResponse;
import com.kokbaejeong.entity.Assignment;
import com.kokbaejeong.entity.AssignmentStatus;
import com.kokbaejeong.entity.Dock;
import com.kokbaejeong.entity.DockStatus;
import com.kokbaejeong.exception.BusinessException;
import com.kokbaejeong.exception.ErrorCode;
import com.kokbaejeong.repository.AssignmentRepository;
import com.kokbaejeong.repository.DockRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final DockRepository dockRepository;
    private final PasswordEncoder passwordEncoder;

    public AssignmentService(AssignmentRepository assignmentRepository, DockRepository dockRepository,
                              PasswordEncoder passwordEncoder) {
        this.assignmentRepository = assignmentRepository;
        this.dockRepository = dockRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AssignmentResponse create(AssignmentCreateRequest request) {
        Dock dock = dockRepository.findById(request.dockId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCK_NOT_AVAILABLE));

        if (!dock.getWarehouse().isActive()) {
            throw new BusinessException(ErrorCode.WAREHOUSE_INACTIVE);
        }
        if (dock.getStatus() != DockStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.DOCK_NOT_AVAILABLE);
        }

        if (request.scheduledTime() != null && request.scheduledTime().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULED_TIME);
        }
        LocalDateTime scheduledTime = request.scheduledTime() != null ? request.scheduledTime() : LocalDateTime.now();
        String pinHash = passwordEncoder.encode(request.pin());
        Assignment assignment = new Assignment(dock, request.driverName(), scheduledTime, pinHash);

        try {
            return AssignmentResponse.from(assignmentRepository.save(assignment));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ASSIGNMENT_CONFLICT);
        }
    }

    @Transactional
    public AssignmentResponse cancel(Long id, AssignmentCancelRequest request) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_MISMATCH));

        if (!passwordEncoder.matches(request.pin(), assignment.getPinHash())) {
            throw new BusinessException(ErrorCode.PIN_MISMATCH);
        }

        if (assignment.getStatus() == AssignmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELLED);
        }

        assignment.cancel();
        return AssignmentResponse.from(assignment);
    }
}
