package com.haqms.entity;

import com.haqms.enums.QueueStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "queues",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id","queue_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Queue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "queue_id")
    private Long queueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private HealthcareProvider provider;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status = QueueStatus.OPEN;

    @Column(name = "current_position", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private Integer currentPosition = 0;

    @Column(name = "total_registered", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private Integer totalRegistered = 0;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
