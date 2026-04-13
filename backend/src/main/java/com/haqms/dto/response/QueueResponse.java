package com.haqms.dto.response;

import com.haqms.entity.Queue;
import com.haqms.enums.QueueStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class QueueResponse {

    private Long          queueId;
    private Long          providerId;
    private String        providerName;
    private LocalDate     queueDate;
    private QueueStatus   status;
    private Integer       currentPosition;
    private Integer       totalRegistered;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    public static QueueResponse from(Queue q) {
        return QueueResponse.builder()
                .queueId(q.getQueueId())
                .providerId(q.getProvider().getProviderId())
                .providerName(q.getProvider().getFullName())
                .queueDate(q.getQueueDate())
                .status(q.getStatus())
                .currentPosition(q.getCurrentPosition())
                .totalRegistered(q.getTotalRegistered())
                .openedAt(q.getOpenedAt())
                .closedAt(q.getClosedAt())
                .build();
    }
}
