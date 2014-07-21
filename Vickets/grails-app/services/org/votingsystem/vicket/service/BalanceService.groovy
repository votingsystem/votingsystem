package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.hibernate.ScrollableResults
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketSource
import org.votingsystem.util.DateUtils
import grails.converters.JSON;

//@Transactional
class BalanceService {

    def sessionFactory
    def transactionVSService
    def groupVSService
    def userVSService
    def vicketSourceService
    def filesService

    //@Transactional
    public ResponseVS initWeek() {
        log.debug("initWeek")
        //we know this is launch every Monday at 00:00 so we must select a day
        long begin = System.currentTimeMillis()
        Date oneDayLastWeek = org.votingsystem.util.DateUtils.getDatePlus(-3)
        DateUtils.TimePeriod timePeriod = org.votingsystem.util.DateUtils.getWeekPeriod(oneDayLastWeek)

        int numTotalUsers = UserVS.countByDateCancelledIsNullOrDateCancelledGreaterThanEquals(timePeriod.getDateFrom())

        Map<String, File> weekReportFiles = filesService.getWeekReportFiles(timePeriod)
        File reportsFile = weekReportFiles.reportsFile


        List groupBalanceList = []
        List userBalanceList = []
        List vicketSourceBalanceList = []

        ScrollableResults scrollableResults = UserVS.createCriteria().scroll {//Check active users and users cancelled last week period
            or {
                isNull("dateCancelled")
                ge("dateCancelled", timePeriod.getDateFrom())
            }
        }
        while (scrollableResults.next()) {
            UserVS userVS = (UserVS) scrollableResults.get(0);

            if(userVS instanceof VicketSource) vicketSourceBalanceList.add(genBalanceForVicketSource(userVS, timePeriod))
            else if(userVS instanceof GroupVS) groupBalanceList.add(genBalanceForGroupVS(userVS, timePeriod))
            else userBalanceList.add(genBalanceForUserVS(userVS, timePeriod))

            if((scrollableResults.getRowNumber() % 100) == 0) {
                String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
                        System.currentTimeMillis() - begin)
                log.debug("userVS ${scrollableResults.getRowNumber()} of ${numTotalUsers} - ${elapsedTimeStr}");
                sessionFactory.currentSession.flush()
                sessionFactory.currentSession.clear()
            }
            if(((scrollableResults.getRowNumber() + 1) % 2000) == 0) {
                //accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
                //new File(accessRequestBaseDir).mkdirs()
            }


        }
        Map userBalances = [groupBalanceList:groupBalanceList, userBalanceList:userBalanceList, vicketSourceBalanceList:vicketSourceBalanceList]
        Map resultMap = [userBalances:userBalances]
        //transactionslog.info(new JSON(dataMap) + ",");
        reportsFile.write(new JSON(resultMap).toString())
        return new ResponseVS(ResponseVS.SC_OK)
    }

    private Map genBalanceForVicketSource(VicketSource vicketSource, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("genBalanceForVicketSource - id '${vicketSource.id}'")
        Map dataMap = vicketSourceService.getDetailedDataMap(vicketSource, timePeriod)
        if(vicketSource.state == UserVS.State.ACTIVE) {

        } else {}
        return dataMap
    }

    private Map genBalanceForGroupVS(GroupVS groupvs, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("genBalanceForGroupVS - id '${groupvs.id}'")
        Map dataMap = groupVSService.getDetailedDataMap(groupvs, timePeriod)
        if(groupvs.state == UserVS.State.ACTIVE) {

        } else {}
        return dataMap
    }

    private Map genBalanceForUserVS(UserVS uservs, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("genBalanceForUserVS - id '${uservs.id}'")
        Map dataMap = userVSService.getDetailedDataMap(uservs, timePeriod)
        if(uservs.state == UserVS.State.ACTIVE) {

        } else {}
        return dataMap
    }

}
