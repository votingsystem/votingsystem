package org.votingsystem.groovy.util

import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import grails.converters.JSON
/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 *
 * from http://www.stichlberger.com/software/grails-log-to-database-with-custom-log4j-appender/
 */
class VotingSystemLogAppender extends org.apache.log4j.AppenderSkeleton implements org.apache.log4j.Appender {

    private static Logger reportslog = Logger.getLogger("reportsLog");

    public String source

    @Override protected void append(LoggingEvent event) {
        //EventLog.withNewTransaction {}
        //String logStatement = getLayout().format(event);
        if(event.getRenderedMessage().contains("REPORT_MSG")) {
            Map<String,Object> eventMap = new LinkedHashMap();
            eventMap.put("timestamp", event.timeStamp);
            eventMap.put("date", new Date(event.timeStamp));
            eventMap.put("message", event.getRenderedMessage());
            String msg = new JSON(eventMap);
            reportslog.info(msg + ",")
        }
    }

    /**
     * Set the source value for the logger (e.g. which application the logger belongs to)
     * @param source
     */
    public void setSource(String source) {
        this.source = source
    }

    public String getSource() {
        return source;
    }

    @Override void close() {  }

    @Override boolean requiresLayout() {
        return true
    }

}