package com.attri.systemdesign.lld.loggingframework.entities;

import com.attri.systemdesign.lld.loggingframework.enums.LogLevel;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public final class LogMessage {
    // Getters for all fields
    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String loggerName;
    private final String threadName;
    private final String message;

    public LogMessage(LogLevel level, String loggerName, String message) {
        this.timestamp = LocalDateTime.now();
        this.level = level;
        this.loggerName = loggerName;
        this.message = message;
        this.threadName = Thread.currentThread().getName();
    }

}
