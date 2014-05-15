package org.votingsystem.groovy.util

import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 *
 * from http://www.stichlberger.com/software/grails-log-to-database-with-custom-log4j-appender/
 */
class VotingSystemLogAppender extends org.apache.log4j.AppenderSkeleton implements org.apache.log4j.Appender {

    private static Logger log = Logger.getLogger(VotingSystemLogAppender.class);

    public String source

    @Override protected void append(LoggingEvent event) {
        String logStatement = getLayout().format(event);
        //EventLog.withNewTransaction {}
        //System.out.println("logStatement: " + logStatement)
        //System.out.println("getRenderedMessage: " + event.getRenderedMessage())
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