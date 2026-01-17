package com.insuretech.pms.project.dto;

import java.util.List;

public class ReorderBacklogItemsRequest {
    /**
     * List of item IDs in the desired priority order
     * Index 0 = highest priority (priorityOrder = 0)
     */
    private List<String> itemIds;

    public ReorderBacklogItemsRequest() {
    }

    public ReorderBacklogItemsRequest(List<String> itemIds) {
        this.itemIds = itemIds;
    }

    public List<String> getItemIds() { return itemIds; }
    public void setItemIds(List<String> itemIds) { this.itemIds = itemIds; }
}
