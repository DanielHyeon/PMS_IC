package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.KpiDto;
import com.insuretech.pms.project.entity.Kpi;
import com.insuretech.pms.project.entity.Phase;
import com.insuretech.pms.project.repository.KpiRepository;
import com.insuretech.pms.project.repository.PhaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final KpiRepository kpiRepository;
    private final PhaseRepository phaseRepository;

    @Transactional(readOnly = true)
    public List<KpiDto> getKpisByPhase(String phaseId) {
        ensurePhaseExists(phaseId);
        return kpiRepository.findByPhaseId(phaseId).stream()
                .map(KpiDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public KpiDto createKpi(String phaseId, KpiDto dto) {
        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> CustomException.notFound("단계를 찾을 수 없습니다: " + phaseId));

        Kpi kpi = Kpi.builder()
                .phase(phase)
                .name(dto.getName())
                .target(dto.getTarget())
                .current(dto.getCurrent())
                .status(parseStatus(dto.getStatus()))
                .build();

        return KpiDto.from(kpiRepository.save(kpi));
    }

    @Transactional
    public KpiDto updateKpi(String phaseId, String kpiId, KpiDto dto) {
        Kpi kpi = kpiRepository.findById(kpiId)
                .orElseThrow(() -> CustomException.notFound("KPI를 찾을 수 없습니다: " + kpiId));

        if (!kpi.getPhase().getId().equals(phaseId)) {
            throw CustomException.badRequest("해당 단계와 KPI ID가 일치하지 않습니다.");
        }

        kpi.setName(dto.getName());
        kpi.setTarget(dto.getTarget());
        kpi.setCurrent(dto.getCurrent());
        kpi.setStatus(parseStatus(dto.getStatus()));

        return KpiDto.from(kpiRepository.save(kpi));
    }

    @Transactional
    public void deleteKpi(String phaseId, String kpiId) {
        Kpi kpi = kpiRepository.findById(kpiId)
                .orElseThrow(() -> CustomException.notFound("KPI를 찾을 수 없습니다: " + kpiId));

        if (!kpi.getPhase().getId().equals(phaseId)) {
            throw CustomException.badRequest("해당 단계와 KPI ID가 일치하지 않습니다.");
        }

        kpiRepository.delete(kpi);
    }

    private void ensurePhaseExists(String phaseId) {
        if (!phaseRepository.existsById(phaseId)) {
            throw CustomException.notFound("단계를 찾을 수 없습니다: " + phaseId);
        }
    }

    private Kpi.KpiStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Kpi.KpiStatus.ON_TRACK;
        }
        return Kpi.KpiStatus.valueOf(status);
    }
}
