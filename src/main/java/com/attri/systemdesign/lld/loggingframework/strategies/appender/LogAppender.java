package com.attri.systemdesign.lld.loggingframework.strategies.appender;

import com.attri.systemdesign.lld.loggingframework.entities.LogMessage;
import com.attri.systemdesign.lld.loggingframework.strategies.formatter.LogFormatter;

public interface LogAppender {
    void append(LogMessage logMessage);
    void close();
    LogFormatter getFormatter();
    void setFormatter(LogFormatter formatter);
}
