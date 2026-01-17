package com.insuretech.pms.project.dto;

public class UpdateBacklogItemRequest {
    private Integer storyPoints;
    private Integer estimatedEffortHours;
    private String acceptanceCriteria;
    private String epicId;
    private String epicIdRef;

    public UpdateBacklogItemRequest() {
    }

    public UpdateBacklogItemRequest(Integer storyPoints, Integer estimatedEffortHours,
                                   String acceptanceCriteria, String epicId, String epicIdRef) {
        this.storyPoints = storyPoints;
        this.estimatedEffortHours = estimatedEffortHours;
        this.acceptanceCriteria = acceptanceCriteria;
        this.epicId = epicId;
        this.epicIdRef = epicIdRef;
    }

    public Integer getStoryPoints() { return storyPoints; }
    public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }

    public Integer getEstimatedEffortHours() { return estimatedEffortHours; }
    public void setEstimatedEffortHours(Integer estimatedEffortHours) { this.estimatedEffortHours = estimatedEffortHours; }

    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public String getEpicId() { return epicId; }
    public void setEpicId(String epicId) { this.epicId = epicId; }

    public String getEpicIdRef() { return epicIdRef; }
    public void setEpicIdRef(String epicIdRef) { this.epicIdRef = epicIdRef; }
}
