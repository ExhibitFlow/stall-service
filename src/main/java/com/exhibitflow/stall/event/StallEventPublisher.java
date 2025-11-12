package com.exhibitflow.stall.event;

import com.exhibitflow.stall.dto.StallEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StallEventPublisher {

    private final KafkaTemplate<String, StallEventDto> kafkaTemplate;

    @Value("${kafka.topics.stall-reserved}")
    private String stallReservedTopic;

    @Value("${kafka.topics.stall-released}")
    private String stallReleasedTopic;

    public void publishStallReserved(StallEventDto event) {
        log.info("Publishing stall reserved event for stall: {}", event.getStallId());
        kafkaTemplate.send(stallReservedTopic, event.getStallId().toString(), event);
    }

    public void publishStallReleased(StallEventDto event) {
        log.info("Publishing stall released event for stall: {}", event.getStallId());
        kafkaTemplate.send(stallReleasedTopic, event.getStallId().toString(), event);
    }
}
