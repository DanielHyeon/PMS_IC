package com.insuretech.pms.rfp.service;

import com.insuretech.pms.rfp.dto.*;
import com.insuretech.pms.rfp.entity.*;
import com.insuretech.pms.rfp.repository.RfpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RfpService {

    private final RfpRepository rfpRepository;
    private static final String UPLOAD_DIR = "./uploads/rfp/";

    public List<RfpDto> getRfpsByProject(String projectId) {
        return rfpRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(RfpDto::fromEntity)
                .collect(Collectors.toList());
    }

    public RfpDto getRfpById(String projectId, String rfpId) {
        Rfp rfp = rfpRepository.findByIdAndProjectId(rfpId, projectId)
                .orElseThrow(() -> new RuntimeException("RFP not found: " + rfpId));
        return RfpDto.fromEntity(rfp);
    }

    @Transactional
    public RfpDto createRfp(String projectId, CreateRfpRequest request) {
        Rfp rfp = Rfp.builder()
                .projectId(projectId)
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus() != null ? RfpStatus.valueOf(request.getStatus()) : RfpStatus.DRAFT)
                .processingStatus(request.getProcessingStatus() != null ?
                        ProcessingStatus.valueOf(request.getProcessingStatus()) : ProcessingStatus.PENDING)
                .tenantId(projectId)
                .build();

        Rfp saved = rfpRepository.save(rfp);
        log.info("Created RFP: {} for project: {}", saved.getId(), projectId);
        return RfpDto.fromEntity(saved);
    }

    @Transactional
    public RfpDto updateRfp(String projectId, String rfpId, UpdateRfpRequest request) {
        Rfp rfp = rfpRepository.findByIdAndProjectId(rfpId, projectId)
                .orElseThrow(() -> new RuntimeException("RFP not found: " + rfpId));

        if (request.getTitle() != null) {
            rfp.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            rfp.setContent(request.getContent());
        }
        if (request.getStatus() != null) {
            rfp.setStatus(RfpStatus.valueOf(request.getStatus()));
        }

        Rfp saved = rfpRepository.save(rfp);
        log.info("Updated RFP: {}", rfpId);
        return RfpDto.fromEntity(saved);
    }

    @Transactional
    public void deleteRfp(String projectId, String rfpId) {
        Rfp rfp = rfpRepository.findByIdAndProjectId(rfpId, projectId)
                .orElseThrow(() -> new RuntimeException("RFP not found: " + rfpId));

        // Delete file if exists
        if (rfp.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(rfp.getFilePath()));
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", rfp.getFilePath());
            }
        }

        rfpRepository.delete(rfp);
        log.info("Deleted RFP: {}", rfpId);
    }

    @Transactional
    public RfpDto uploadRfpFile(String projectId, MultipartFile file, String title) throws IOException {
        // Create upload directory if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR, projectId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String newFilename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(newFilename);

        // Save file
        Files.copy(file.getInputStream(), filePath);

        // Create RFP entity
        Rfp rfp = Rfp.builder()
                .projectId(projectId)
                .title(title != null ? title : originalFilename.replace(extension, ""))
                .filePath(filePath.toString())
                .fileName(originalFilename)
                .fileType(extension.replace(".", ""))
                .fileSize(file.getSize())
                .status(RfpStatus.DRAFT)
                .processingStatus(ProcessingStatus.PENDING)
                .tenantId(projectId)
                .build();

        Rfp saved = rfpRepository.save(rfp);
        log.info("Uploaded RFP file: {} for project: {}", originalFilename, projectId);
        return RfpDto.fromEntity(saved);
    }

    @Transactional
    public RfpDto startExtraction(String projectId, String rfpId) {
        Rfp rfp = rfpRepository.findByIdAndProjectId(rfpId, projectId)
                .orElseThrow(() -> new RuntimeException("RFP not found: " + rfpId));

        rfp.setProcessingStatus(ProcessingStatus.EXTRACTING);
        rfp.setProcessingMessage("Requirement extraction started");

        Rfp saved = rfpRepository.save(rfp);
        log.info("Started extraction for RFP: {}", rfpId);

        // TODO: Trigger async extraction via LLM service

        return RfpDto.fromEntity(saved);
    }

    public List<RfpDto> searchRfps(String projectId, String keyword) {
        return rfpRepository.searchByKeyword(projectId, keyword)
                .stream()
                .map(RfpDto::fromEntity)
                .collect(Collectors.toList());
    }
}
