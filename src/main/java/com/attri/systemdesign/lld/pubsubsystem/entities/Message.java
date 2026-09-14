package com.attri.systemdesign.lld.pubsubsystem.entities;

import lombok.Getter;

import java.time.Instant;

@Getter
public class Message {
    private final String payload;
    private final Instant timestamp;

    public Message(String payload) {
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    @Override
    public String toString() {
        return "Message{" + "payload='" + payload + '\'' + '}';
    }
}
