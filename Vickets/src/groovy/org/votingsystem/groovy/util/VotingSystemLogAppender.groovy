package org.votingsystem.groovy.util

import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import org.votingsystem.model.vicket.LogMsg

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 *
 * from http://www.stichlberger.com/software/grails-log-to-database-with-custom-log4j-appender/
 */
class VotingSystemLogAppender extends org.apache.log4j.AppenderSkeleton implements org.apache.log4j.Appender {

    private static Logger reportslog = Logger.getLogger("reportsLog");
    private static Logger transactionslog = Logger.getLogger("transactionsLog");

    public String source

    @Override protected void append(LoggingEvent event) {
        //EventLog.withNewTransaction {}
        def matcher = LogMsg.regExPattern.matcher(event.getRenderedMessage())
        if(matcher.matches()) {
            switch(matcher[0][2]) {
                case LogMsg.REPORT_MSG:
                    reportslog.info(matcher[0][3] + ",")
                    break;
                case LogMsg.TRSANSACTIONVS_MSG:
                    transactionslog.info(matcher[0][3] + ",")
                    break;
            }
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