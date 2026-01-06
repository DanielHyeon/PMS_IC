package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.DeliverableDto;
import com.insuretech.pms.project.entity.Deliverable;
import com.insuretech.pms.project.entity.Phase;
import com.insuretech.pms.project.repository.DeliverableRepository;
import com.insuretech.pms.project.repository.PhaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverableService {

    private final DeliverableRepository deliverableRepository;
    private final PhaseRepository phaseRepository;

    @Value("${pms.storage.deliverables:uploads/deliverables}")
    private String deliverableStoragePath;

    @Transactional(readOnly = true)
    public List<DeliverableDto> getDeliverablesByPhase(String phaseId) {
        ensurePhaseExists(phaseId);
        return deliverableRepository.findByPhaseId(phaseId).stream()
                .map(DeliverableDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliverableDto uploadDeliverable(
            String phaseId,
            String deliverableId,
            MultipartFile file,
            String name,
            String description,
            String type,
            String uploadedBy
    ) {
        if (file == null || file.isEmpty()) {
            throw CustomException.badRequest("업로드할 파일이 없습니다.");
        }

        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> CustomException.notFound("단계를 찾을 수 없습니다: " + phaseId));

        Deliverable deliverable = resolveDeliverable(deliverableId, phase, name, description, type);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "-" + originalFileName;
        Path targetPath = buildStoragePath(storedFileName);

        try {
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException e) {
            throw CustomException.internalError("파일 저장에 실패했습니다.");
        }

        deliverable.setFileName(originalFileName);
        deliverable.setFilePath(targetPath.toString());
        deliverable.setFileSize(file.getSize());
        deliverable.setUploadedBy(uploadedBy);
        deliverable.setStatus(Deliverable.DeliverableStatus.IN_REVIEW);

        Deliverable saved = deliverableRepository.save(deliverable);
        log.info("Deliverable uploaded: {} (phase={})", saved.getId(), phaseId);
        return DeliverableDto.from(saved);
    }

    @Transactional
    public DeliverableDto approveDeliverable(String deliverableId, boolean approved, String approver) {
        Deliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> CustomException.notFound("산출물을 찾을 수 없습니다: " + deliverableId));

        deliverable.setStatus(approved ? Deliverable.DeliverableStatus.APPROVED : Deliverable.DeliverableStatus.REJECTED);
        deliverable.setApprover(approver);
        deliverable.setApprovedAt(java.time.LocalDateTime.now());

        Deliverable saved = deliverableRepository.save(deliverable);
        log.info("Deliverable approval updated: {} -> {}", saved.getId(), saved.getStatus());
        return DeliverableDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Deliverable getDeliverable(String deliverableId) {
        return deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> CustomException.notFound("산출물을 찾을 수 없습니다: " + deliverableId));
    }

    @Transactional(readOnly = true)
    public Resource loadDeliverableFile(Deliverable deliverable) {
        if (deliverable.getFilePath() == null) {
            throw CustomException.badRequest("파일이 업로드되지 않았습니다.");
        }

        try {
            Path filePath = Paths.get(deliverable.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw CustomException.notFound("파일을 찾을 수 없습니다.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw CustomException.internalError("파일 경로가 올바르지 않습니다.");
        }
    }

    private void ensurePhaseExists(String phaseId) {
        if (!phaseRepository.existsById(phaseId)) {
            throw CustomException.notFound("단계를 찾을 수 없습니다: " + phaseId);
        }
    }

    private Deliverable resolveDeliverable(
            String deliverableId,
            Phase phase,
            String name,
            String description,
            String type
    ) {
        if (deliverableId != null && !deliverableId.isBlank()) {
            Deliverable existing = deliverableRepository.findById(deliverableId)
                    .orElseThrow(() -> CustomException.notFound("산출물을 찾을 수 없습니다: " + deliverableId));
            if (!existing.getPhase().getId().equals(phase.getId())) {
                throw CustomException.badRequest("해당 단계와 산출물 ID가 일치하지 않습니다.");
            }
            if (name != null && !name.isBlank()) {
                existing.setName(name);
            }
            if (description != null) {
                existing.setDescription(description);
            }
            if (type != null && !type.isBlank()) {
                existing.setType(Deliverable.DeliverableType.valueOf(type));
            }
            return existing;
        }

        if (name == null || name.isBlank()) {
            throw CustomException.badRequest("산출물 이름이 필요합니다.");
        }

        Deliverable.DeliverableType deliverableType = Deliverable.DeliverableType.DOCUMENT;
        if (type != null && !type.isBlank()) {
            deliverableType = Deliverable.DeliverableType.valueOf(type);
        }

        return Deliverable.builder()
                .phase(phase)
                .name(name)
                .description(description)
                .type(deliverableType)
                .status(Deliverable.DeliverableStatus.PENDING)
                .build();
    }

    private Path buildStoragePath(String storedFileName) {
        Path storageDir = Paths.get(deliverableStoragePath);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw CustomException.internalError("파일 저장 경로 생성에 실패했습니다.");
        }
        return storageDir.resolve(storedFileName);
    }
}
