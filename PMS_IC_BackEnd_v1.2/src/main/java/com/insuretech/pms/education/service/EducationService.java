package com.insuretech.pms.education.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.education.dto.EducationDto;
import com.insuretech.pms.education.dto.EducationRoadmapDto;
import com.insuretech.pms.education.dto.EducationSessionDto;
import com.insuretech.pms.education.entity.Education;
import com.insuretech.pms.education.entity.EducationRoadmap;
import com.insuretech.pms.education.entity.EducationSession;
import com.insuretech.pms.education.repository.EducationRepository;
import com.insuretech.pms.education.repository.EducationRoadmapRepository;
import com.insuretech.pms.education.repository.EducationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EducationService {

    private final EducationRepository educationRepository;
    private final EducationSessionRepository sessionRepository;
    private final EducationRoadmapRepository roadmapRepository;

    // ========== Education CRUD ==========

    @Transactional(readOnly = true)
    public List<EducationDto> getAllEducations() {
        return educationRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(EducationDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EducationDto getEducationById(String educationId) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> CustomException.notFound("교육 과정을 찾을 수 없습니다: " + educationId));
        return EducationDto.from(education);
    }

    @Transactional
    public EducationDto createEducation(EducationDto dto) {
        Education education = Education.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .educationType(parseEducationType(dto.getEducationType()))
                .category(parseCategory(dto.getCategory()))
                .targetRole(parseTargetRole(dto.getTargetRole()))
                .durationHours(dto.getDurationHours())
                .prerequisites(dto.getPrerequisites())
                .learningObjectives(dto.getLearningObjectives())
                .instructor(dto.getInstructor())
                .materials(dto.getMaterials())
                .isActive(true)
                .build();

        Education saved = educationRepository.save(education);
        log.info("Education created: {}", saved.getId());
        return EducationDto.from(saved);
    }

    @Transactional
    public EducationDto updateEducation(String educationId, EducationDto dto) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> CustomException.notFound("교육 과정을 찾을 수 없습니다: " + educationId));

        education.setTitle(dto.getTitle());
        education.setDescription(dto.getDescription());
        education.setEducationType(parseEducationType(dto.getEducationType()));
        education.setCategory(parseCategory(dto.getCategory()));
        education.setTargetRole(parseTargetRole(dto.getTargetRole()));
        education.setDurationHours(dto.getDurationHours());
        education.setPrerequisites(dto.getPrerequisites());
        education.setLearningObjectives(dto.getLearningObjectives());
        education.setInstructor(dto.getInstructor());
        education.setMaterials(dto.getMaterials());
        if (dto.getIsActive() != null) {
            education.setIsActive(dto.getIsActive());
        }

        Education saved = educationRepository.save(education);
        log.info("Education updated: {}", saved.getId());
        return EducationDto.from(saved);
    }

    @Transactional
    public void deleteEducation(String educationId) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> CustomException.notFound("교육 과정을 찾을 수 없습니다: " + educationId));
        education.setIsActive(false);
        educationRepository.save(education);
        log.info("Education deactivated: {}", educationId);
    }

    // ========== Session CRUD ==========

    @Transactional(readOnly = true)
    public List<EducationSessionDto> getSessionsByEducation(String educationId) {
        return sessionRepository.findByEducationIdOrderByScheduledAtDesc(educationId).stream()
                .map(EducationSessionDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public EducationSessionDto createSession(String educationId, EducationSessionDto dto) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> CustomException.notFound("교육 과정을 찾을 수 없습니다: " + educationId));

        EducationSession session = EducationSession.builder()
                .education(education)
                .sessionName(dto.getSessionName())
                .scheduledAt(dto.getScheduledAt())
                .endAt(dto.getEndAt())
                .location(dto.getLocation())
                .instructor(dto.getInstructor() != null ? dto.getInstructor() : education.getInstructor())
                .maxParticipants(dto.getMaxParticipants())
                .currentParticipants(0)
                .status(EducationSession.SessionStatus.SCHEDULED)
                .notes(dto.getNotes())
                .build();

        EducationSession saved = sessionRepository.save(session);
        log.info("Education session created: {}", saved.getId());
        return EducationSessionDto.from(saved);
    }

    @Transactional
    public EducationSessionDto updateSession(String educationId, String sessionId, EducationSessionDto dto) {
        EducationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> CustomException.notFound("교육 세션을 찾을 수 없습니다: " + sessionId));

        if (!session.getEducation().getId().equals(educationId)) {
            throw CustomException.badRequest("해당 교육 과정과 세션 ID가 일치하지 않습니다.");
        }

        session.setSessionName(dto.getSessionName());
        session.setScheduledAt(dto.getScheduledAt());
        session.setEndAt(dto.getEndAt());
        session.setLocation(dto.getLocation());
        session.setInstructor(dto.getInstructor());
        session.setMaxParticipants(dto.getMaxParticipants());
        session.setStatus(parseSessionStatus(dto.getStatus()));
        session.setNotes(dto.getNotes());

        EducationSession saved = sessionRepository.save(session);
        log.info("Education session updated: {}", saved.getId());
        return EducationSessionDto.from(saved);
    }

    @Transactional
    public void deleteSession(String educationId, String sessionId) {
        EducationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> CustomException.notFound("교육 세션을 찾을 수 없습니다: " + sessionId));

        if (!session.getEducation().getId().equals(educationId)) {
            throw CustomException.badRequest("해당 교육 과정과 세션 ID가 일치하지 않습니다.");
        }

        sessionRepository.delete(session);
        log.info("Education session deleted: {}", sessionId);
    }

    // ========== Roadmap ==========

    @Transactional(readOnly = true)
    public List<EducationRoadmapDto> getAllRoadmaps() {
        return roadmapRepository.findAllByOrderByTargetRoleAscLevelAscOrderNumAsc().stream()
                .map(EducationRoadmapDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EducationRoadmapDto> getRoadmapsByRole(String role) {
        Education.TargetRole targetRole = parseTargetRole(role);
        return roadmapRepository.findByTargetRoleOrderByLevelAscOrderNumAsc(targetRole).stream()
                .map(EducationRoadmapDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public EducationRoadmapDto createRoadmap(EducationRoadmapDto dto) {
        Education education = educationRepository.findById(dto.getEducationId())
                .orElseThrow(() -> CustomException.notFound("교육 과정을 찾을 수 없습니다: " + dto.getEducationId()));

        EducationRoadmap roadmap = EducationRoadmap.builder()
                .education(education)
                .targetRole(parseTargetRole(dto.getTargetRole()))
                .level(parseLevel(dto.getLevel()))
                .orderNum(dto.getOrderNum())
                .isRequired(dto.getIsRequired())
                .description(dto.getDescription())
                .build();

        EducationRoadmap saved = roadmapRepository.save(roadmap);
        log.info("Education roadmap created: {}", saved.getId());
        return EducationRoadmapDto.from(saved);
    }

    @Transactional
    public EducationRoadmapDto updateRoadmap(String roadmapId, EducationRoadmapDto dto) {
        EducationRoadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> CustomException.notFound("로드맵을 찾을 수 없습니다: " + roadmapId));

        roadmap.setTargetRole(parseTargetRole(dto.getTargetRole()));
        roadmap.setLevel(parseLevel(dto.getLevel()));
        roadmap.setOrderNum(dto.getOrderNum());
        roadmap.setIsRequired(dto.getIsRequired());
        roadmap.setDescription(dto.getDescription());

        EducationRoadmap saved = roadmapRepository.save(roadmap);
        log.info("Education roadmap updated: {}", saved.getId());
        return EducationRoadmapDto.from(saved);
    }

    @Transactional
    public void deleteRoadmap(String roadmapId) {
        EducationRoadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> CustomException.notFound("로드맵을 찾을 수 없습니다: " + roadmapId));
        roadmapRepository.delete(roadmap);
        log.info("Education roadmap deleted: {}", roadmapId);
    }

    // ========== Helper Methods ==========

    private Education.EducationType parseEducationType(String type) {
        if (type == null || type.isBlank()) {
            return Education.EducationType.IT_BASIC;
        }
        return Education.EducationType.valueOf(type);
    }

    private Education.EducationCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return Education.EducationCategory.AGENT_AI;
        }
        return Education.EducationCategory.valueOf(category);
    }

    private Education.TargetRole parseTargetRole(String role) {
        if (role == null || role.isBlank()) {
            return Education.TargetRole.ALL;
        }
        return Education.TargetRole.valueOf(role);
    }

    private EducationSession.SessionStatus parseSessionStatus(String status) {
        if (status == null || status.isBlank()) {
            return EducationSession.SessionStatus.SCHEDULED;
        }
        return EducationSession.SessionStatus.valueOf(status);
    }

    private EducationRoadmap.EducationLevel parseLevel(String level) {
        if (level == null || level.isBlank()) {
            return EducationRoadmap.EducationLevel.BASIC;
        }
        return EducationRoadmap.EducationLevel.valueOf(level);
    }
}
