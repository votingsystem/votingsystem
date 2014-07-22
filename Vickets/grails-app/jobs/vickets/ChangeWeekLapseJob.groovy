package vickets

import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.Vicket

class ChangeWeekLapseJob {
    static triggers = {
        //http://quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
        //0 15 10 ? * MON	Fire at 10:15am every Monday
        cron name:'cronTrigger', startDelay:10000, cronExpression: '0 0 0 ? * MON' //00:00am every Monday
        //simple repeatInterval: 10000l // execute job once in 10 seconds
    }

    def BalanceService

    def execute() {
        checkCancelledVickets();
        //we know this is launch every Monday at 00:00 so we just make sure to select a day from last week to select the period
        Date oneDayLastWeek = org.votingsystem.util.DateUtils.getDatePlus(-3)
        DateUtils.TimePeriod timePeriod = org.votingsystem.util.DateUtils.getWeekPeriod(oneDayLastWeek)
        BalanceService.calculatePeriod(timePeriod)
    }


    private checkCancelledVickets() {
        Calendar calendar = Calendar.getInstance()
        Vicket.withTransaction {
            def criteria = Vicket.createCriteria()
            log.debug("checkCancelledVickets ${calendar.getTime()}")
            def criteriaVicket = criteria.scroll {
                le("validTo", calendar.getTime())
                eq("state", Vicket.State.OK)
            }
            while (criteriaVicket.next()) {
                Vicket vicket = (Vicket) criteriaVicket.get(0);
                vicket.state = Vicket.State.LAPSED
                vicket.save();
                log.debug("checkCancelledVickets - LAPSED ticked ${vicket.id}")
            }
        }
    }

}
