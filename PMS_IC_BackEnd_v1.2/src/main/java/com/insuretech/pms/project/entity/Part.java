package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parts", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Part extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "leader_id", length = 50)
    private String leaderId;

    @Column(name = "leader_name", length = 100)
    private String leaderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private PartStatus status = PartStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "progress", nullable = false)
    @Builder.Default
    private Integer progress = 0;

    @ElementCollection
    @CollectionTable(name = "part_members", schema = "project",
            joinColumns = @JoinColumn(name = "part_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<String> memberIds = new HashSet<>();

    public enum PartStatus {
        ACTIVE,
        INACTIVE,
        COMPLETED
    }
}
