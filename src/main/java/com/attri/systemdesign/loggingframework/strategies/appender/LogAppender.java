package com.attri.systemdesign.loggingframework.strategies.appender;

import com.attri.systemdesign.loggingframework.entities.LogMessage;
import com.attri.systemdesign.loggingframework.strategies.formatter.LogFormatter;

public interface LogAppender {
    void append(LogMessage logMessage);
    void close();
    LogFormatter getFormatter();
    void setFormatter(LogFormatter formatter);
}
