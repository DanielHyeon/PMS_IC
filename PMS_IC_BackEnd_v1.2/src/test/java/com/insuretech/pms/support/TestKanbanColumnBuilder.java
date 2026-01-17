package com.insuretech.pms.support;

import com.insuretech.pms.task.entity.KanbanColumn;
import java.util.UUID;

/**
 * Fluent builder for KanbanColumn entity in tests.
 * Provides default Korean test data.
 */
public class TestKanbanColumnBuilder {

    private String id = UUID.randomUUID().toString();
    private String projectId = "proj-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "컬럼";
    private Integer orderNum = 1;
    private Integer wipLimit = null;
    private String color = "#3498db";

    public TestKanbanColumnBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestKanbanColumnBuilder projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public TestKanbanColumnBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TestKanbanColumnBuilder orderNum(Integer orderNum) {
        this.orderNum = orderNum;
        return this;
    }

    public TestKanbanColumnBuilder wipLimit(Integer wipLimit) {
        this.wipLimit = wipLimit;
        return this;
    }

    public TestKanbanColumnBuilder color(String color) {
        this.color = color;
        return this;
    }

    public TestKanbanColumnBuilder asTodoColumn() {
        this.name = "할 일";
        this.orderNum = 1;
        this.wipLimit = null;
        this.color = "#ecf0f1";
        return this;
    }

    public TestKanbanColumnBuilder asInProgressColumn() {
        this.name = "진행 중";
        this.orderNum = 2;
        this.wipLimit = 5;
        this.color = "#3498db";
        return this;
    }

    public TestKanbanColumnBuilder asReviewColumn() {
        this.name = "검토 중";
        this.orderNum = 3;
        this.wipLimit = 3;
        this.color = "#f39c12";
        return this;
    }

    public TestKanbanColumnBuilder asCompletedColumn() {
        this.name = "완료";
        this.orderNum = 4;
        this.wipLimit = null;
        this.color = "#27ae60";
        return this;
    }

    public TestKanbanColumnBuilder asOnHoldColumn() {
        this.name = "보류";
        this.orderNum = 5;
        this.wipLimit = 2;
        this.color = "#e74c3c";
        return this;
    }

    public TestKanbanColumnBuilder withWipLimit(Integer limit) {
        this.wipLimit = limit;
        return this;
    }

    public TestKanbanColumnBuilder customColor(String colorCode) {
        this.color = colorCode;
        return this;
    }

    public KanbanColumn build() {
        return KanbanColumn.builder()
                .id(id)
                .projectId(projectId)
                .name(name)
                .orderNum(orderNum)
                .wipLimit(wipLimit)
                .color(color)
                .build();
    }
}
