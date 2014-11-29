package cooins

import org.votingsystem.util.DateUtils
import org.votingsystem.cooin.model.Cooin

class ChangeWeekLapseJob {
    static triggers = {
        //http://quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
        //0 15 10 ? * MON	Fire at 10:15am every Monday
        cron name:'cronTrigger', startDelay:10000, cronExpression: '0 0 0 ? * MON' //00:00am every Monday
        //simple repeatInterval: 10000l // execute job once in 10 seconds
    }

    def BalanceService

    def execute() {
        checkCancelledCooins();
        BalanceService.initWeekPeriod(Calendar.getInstance())
    }


    private checkCancelledCooins() {
        Calendar calendar = Calendar.getInstance()
        Cooin.withTransaction {
            log.debug("checkCancelledCooins ${calendar.getTime()}")
            def criteriaCooin = Cooin.createCriteria().scroll {
                le("validTo", calendar.getTime())
                eq("state", Cooin.State.OK)
            }
            while (criteriaCooin.next()) {
                Cooin cooin = (Cooin) criteriaCooin.get(0);
                cooin.state = Cooin.State.LAPSED
                cooin.save();
                log.debug("checkCancelledCooins - LAPSED ticked ${cooin.id}")
            }
        }
    }

}
