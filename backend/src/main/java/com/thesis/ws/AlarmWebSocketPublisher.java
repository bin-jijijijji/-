package com.thesis.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlarmWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public AlarmWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(AlarmMessage message) {
        messagingTemplate.convertAndSend("/topic/alarms", message);
    }
}

