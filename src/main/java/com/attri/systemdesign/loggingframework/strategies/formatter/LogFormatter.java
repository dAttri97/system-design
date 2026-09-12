package com.attri.systemdesign.loggingframework.strategies.formatter;

import com.attri.systemdesign.loggingframework.entities.LogMessage;

public interface LogFormatter {
    String format(LogMessage logMessage);
}
