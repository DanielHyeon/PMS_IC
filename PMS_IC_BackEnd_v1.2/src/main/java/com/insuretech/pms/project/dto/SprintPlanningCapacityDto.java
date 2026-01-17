package com.insuretech.pms.project.dto;

import java.util.List;

public class SprintPlanningCapacityDto {
    /**
     * Summary data for sprint planning
     */
    private long backlogItemCount;
    private long selectedItemCount;
    private long sprintItemCount;
    private long completedItemCount;
    private Integer totalStoryPointsForSelected;
    private Integer totalEstimatedEffortForSelected;
    private List<BacklogItemDto> selectedItems;

    public SprintPlanningCapacityDto() {
    }

    public SprintPlanningCapacityDto(long backlogItemCount, long selectedItemCount, long sprintItemCount,
                                   long completedItemCount, Integer totalStoryPointsForSelected,
                                   Integer totalEstimatedEffortForSelected, List<BacklogItemDto> selectedItems) {
        this.backlogItemCount = backlogItemCount;
        this.selectedItemCount = selectedItemCount;
        this.sprintItemCount = sprintItemCount;
        this.completedItemCount = completedItemCount;
        this.totalStoryPointsForSelected = totalStoryPointsForSelected;
        this.totalEstimatedEffortForSelected = totalEstimatedEffortForSelected;
        this.selectedItems = selectedItems;
    }

    public long getBacklogItemCount() { return backlogItemCount; }
    public void setBacklogItemCount(long backlogItemCount) { this.backlogItemCount = backlogItemCount; }

    public long getSelectedItemCount() { return selectedItemCount; }
    public void setSelectedItemCount(long selectedItemCount) { this.selectedItemCount = selectedItemCount; }

    public long getSprintItemCount() { return sprintItemCount; }
    public void setSprintItemCount(long sprintItemCount) { this.sprintItemCount = sprintItemCount; }

    public long getCompletedItemCount() { return completedItemCount; }
    public void setCompletedItemCount(long completedItemCount) { this.completedItemCount = completedItemCount; }

    public Integer getTotalStoryPointsForSelected() { return totalStoryPointsForSelected; }
    public void setTotalStoryPointsForSelected(Integer totalStoryPointsForSelected) { this.totalStoryPointsForSelected = totalStoryPointsForSelected; }

    public Integer getTotalEstimatedEffortForSelected() { return totalEstimatedEffortForSelected; }
    public void setTotalEstimatedEffortForSelected(Integer totalEstimatedEffortForSelected) { this.totalEstimatedEffortForSelected = totalEstimatedEffortForSelected; }

    public List<BacklogItemDto> getSelectedItems() { return selectedItems; }
    public void setSelectedItems(List<BacklogItemDto> selectedItems) { this.selectedItems = selectedItems; }
}
