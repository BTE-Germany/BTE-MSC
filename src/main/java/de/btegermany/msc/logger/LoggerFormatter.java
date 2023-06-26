package de.btegermany.msc.logger;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggerFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return "["+new Date(record.getMillis()).toString()+"] ["+record.getLevel().toString()+"] "+record.getMessage()+"\n";
    }
}
