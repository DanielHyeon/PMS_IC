package com.insuretech.pms.education.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.education.dto.EducationHistoryDto;
import com.insuretech.pms.education.entity.EducationHistory;
import com.insuretech.pms.education.entity.EducationSession;
import com.insuretech.pms.education.repository.EducationHistoryRepository;
import com.insuretech.pms.education.repository.EducationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EducationHistoryService {

    private final EducationHistoryRepository historyRepository;
    private final EducationSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public List<EducationHistoryDto> getHistoriesBySession(String sessionId) {
        return historyRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(EducationHistoryDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EducationHistoryDto> getHistoriesByParticipant(String participantId) {
        return historyRepository.findByParticipantIdOrderByCreatedAtDesc(participantId).stream()
                .map(EducationHistoryDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public EducationHistoryDto registerParticipant(String sessionId, EducationHistoryDto dto) {
        EducationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> CustomException.notFound("교육 세션을 찾을 수 없습니다: " + sessionId));

        if (session.getMaxParticipants() != null &&
            session.getCurrentParticipants() >= session.getMaxParticipants()) {
            throw CustomException.badRequest("정원이 초과되었습니다.");
        }

        EducationHistory history = EducationHistory.builder()
                .session(session)
                .participantId(dto.getParticipantId())
                .participantName(dto.getParticipantName())
                .participantDepartment(dto.getParticipantDepartment())
                .completionStatus(EducationHistory.CompletionStatus.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .build();

        session.setCurrentParticipants(session.getCurrentParticipants() + 1);
        sessionRepository.save(session);

        EducationHistory saved = historyRepository.save(history);
        log.info("Participant registered: {} for session {}", saved.getParticipantId(), sessionId);
        return EducationHistoryDto.from(saved);
    }

    @Transactional
    public EducationHistoryDto updateHistory(String historyId, EducationHistoryDto dto) {
        EducationHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> CustomException.notFound("수강 이력을 찾을 수 없습니다: " + historyId));

        history.setCompletionStatus(parseCompletionStatus(dto.getCompletionStatus()));
        history.setScore(dto.getScore());
        history.setFeedback(dto.getFeedback());

        if (dto.getCompletionStatus() != null &&
            dto.getCompletionStatus().equals("COMPLETED") &&
            history.getCompletedAt() == null) {
            history.setCompletedAt(LocalDateTime.now());
        }

        if (dto.getCertificateIssued() != null) {
            history.setCertificateIssued(dto.getCertificateIssued());
        }

        EducationHistory saved = historyRepository.save(history);
        log.info("Education history updated: {}", saved.getId());
        return EducationHistoryDto.from(saved);
    }

    @Transactional
    public void cancelRegistration(String historyId) {
        EducationHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> CustomException.notFound("수강 이력을 찾을 수 없습니다: " + historyId));

        EducationSession session = history.getSession();
        if (session.getCurrentParticipants() > 0) {
            session.setCurrentParticipants(session.getCurrentParticipants() - 1);
            sessionRepository.save(session);
        }

        historyRepository.delete(history);
        log.info("Education registration cancelled: {}", historyId);
    }

    private EducationHistory.CompletionStatus parseCompletionStatus(String status) {
        if (status == null || status.isBlank()) {
            return EducationHistory.CompletionStatus.REGISTERED;
        }
        return EducationHistory.CompletionStatus.valueOf(status);
    }
}
