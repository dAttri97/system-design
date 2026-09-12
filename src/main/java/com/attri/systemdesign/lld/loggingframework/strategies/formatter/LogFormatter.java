package com.attri.systemdesign.lld.loggingframework.strategies.formatter;

import com.attri.systemdesign.lld.loggingframework.entities.LogMessage;

public interface LogFormatter {
    String format(LogMessage logMessage);
}
