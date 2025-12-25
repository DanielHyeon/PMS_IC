package com.insuretech.pms.task.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kanban_columns", schema = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanbanColumn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "project_id", length = 50, nullable = false)
    private String projectId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "order_num", nullable = false)
    private Integer orderNum;

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "color", length = 20)
    private String color;
}