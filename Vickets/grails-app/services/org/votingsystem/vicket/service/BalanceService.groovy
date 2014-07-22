package org.votingsystem.vicket.service

import grails.converters.JSON
import org.hibernate.ScrollableResults
import org.votingsystem.model.GroupVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketSource
import org.votingsystem.util.DateUtils

//@Transactional
class BalanceService {

    def systemService
    def messageSource
    def grailsApplication
    def sessionFactory
    def transactionVSService
    def groupVSService
    def userVSService
    def vicketSourceService
    def filesService
    def signatureVSService

    //@Transactional
    public ResponseVS calculatePeriod(DateUtils.TimePeriod timePeriod) {
        log.debug("calculatePeriod - timePeriod: ${timePeriod.toString()}")
        //we know this is launch every Monday at 00:00 so we just make sure to select a day from last week
        long begin = System.currentTimeMillis()


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
        JSON userBalancesJSON = new JSON(resultMap)
        reportsFile.write(userBalancesJSON.toString())
        Locale defaultLocale = new Locale(grailsApplication.config.VotingSystem.defaultLocale)

        String subject =  messageSource.getMessage('periodBalancesReportMsgSubject',
                ["[${DateUtils.getStringFromDate(timePeriod.getDateFrom())} - ${DateUtils.getStringFromDate(timePeriod.getDateTo())}]"].toArray(), defaultLocale)
        ResponseVS responseVS = signatureVSService.getTimestampedSignedMimeMessage (systemService.getSystemUser().name,
                "", userBalancesJSON.toString(),subject)
        responseVS.getSmimeMessage().writeTo(new FileOutputStream(weekReportFiles.systemReceipt))
        return responseVS
    }

    private Map genBalanceForVicketSource(VicketSource vicketSource, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("genBalanceForVicketSource - id '${vicketSource.id}'")
        Map dataMap = vicketSourceService.getDetailedDataMapWithBalances(vicketSource, timePeriod)
        if(vicketSource.state == UserVS.State.ACTIVE) {

        } else {}
        return dataMap
    }

    private Map genBalanceForGroupVS(GroupVS groupvs, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("genBalanceForGroupVS - id '${groupvs.id}'")
        Map dataMap = groupVSService.getDetailedDataMapWithBalances(groupvs, timePeriod)
        //Now we calculate balances for each tag and make the beginning of period adjustment


        if(groupvs.state == UserVS.State.ACTIVE) {

        } else {}
        return dataMap
    }

    private Map genBalanceForUserVS(UserVS uservs, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("genBalanceForUserVS - id '${uservs.id}'")
        Map dataMap = userVSService.getDetailedDataMapWithBalances(uservs, timePeriod)
        if(uservs.state == UserVS.State.ACTIVE) {

        } else {}
        return dataMap
    }

}
