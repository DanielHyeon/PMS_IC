package com.insuretech.pms.task.repository;

import com.insuretech.pms.task.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, String> {
    List<WeeklyReport> findByProjectIdOrderByWeekStartDateDesc(String projectId);
    List<WeeklyReport> findBySprintIdOrderByWeekStartDateDesc(String sprintId);
    Optional<WeeklyReport> findByProjectIdAndWeekStartDate(String projectId, LocalDate weekStartDate);
    Optional<WeeklyReport> findBySprintIdAndWeekStartDate(String sprintId, LocalDate weekStartDate);
    List<WeeklyReport> findByProjectIdAndWeekStartDateBetween(String projectId, LocalDate startDate, LocalDate endDate);
    List<WeeklyReport> findBySprintIdAndWeekStartDateBetween(String sprintId, LocalDate startDate, LocalDate endDate);
}
