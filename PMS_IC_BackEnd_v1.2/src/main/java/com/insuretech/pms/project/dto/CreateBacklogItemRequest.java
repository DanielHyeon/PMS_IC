package com.insuretech.pms.project.dto;

public class CreateBacklogItemRequest {
    /**
     * For REQUIREMENT-origin items: requirementId is required
     * For MANUAL-origin items: requirementId should be null, title and storyPoints provided
     */
    private String requirementId;
    private String title;
    private Integer storyPoints;
    private Integer estimatedEffortHours;
    private String epicId;
    private String epicIdRef;

    public CreateBacklogItemRequest() {
    }

    public CreateBacklogItemRequest(String requirementId, String title, Integer storyPoints,
                                   Integer estimatedEffortHours, String epicId, String epicIdRef) {
        this.requirementId = requirementId;
        this.title = title;
        this.storyPoints = storyPoints;
        this.estimatedEffortHours = estimatedEffortHours;
        this.epicId = epicId;
        this.epicIdRef = epicIdRef;
    }

    public String getRequirementId() { return requirementId; }
    public void setRequirementId(String requirementId) { this.requirementId = requirementId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getStoryPoints() { return storyPoints; }
    public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }

    public Integer getEstimatedEffortHours() { return estimatedEffortHours; }
    public void setEstimatedEffortHours(Integer estimatedEffortHours) { this.estimatedEffortHours = estimatedEffortHours; }

    public String getEpicId() { return epicId; }
    public void setEpicId(String epicId) { this.epicId = epicId; }

    public String getEpicIdRef() { return epicIdRef; }
    public void setEpicIdRef(String epicIdRef) { this.epicIdRef = epicIdRef; }
}
