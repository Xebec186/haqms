package com.haqms.dto.response;

import com.haqms.entity.QueueEntry;
import com.haqms.enums.AppointmentPriority;
import com.haqms.enums.QueueEntryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QueueEntryResponse {

    private Long               entryId;
    private Long               queueId;
    private Long               appointmentId;
    private Long               patientId;
    private String             patientFirstName;
    private String             patientLastName;
    private Integer            queuePosition;
    private QueueEntryStatus   status;
    private AppointmentPriority appointmentPriority;
    private LocalDateTime      checkedInAt;
    private LocalDateTime      calledAt;
    private LocalDateTime      servingStartedAt;
    private LocalDateTime      completedAt;
    private Integer            waitMinutes;
    private Integer            estimatedWaitMinutes; // computed, not persisted

    public static QueueEntryResponse from(QueueEntry e, int estimatedWait) {
        return QueueEntryResponse.builder()
                .entryId(e.getEntryId())
                .queueId(e.getQueue().getQueueId())
                .appointmentId(e.getAppointment().getAppointmentId())
                .patientId(e.getPatient().getPatientId())
                .patientFirstName(e.getPatient().getFirstName())
                .patientLastName(e.getPatient().getLastName())
                .queuePosition(e.getQueuePosition())
                .status(e.getStatus())
                .appointmentPriority(e.getAppointment().getAppointmentPriority())
                .checkedInAt(e.getCheckedInAt())
                .calledAt(e.getCalledAt())
                .servingStartedAt(e.getServingStartedAt())
                .completedAt(e.getCompletedAt())
                .waitMinutes(e.getWaitMinutes())
                .estimatedWaitMinutes(estimatedWait)
                .build();
    }
}
