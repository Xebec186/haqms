package com.haqms.repository;

import com.haqms.entity.QueueEntry;
import com.haqms.enums.AppointmentPriority;
import com.haqms.enums.QueueEntryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByAppointment_AppointmentId(Long appointmentId);

    // ── Duplicate check-in guard ──────────────────────────────────────────────

    Optional<QueueEntry> findByAppointmentAppointmentId(Long appointmentId);

    // ── Position assignment ───────────────────────────────────────────────────

    @Query("SELECT MAX(qe.queuePosition) FROM QueueEntry qe " +
           "WHERE qe.queue.queueId = :queueId")
    Optional<Integer> findMaxPositionInQueue(@Param("queueId") Long queueId);

    // ── Priority-ordered call-next ────────────────────────────────────────────

    /**
     * Returns WAITING entries ordered by priority weight then queue_position.
     * EMERGENCY (1) → URGENT (2) → REGULAR (3), FIFO within each tier.
     * Use with PageRequest.of(0, 1) to fetch only the next patient.
     */
    @Query("SELECT qe FROM QueueEntry qe " +
           "JOIN qe.appointment a " +
           "WHERE qe.queue.queueId = :queueId " +
           "  AND qe.status = 'WAITING' " +
           "ORDER BY " +
           "  CASE a.appointmentPriority " +
           "    WHEN 'EMERGENCY' THEN 1 " +
           "    WHEN 'URGENT'    THEN 2 " +
           "    ELSE 3 " +
           "  END ASC, " +
           "  qe.queuePosition ASC")
    List<QueueEntry> findWaitingByQueueOrderedByPriority(
            @Param("queueId") Long queueId, Pageable pageable);

    // ── All entries for provider dashboard (priority-sorted) ─────────────────

    @Query("SELECT qe FROM QueueEntry qe " +
           "JOIN qe.appointment a " +
           "WHERE qe.queue.queueId = :queueId " +
           "ORDER BY " +
           "  CASE a.appointmentPriority " +
           "    WHEN 'EMERGENCY' THEN 1 " +
           "    WHEN 'URGENT'    THEN 2 " +
           "    ELSE 3 " +
           "  END ASC, " +
           "  qe.queuePosition ASC")
    List<QueueEntry> findAllByQueueOrderedByPriority(@Param("queueId") Long queueId);

    @Query("SELECT qe FROM QueueEntry qe " +
            "JOIN qe.appointment a " +
            "WHERE a.provider.providerId = :providerId " +
            "  AND qe.queue.queueDate = CURRENT_DATE " +
            "ORDER BY " +
            "  CASE a.appointmentPriority " +
            "    WHEN 'EMERGENCY' THEN 1 " +
            "    WHEN 'URGENT'    THEN 2 " +
            "    ELSE 3 " +
            "  END ASC, " +
            "  qe.queuePosition ASC")
    List<QueueEntry> findAllByProviderOrderedByPriority(@Param("providerId") Long providerId);



    // ── Priority-aware estimated wait calculation ─────────────────────────────

    @Query("SELECT COUNT(qe) FROM QueueEntry qe " +
           "JOIN qe.appointment a " +
           "WHERE qe.queue.queueId = :queueId " +
           "  AND qe.status = 'WAITING' " +
           "  AND a.appointmentPriority = :priority")
    long countWaitingByQueueAndPriority(
            @Param("queueId") Long queueId,
            @Param("priority") AppointmentPriority priority);

    @Query("SELECT COUNT(qe) FROM QueueEntry qe " +
           "JOIN qe.appointment a " +
           "WHERE qe.queue.queueId = :queueId " +
           "  AND qe.status = 'WAITING' " +
           "  AND a.appointmentPriority = :priority " +
           "  AND qe.queuePosition < :position")
    long countWaitingByQueuePriorityAndPositionLessThan(
            @Param("queueId") Long queueId,
            @Param("priority") AppointmentPriority priority,
            @Param("position") int position);

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(qe) FROM QueueEntry qe " +
           "WHERE qe.queue.queueDate = :date " +
           "  AND qe.status = :status")
    long countByQueueQueueDateAndStatus(
            @Param("date") LocalDate date,
            @Param("status") QueueEntryStatus status);

    @Query("SELECT COUNT(qe) FROM QueueEntry qe " +
           "JOIN qe.appointment a " +
           "WHERE qe.queue.queueDate = :date " +
           "  AND qe.status = 'WAITING' " +
           "  AND a.appointmentPriority = :priority")
    long countWaitingByDateAndPriority(
            @Param("date") LocalDate date,
            @Param("priority") AppointmentPriority priority);

    @Query("SELECT AVG(qe.waitMinutes) FROM QueueEntry qe " +
           "JOIN qe.appointment a " +
           "WHERE a.department.departmentId = :departmentId " +
           "  AND qe.queue.queueDate BETWEEN :from AND :to " +
           "  AND qe.waitMinutes IS NOT NULL")
    Double findAverageWaitMinutesByDepartmentAndDateRange(
            @Param("departmentId") Long departmentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
