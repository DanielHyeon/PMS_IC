package com.insuretech.pms.rfp.service;

import com.insuretech.pms.lineage.service.LineageEventProducer;
import com.insuretech.pms.rfp.dto.*;
import com.insuretech.pms.rfp.entity.*;
import com.insuretech.pms.rfp.repository.RequirementRepository;
import com.insuretech.pms.rfp.repository.RfpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final RfpRepository rfpRepository;
    private final LineageEventProducer lineageEventProducer;

    public List<RequirementDto> getRequirementsByProject(String projectId) {
        return requirementRepository.findByProjectIdOrderByCodeAsc(projectId)
                .stream()
                .map(RequirementDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<RequirementDto> getRequirementsByRfp(String rfpId) {
        return requirementRepository.findByRfpIdOrderByCodeAsc(rfpId)
                .stream()
                .map(RequirementDto::fromEntity)
                .collect(Collectors.toList());
    }

    public RequirementDto getRequirementById(String projectId, String requirementId) {
        Requirement req = requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new RuntimeException("Requirement not found: " + requirementId));
        return RequirementDto.fromEntity(req);
    }

    @Transactional
    public RequirementDto createRequirement(String projectId, CreateRequirementRequest request) {
        // Generate requirement code
        String code = generateRequirementCode(projectId, request.getCategory());

        Requirement req = Requirement.builder()
                .projectId(projectId)
                .code(code)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory() != null ?
                        RequirementCategory.valueOf(request.getCategory()) : RequirementCategory.FUNCTIONAL)
                .priority(request.getPriority() != null ?
                        Priority.valueOf(request.getPriority()) : Priority.MEDIUM)
                .status(request.getStatus() != null ?
                        RequirementStatus.valueOf(request.getStatus()) : RequirementStatus.IDENTIFIED)
                .acceptanceCriteria(request.getAcceptanceCriteria())
                .assigneeId(request.getAssigneeId())
                .dueDate(request.getDueDate())
                .estimatedEffort(request.getEstimatedEffort())
                .tenantId(projectId)
                .build();

        // Link to RFP if provided
        if (request.getRfpId() != null && !request.getRfpId().isEmpty()) {
            Rfp rfp = rfpRepository.findById(request.getRfpId())
                    .orElseThrow(() -> new RuntimeException("RFP not found: " + request.getRfpId()));
            req.setRfp(rfp);
        }

        Requirement saved = requirementRepository.save(req);
        log.info("Created Requirement: {} for project: {}", saved.getCode(), projectId);

        // Publish lineage event (same transaction)
        lineageEventProducer.publishRequirementCreated(
                saved.getId(), projectId, saved.getCode(), saved.getTitle());

        return RequirementDto.fromEntity(saved);
    }

    @Transactional
    public RequirementDto updateRequirement(String projectId, String requirementId, UpdateRequirementRequest request) {
        Requirement req = requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new RuntimeException("Requirement not found: " + requirementId));

        if (request.getTitle() != null) {
            req.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            req.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            req.setCategory(RequirementCategory.valueOf(request.getCategory()));
        }
        if (request.getPriority() != null) {
            req.setPriority(Priority.valueOf(request.getPriority()));
        }
        if (request.getStatus() != null) {
            req.setStatus(RequirementStatus.valueOf(request.getStatus()));
        }
        if (request.getAcceptanceCriteria() != null) {
            req.setAcceptanceCriteria(request.getAcceptanceCriteria());
        }
        if (request.getAssigneeId() != null) {
            req.setAssigneeId(request.getAssigneeId());
        }
        if (request.getDueDate() != null) {
            req.setDueDate(request.getDueDate());
        }
        if (request.getEstimatedEffort() != null) {
            req.setEstimatedEffort(request.getEstimatedEffort());
        }
        if (request.getActualEffort() != null) {
            req.setActualEffort(request.getActualEffort());
        }
        if (request.getProgress() != null) {
            req.setProgress(request.getProgress());
        }

        // Track status change for lineage event
        String oldStatus = null;
        String newStatus = null;
        if (request.getStatus() != null && !request.getStatus().equals(req.getStatus().name())) {
            oldStatus = req.getStatus().name();
            newStatus = request.getStatus();
        }

        Requirement saved = requirementRepository.save(req);
        log.info("Updated Requirement: {}", requirementId);

        // Publish lineage events
        Map<String, Object> changes = new HashMap<>();
        if (request.getTitle() != null) changes.put("title", request.getTitle());
        if (request.getDescription() != null) changes.put("description", request.getDescription());
        if (request.getStatus() != null) changes.put("status", request.getStatus());

        if (!changes.isEmpty()) {
            lineageEventProducer.publishRequirementUpdated(requirementId, projectId, changes);
        }

        if (oldStatus != null) {
            lineageEventProducer.publishRequirementStatusChanged(
                    requirementId, projectId, oldStatus, newStatus);
        }

        return RequirementDto.fromEntity(saved);
    }

    @Transactional
    public void deleteRequirement(String projectId, String requirementId) {
        Requirement req = requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new RuntimeException("Requirement not found: " + requirementId));

        requirementRepository.delete(req);
        log.info("Deleted Requirement: {}", requirementId);

        // Publish lineage event
        lineageEventProducer.publishRequirementDeleted(requirementId, projectId);
    }

    @Transactional
    public RequirementDto linkTask(String projectId, String requirementId, String taskId) {
        Requirement req = requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new RuntimeException("Requirement not found: " + requirementId));

        req.linkTask(taskId);
        Requirement saved = requirementRepository.save(req);
        log.info("Linked task {} to requirement {}", taskId, requirementId);

        // Publish lineage event for Requirement -> Task link
        lineageEventProducer.publishRequirementTaskLinked(requirementId, taskId, projectId);

        return RequirementDto.fromEntity(saved);
    }

    @Transactional
    public void unlinkTask(String projectId, String requirementId, String taskId) {
        Requirement req = requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new RuntimeException("Requirement not found: " + requirementId));

        req.unlinkTask(taskId);
        requirementRepository.save(req);
        log.info("Unlinked task {} from requirement {}", taskId, requirementId);

        // Publish lineage event
        lineageEventProducer.publishRequirementTaskUnlinked(requirementId, taskId, projectId);
    }

    public List<RequirementDto> searchRequirements(String projectId, String keyword) {
        return requirementRepository.searchByKeyword(projectId, keyword)
                .stream()
                .map(RequirementDto::fromEntity)
                .collect(Collectors.toList());
    }

    private String generateRequirementCode(String projectId, String category) {
        // Format: REQ-{PROJECT_CODE}-{CATEGORY}-{SEQ}
        String categoryCode = getCategoryCode(category);
        String prefix = "REQ-" + projectId.substring(0, Math.min(4, projectId.length())).toUpperCase() + "-" + categoryCode + "-";

        Integer maxNum = requirementRepository.findMaxCodeNumber(projectId, prefix);
        int nextNum = (maxNum != null ? maxNum : 0) + 1;

        return prefix + String.format("%03d", nextNum);
    }

    private String getCategoryCode(String category) {
        if (category == null) return "FUNC";
        return switch (category) {
            case "FUNCTIONAL" -> "FUNC";
            case "NON_FUNCTIONAL" -> "NFUNC";
            case "UI" -> "UI";
            case "INTEGRATION" -> "INT";
            case "SECURITY" -> "SEC";
            case "AI" -> "AI";
            case "SI" -> "SI";
            case "COMMON" -> "COM";
            case "TECHNICAL" -> "TECH";
            case "BUSINESS" -> "BIZ";
            case "CONSTRAINT" -> "CONS";
            default -> "FUNC";
        };
    }
}
