package com.insuretech.pms.project.dto;

import java.time.LocalDate;

public class CreateSprintRequest {
    private String sprintName;
    private String sprintGoal;
    private LocalDate startDate;
    private LocalDate endDate;

    public CreateSprintRequest() {
    }

    public CreateSprintRequest(String sprintName, String sprintGoal, LocalDate startDate, LocalDate endDate) {
        this.sprintName = sprintName;
        this.sprintGoal = sprintGoal;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getSprintName() { return sprintName; }
    public void setSprintName(String sprintName) { this.sprintName = sprintName; }

    public String getSprintGoal() { return sprintGoal; }
    public void setSprintGoal(String sprintGoal) { this.sprintGoal = sprintGoal; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
