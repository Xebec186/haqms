package com.haqms.service;

import com.haqms.dto.response.QueueEntryResponse;
import com.haqms.dto.response.QueueResponse;
import com.haqms.entity.SystemUser;
import com.haqms.enums.QueueStatus;

import java.util.List;

public interface QueueService {

    QueueEntryResponse checkIn(Long appointmentId);

    QueueResponse getQueueById(Long queueId);

    QueueEntryResponse getQueueEntryByAppointmentId(Long appointmentId, SystemUser currentUser);

    List<QueueEntryResponse> getEntriesByQueue(Long queueId);

    List<QueueEntryResponse> getEntriesByProvider(Long providerId);

    QueueEntryResponse callNext(Long queueId);

    QueueEntryResponse startServing(Long entryId);

    QueueEntryResponse completeEntry(Long entryId);

    QueueEntryResponse markMissed(Long entryId);

    QueueResponse updateQueueStatus(Long queueId, QueueStatus newStatus);

    QueueResponse getTodaysQueueByProvider(Long providerId);

}
