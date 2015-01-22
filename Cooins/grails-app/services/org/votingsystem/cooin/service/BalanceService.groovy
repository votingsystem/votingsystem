package org.votingsystem.cooin.service

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.hibernate.ScrollableResults
import org.votingsystem.cooin.model.CooinAccount
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.groovy.util.ReportFiles
import org.votingsystem.groovy.util.TransactionVSUtils
import org.votingsystem.model.*
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.MetaInfMsg

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

//@Transactional
class BalanceService {

    def systemService
    def messageSource
    def grailsApplication
    def sessionFactory
    def transactionVSService
    def groupVSService
    def userVSService
    def bankVSService
    def signatureVSService

    public initWeekPeriod(Calendar requestDate) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        long beginCalc = System.currentTimeMillis()
        //we know this is launch every Monday after 00:00 so we just make sure to select a day from last week
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(DateUtils.getDayFromPreviousWeek(requestDate))
        String transactionMsgSubject =  messageSource.getMessage('initWeekMsg',
                [DateUtils.getDayWeekDateStr(timePeriod.getDateTo())].toArray(), locale)
        int numTotalUsers = UserVS.countByDateCancelledIsNullOrDateCancelledGreaterThanEquals(timePeriod.getDateFrom())
        log.debug("$methodName - $transactionMsgSubject - numTotalUsers: '$numTotalUsers' - $timePeriod")
        ReportFiles reportFiles
        File errorsFile = ReportFiles.getReportErrorsFile(timePeriod, "$grailsApplication.config.vs.weekReportsPath");
        ScrollableResults scrollableResults = UserVS.createCriteria().scroll {//init week only with active users
            or {
                gt("dateCancelled", timePeriod.getDateFrom())
                isNull("dateCancelled")
            }
            inList("type", [UserVS.Type.USER, UserVS.Type.GROUP])
        }
        while (scrollableResults.next()) {
            try {
                UserVS userVS = (UserVS) scrollableResults.get(0);
                Map balanceMap
                String userSubPath
                if(userVS instanceof GroupVS) {
                    balanceMap = groupVSService.getDataWithBalancesMap(userVS, timePeriod)
                    userSubPath = "GroupVS_${userVS.id}"
                } else if (userVS instanceof UserVS) {
                    balanceMap = userVSService.getDataWithBalancesMap(userVS, timePeriod)
                    //userSubPath = StringUtils.getUserDirPath(userVS.getNif());
                    userSubPath = userVS.getNif();
                } else {
                    log.debug("$methodName - #### User type not valid for operation ${methodName} - UserVS id ${userVS.id}")
                    continue
                }
                reportFiles = new ReportFiles(timePeriod, "$grailsApplication.config.vs.weekReportsPath", userSubPath)
                log.debug("$methodName - week report for UserVS '$userVS.id' - dir: '$reportFiles.baseDir.absolutePath'")
                Map<String, Map> currencyMap = balanceMap.balancesCash
                for(String currency: currencyMap.keySet()) {
                    for(Map.Entry<String, BigDecimal> tagVSEntry:  currencyMap[currency].entrySet()) {
                        TagVS currentTagVS = systemService.getTag(tagVSEntry.key)
                        List<TransactionVS> transactionList = TransactionVS.createCriteria().list(offset: 0) {
                            ge("dateCreated", timePeriod.getDateFrom())
                            eq("type", TransactionVS.Type.COOIN_INIT_PERIOD)
                            eq("state", TransactionVS.State.OK)
                            eq("tag", currentTagVS)
                            eq("toUserVS", userVS)
                        }
                        if(!transactionList.isEmpty()) throw new ExceptionVS("REPEATED COOIN_INIT_PERIOD TransactionVS for " +
                                "UserVS: '${userVS.id}' - tag: '${tagVSEntry.key}' - timePeriod: '${timePeriod}'")
                        //Send TimeLimited incomes not expended to system
                        BigDecimal timeLimitedNotExpended = TransactionVSUtils.checkRemainingForTag(balanceMap.balancesFrom,
                                balanceMap.balancesTo, currentTagVS.getName(), currency)
                        BigDecimal amountResult = new BigDecimal(tagVSEntry.getValue()).subtract(timeLimitedNotExpended)
                        String signedMessageSubject =  messageSource.getMessage('tagInitPeriodMsg',
                                [tagVSEntry.getKey()].toArray(), locale)
                        ResponseVS responseVS = signatureVSService.getSMIMETimeStamped (systemService.getSystemUser().name,
                                userVS.getNif(), TransactionVS.getInitPeriodTransactionVSData(amountResult,
                                timeLimitedNotExpended, tagVSEntry.getKey(), userVS).toString(),
                                "${transactionMsgSubject} - ${signedMessageSubject}")
                        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(
                                "${methodName} - error signing system transaction - ${responseVS.getMessage()}")
                        MessageSMIME messageSMIME = new MessageSMIME(userVS:systemService.getSystemUser(),
                                smimeMessage:responseVS.getSMIME(), type:TypeVS.COOIN_INIT_PERIOD).save()
                        new TransactionVS(amount: amountResult, fromUserVS:userVS, fromUserIBAN: userVS.IBAN,
                                toUserIBAN: userVS.IBAN, messageSMIME:messageSMIME, toUserVS: userVS,
                                state:TransactionVS.State.OK, subject: signedMessageSubject, currencyCode: currency,
                                type:TransactionVS.Type.COOIN_INIT_PERIOD, tag:currentTagVS).save()
                        if(timeLimitedNotExpended.compareTo(BigDecimal.ZERO) > 0) {
                            CooinAccount account = CooinAccount.findWhere(userVS:userVS, tag:currentTagVS)
                            new TransactionVS(amount: timeLimitedNotExpended, fromUserVS:userVS, fromUserIBAN:
                                    userVS.IBAN, currencyCode: currency, tag:currentTagVS,
                                    toUserIBAN: systemService.getSystemUser().IBAN, messageSMIME:messageSMIME,
                                    toUserVS: systemService.getSystemUser(), state:TransactionVS.State.OK,
                                    subject: signedMessageSubject, type:TransactionVS.Type.COOIN_INIT_PERIOD_TIME_LIMITED,
                                    accountFromMovements:[(account):timeLimitedNotExpended]).save()
                        }
                        responseVS.getSMIME().writeTo(new FileOutputStream(reportFiles.getTagReceiptFile(tagVSEntry.getKey())))
                    }
                }
                if((scrollableResults.getRowNumber() % 100) == 0) {
                    String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
                            System.currentTimeMillis() - beginCalc)
                    log.debug("${methodName} - ${scrollableResults.getRowNumber()} of ${numTotalUsers} - ${elapsedTimeStr}");
                    sessionFactory.currentSession.flush()
                    sessionFactory.currentSession.clear()
                }
                if(((scrollableResults.getRowNumber() + 1) % 2000) == 0) {
                    //accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
                    //new File(accessRequestBaseDir).mkdirs()
                }
            } catch(Exception ex) {
                log.error(ex.getMessage());
                errorsFile.append("${StackTraceUtils.extractRootCause(ex)} ${System.getProperty("line.separator")}")
            }
        }
        signPeriodResult(timePeriod)
    }

    //@Transactional
    public ResponseVS signPeriodResult(DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("$methodName - timePeriod: ${timePeriod.toString()}")
        long beginCalc = System.currentTimeMillis()
        int numTotalUsers = UserVS.countByDateCancelledIsNullOrDateCancelledGreaterThanEquals(timePeriod.getDateFrom())
        ReportFiles reportFiles = new ReportFiles(timePeriod, grailsApplication.config.vs.weekReportsPath, null)
        List groupVSBalanceList = []
        List userVSBalanceList = []
        List bankVSBalanceList = []

        ScrollableResults scrollableResults = UserVS.createCriteria().scroll {//Check active users and users cancelled last week period
            or {
                isNull("dateCancelled")
                ge("dateCancelled", timePeriod.getDateFrom())
            }
            and {
                inList("type", [UserVS.Type.USER, UserVS.Type.GROUP, UserVS.Type.BANKVS])
            }
        }
        while (scrollableResults.next()) {
            UserVS userVS = (UserVS) scrollableResults.get(0);
            if(userVS instanceof BankVS) bankVSBalanceList.add(bankVSService.getDataWithBalancesMap(userVS, timePeriod))
            else if(userVS instanceof GroupVS) groupVSBalanceList.add(groupVSService.getDataWithBalancesMap(userVS, timePeriod))
            else userVSBalanceList.add(userVSService.getDataWithBalancesMap(userVS, timePeriod))

            if((scrollableResults.getRowNumber() % 100) == 0) {
                String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
                        System.currentTimeMillis() - beginCalc)
                log.debug("userVS ${scrollableResults.getRowNumber()} of ${numTotalUsers} - elapsedTime: '$elapsedTimeStr'");
                sessionFactory.currentSession.flush()
                sessionFactory.currentSession.clear()
            }
            if(((scrollableResults.getRowNumber() + 1) % 2000) == 0) {
                //accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
                //new File(accessRequestBaseDir).mkdirs()
            }
        }

        Map systemBalance = systemService.genBalanceForSystem(timePeriod)
        Map userBalances = [systemBalance:systemBalance, groupVSBalanceList:groupVSBalanceList,
                        userVSBalanceList:userVSBalanceList, bankVSBalanceList:bankVSBalanceList]
        Map resultMap = [timePeriod:timePeriod.toJSON(),userBalances:userBalances]
        //transactionslog.info(new JSON(dataMap) + ",");
        JSON userBalancesJSON = new JSON(resultMap)
        reportFiles.jsonFile.write(userBalancesJSON.toString())
        String subject =  messageSource.getMessage('periodBalancesReportMsgSubject',
                ["[${DateUtils.getDateStr(timePeriod.getDateFrom())} - ${DateUtils.getDateStr(timePeriod.getDateTo())}]"].toArray(),
                locale)
        ResponseVS responseVS = signatureVSService.getSMIMETimeStamped (systemService.getSystemUser().name, null,
                userBalancesJSON.toString(), subject)
        responseVS.getSMIME().writeTo(new FileOutputStream(reportFiles.receiptFile))
        String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
                System.currentTimeMillis() - beginCalc)
        log.debug("$methodName - numTotalUsers: '${numTotalUsers}' - finished in '${elapsedTimeStr}'")
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.OK, data:resultMap, contentType: ContentTypeVS.JSON,
                metaInf:MetaInfMsg.getOKMsg(methodName, timePeriod.toString()))
    }

    public Map genBalance(UserVS uservs, DateUtils.TimePeriod timePeriod) {
        if(UserVS.Type.SYSTEM == uservs.type) return systemService.genBalanceForSystem(timePeriod)
        else if(uservs instanceof BankVS) return bankVSService.getDataWithBalancesMap(uservs, timePeriod)
        else if(uservs instanceof GroupVS) return groupVSService.getDataWithBalancesMap(uservs, timePeriod)
        else return userVSService.getDataWithBalancesMap(uservs, timePeriod)
    }

}