package com.haqms.repository;

import com.haqms.entity.Queue;
import com.haqms.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface QueueRepository extends JpaRepository<Queue, Long> {

    // ── Check-in / queue lookup ───────────────────────────────────────────────

    Optional<Queue> findByProviderProviderIdAndQueueDate(
            Long departmentId, LocalDate queueDate);

    // ── Analytics ─────────────────────────────────────────────────────────────

    long countByQueueDateAndStatus(LocalDate queueDate, QueueStatus status);
}
