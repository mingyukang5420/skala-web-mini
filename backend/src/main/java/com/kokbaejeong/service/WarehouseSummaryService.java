package com.kokbaejeong.service;

import com.kokbaejeong.dto.WarehouseSummaryResponse;
import com.kokbaejeong.dto.WarehouseSummaryResponse.DockOccupancy;
import com.kokbaejeong.entity.AssignmentStatus;
import com.kokbaejeong.entity.Dock;
import com.kokbaejeong.exception.BusinessException;
import com.kokbaejeong.exception.ErrorCode;
import com.kokbaejeong.repository.AssignmentRepository;
import com.kokbaejeong.repository.DockRepository;
import com.kokbaejeong.repository.WarehouseRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WarehouseSummaryService {

    private static final String NO_DOCK_MESSAGE = "등록된 도크가 없어 혼잡도를 계산할 수 없습니다.";

    private final WarehouseRepository warehouseRepository;
    private final DockRepository dockRepository;
    private final AssignmentRepository assignmentRepository;
    private final ChatClient chatClient;

    public WarehouseSummaryService(WarehouseRepository warehouseRepository, DockRepository dockRepository,
                                    AssignmentRepository assignmentRepository, ChatClient.Builder chatClientBuilder) {
        this.warehouseRepository = warehouseRepository;
        this.dockRepository = dockRepository;
        this.assignmentRepository = assignmentRepository;
        this.chatClient = chatClientBuilder.build();
    }

    public WarehouseSummaryResponse getSummary(Long warehouseId) {
        var warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));

        List<Dock> docks = dockRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId);
        if (docks.isEmpty()) {
            return new WarehouseSummaryResponse(warehouseId, 0.0, NO_DOCK_MESSAGE, List.of());
        }

        List<DockOccupancy> dockOccupancies = docks.stream()
                .map(dock -> new DockOccupancy(
                        dock.getId(),
                        dock.getName(),
                        dock.getStatus(),
                        assignmentRepository.existsByDockIdAndStatus(dock.getId(), AssignmentStatus.ACTIVE)
                ))
                .toList();

        long occupiedCount = dockOccupancies.stream().filter(DockOccupancy::occupied).count();
        double occupancyRate = (double) occupiedCount / docks.size();

        String summary = generateSummary(warehouse.getName(), docks.size(), occupiedCount, occupancyRate);
        return new WarehouseSummaryResponse(warehouseId, occupancyRate, summary, dockOccupancies);
    }

    private String generateSummary(String warehouseName, int totalDocks, long occupiedCount, double occupancyRate) {
        String prompt = """
                물류창고 "%s"의 하역장(도크) 점유 현황을 한국어 한두 문장으로 간단히 요약해줘.
                - 전체 활성 도크 수: %d
                - 현재 배정(점유) 중인 도크 수: %d
                - 점유율: %.0f%%
                운영 담당자가 참고할 수 있도록 간결하고 자연스러운 문장으로 작성해줘.
                """.formatted(warehouseName, totalDocks, occupiedCount, occupancyRate * 100);

        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (RuntimeException e) {
            // 외부 LLM 호출 실패(네트워크/키 문제 등) 시에도 화면이 죽지 않도록 집계값 기반 문장으로 대체한다.
            return "전체 활성 도크 %d개 중 %d개가 배정되어 점유율은 %.0f%%입니다."
                    .formatted(totalDocks, occupiedCount, occupancyRate * 100);
        }
    }
}
