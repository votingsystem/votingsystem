package tickets

import org.votingsystem.model.ticket.TicketVS

import java.util.Calendar;


class ChangeWeekLapseJob {
    static triggers = {
        //http://quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
        //0 15 10 ? * MON	Fire at 10:15am every Monday
        cron name:'cronTrigger', startDelay:10000, cronExpression: '0 0 0 ? * MON' //00:00am every Monday
        //simple repeatInterval: 10000l // execute job once in 10 seconds
    }

    def execute() {
        checkCancelledTickets();
    }


    private checkCancelledTickets() {
        Calendar calendar = Calendar.getInstance()
        TicketVS.withTransaction {
            def criteria = TicketVS.createCriteria()
            log.debug("checkCancelledTickets ${calendar.getTime()}")
            def criteriaTicket = criteria.scroll {
                le("validTo", calendar.getTime())
                eq("state", TicketVS.State.OK)
            }
            while (criteriaTicket.next()) {
                TicketVS ticketVS = (TicketVS) criteriaTicket.get(0);
                ticketVS.state = TicketVS.State.LAPSED
                ticketVS.save();
                log.debug("checkCancelledTickets - LAPSED ticked ${ticketVS.id}")
            }
        }
    }

}
