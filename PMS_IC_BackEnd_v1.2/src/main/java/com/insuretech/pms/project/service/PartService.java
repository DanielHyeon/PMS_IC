package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.*;
import com.insuretech.pms.project.entity.Part;
import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.repository.PartRepository;
import com.insuretech.pms.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartService {

    private final PartRepository partRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<PartDto> getPartsByProject(String projectId) {
        validateProjectExists(projectId);
        return partRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(PartDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PartDto getPartById(String partId) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> CustomException.notFound("Part not found: " + partId));
        return PartDto.from(part);
    }

    @Transactional
    public PartDto createPart(String projectId, CreatePartRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> CustomException.notFound("Project not found: " + projectId));

        Part part = Part.builder()
                .id("part-" + UUID.randomUUID().toString().substring(0, 8))
                .name(request.getName())
                .description(request.getDescription())
                .project(project)
                .leaderId(request.getLeaderId())
                .leaderName(request.getLeaderName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Part.PartStatus.ACTIVE)
                .progress(0)
                .build();

        Part saved = partRepository.save(part);
        log.info("Part created: {} for project: {}", saved.getId(), projectId);
        return PartDto.from(saved);
    }

    @Transactional
    public PartDto updatePart(String partId, UpdatePartRequest request) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> CustomException.notFound("Part not found: " + partId));

        if (request.getName() != null) {
            part.setName(request.getName());
        }
        if (request.getDescription() != null) {
            part.setDescription(request.getDescription());
        }
        if (request.getLeaderId() != null) {
            part.setLeaderId(request.getLeaderId());
        }
        if (request.getLeaderName() != null) {
            part.setLeaderName(request.getLeaderName());
        }
        if (request.getStatus() != null) {
            part.setStatus(Part.PartStatus.valueOf(request.getStatus()));
        }
        if (request.getStartDate() != null) {
            part.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            part.setEndDate(request.getEndDate());
        }
        if (request.getProgress() != null) {
            part.setProgress(request.getProgress());
        }

        Part updated = partRepository.save(part);
        log.info("Part updated: {}", updated.getId());
        return PartDto.from(updated);
    }

    @Transactional
    public void deletePart(String partId) {
        if (!partRepository.existsById(partId)) {
            throw CustomException.notFound("Part not found: " + partId);
        }
        partRepository.deleteById(partId);
        log.info("Part deleted: {}", partId);
    }

    @Transactional
    public PartDto assignLeader(String partId, AssignLeaderRequest request) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> CustomException.notFound("Part not found: " + partId));

        part.setLeaderId(request.getUserId());
        part.setLeaderName(request.getUserName());

        Part updated = partRepository.save(part);
        log.info("Leader assigned to part {}: {}", partId, request.getUserId());
        return PartDto.from(updated);
    }

    @Transactional(readOnly = true)
    public Set<String> getPartMembers(String partId) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> CustomException.notFound("Part not found: " + partId));
        return part.getMemberIds();
    }

    @Transactional
    public PartDto addMember(String partId, PartMemberRequest request) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> CustomException.notFound("Part not found: " + partId));

        part.getMemberIds().add(request.getUserId());

        Part updated = partRepository.save(part);
        log.info("Member added to part {}: {}", partId, request.getUserId());
        return PartDto.from(updated);
    }

    @Transactional
    public PartDto removeMember(String partId, String memberId) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> CustomException.notFound("Part not found: " + partId));

        part.getMemberIds().remove(memberId);

        Part updated = partRepository.save(part);
        log.info("Member removed from part {}: {}", partId, memberId);
        return PartDto.from(updated);
    }

    private void validateProjectExists(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw CustomException.notFound("Project not found: " + projectId);
        }
    }
}
