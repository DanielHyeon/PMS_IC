package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.PhaseDto;
import com.insuretech.pms.project.entity.Phase;
import com.insuretech.pms.project.repository.PhaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhaseService {

    private final PhaseRepository phaseRepository;

    @Transactional(readOnly = true)
    public List<PhaseDto> getPhases(String projectId) {
        List<Phase> phases = projectId == null || projectId.isBlank()
                ? phaseRepository.findAll()
                : phaseRepository.findByProjectIdOrderByOrderNumAsc(projectId);

        return phases.stream()
                .map(PhaseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PhaseDto getPhaseById(String phaseId) {
        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> CustomException.notFound("단계를 찾을 수 없습니다: " + phaseId));
        return PhaseDto.from(phase);
    }
}
