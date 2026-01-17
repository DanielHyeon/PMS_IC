package com.insuretech.pms.task.event;

import com.insuretech.pms.task.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskStatusChangedEvent {

    private String taskId;
    private String requirementId;
    private Task.TaskStatus oldStatus;
    private Task.TaskStatus newStatus;
    private String changedBy;
    private Long eventTimestamp;

    public TaskStatusChangedEvent(String taskId, String requirementId, Task.TaskStatus oldStatus,
                                   Task.TaskStatus newStatus, String changedBy) {
        this.taskId = taskId;
        this.requirementId = requirementId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.eventTimestamp = System.currentTimeMillis();
    }
}
