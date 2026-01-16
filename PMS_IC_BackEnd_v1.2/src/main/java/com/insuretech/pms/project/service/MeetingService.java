package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.MeetingDto;
import com.insuretech.pms.project.entity.Meeting;
import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.repository.MeetingRepository;
import com.insuretech.pms.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<MeetingDto> getMeetingsByProject(String projectId) {
        ensureProjectExists(projectId);
        return meetingRepository.findByProjectIdOrderByScheduledAtDesc(projectId).stream()
                .map(MeetingDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingDto getMeetingById(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> CustomException.notFound("회의를 찾을 수 없습니다: " + meetingId));
        return MeetingDto.from(meeting);
    }

    @Transactional
    public MeetingDto createMeeting(String projectId, MeetingDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> CustomException.notFound("프로젝트를 찾을 수 없습니다: " + projectId));

        Meeting meeting = Meeting.builder()
                .project(project)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .meetingType(parseMeetingType(dto.getMeetingType()))
                .status(Meeting.MeetingStatus.SCHEDULED)
                .scheduledAt(dto.getScheduledAt())
                .location(dto.getLocation())
                .organizer(dto.getOrganizer())
                .attendees(dto.getAttendees())
                .minutes(dto.getMinutes())
                .build();

        Meeting saved = meetingRepository.save(meeting);
        log.info("Meeting created: {}", saved.getId());
        return MeetingDto.from(saved);
    }

    @Transactional
    public MeetingDto updateMeeting(String projectId, String meetingId, MeetingDto dto) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> CustomException.notFound("회의를 찾을 수 없습니다: " + meetingId));

        if (!meeting.getProject().getId().equals(projectId)) {
            throw CustomException.badRequest("해당 프로젝트와 회의 ID가 일치하지 않습니다.");
        }

        meeting.setTitle(dto.getTitle());
        meeting.setDescription(dto.getDescription());
        meeting.setMeetingType(parseMeetingType(dto.getMeetingType()));
        meeting.setStatus(parseMeetingStatus(dto.getStatus()));
        meeting.setScheduledAt(dto.getScheduledAt());
        meeting.setLocation(dto.getLocation());
        meeting.setOrganizer(dto.getOrganizer());
        meeting.setAttendees(dto.getAttendees());
        meeting.setMinutes(dto.getMinutes());
        meeting.setActualStartAt(dto.getActualStartAt());
        meeting.setActualEndAt(dto.getActualEndAt());

        Meeting saved = meetingRepository.save(meeting);
        log.info("Meeting updated: {}", saved.getId());
        return MeetingDto.from(saved);
    }

    @Transactional
    public void deleteMeeting(String projectId, String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> CustomException.notFound("회의를 찾을 수 없습니다: " + meetingId));

        if (!meeting.getProject().getId().equals(projectId)) {
            throw CustomException.badRequest("해당 프로젝트와 회의 ID가 일치하지 않습니다.");
        }

        meetingRepository.delete(meeting);
        log.info("Meeting deleted: {}", meetingId);
    }

    private void ensureProjectExists(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw CustomException.notFound("프로젝트를 찾을 수 없습니다: " + projectId);
        }
    }

    private Meeting.MeetingType parseMeetingType(String type) {
        if (type == null || type.isBlank()) {
            return Meeting.MeetingType.OTHER;
        }
        return Meeting.MeetingType.valueOf(type);
    }

    private Meeting.MeetingStatus parseMeetingStatus(String status) {
        if (status == null || status.isBlank()) {
            return Meeting.MeetingStatus.SCHEDULED;
        }
        return Meeting.MeetingStatus.valueOf(status);
    }
}
