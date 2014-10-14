package org.votingsystem.vicket.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.hibernate.ScrollableResults
import org.votingsystem.model.*
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.UserVSAccount

//@Transactional
class BalanceService {

    private static final CLASS_NAME = BalanceService.class.getSimpleName()

    def systemService
    def messageSource
    def grailsApplication
    def sessionFactory
    def transactionVSService
    def groupVSService
    def userVSService
    def bankVSService
    def filesService
    def signatureVSService
    Map<String, TagVS> tagMap = [:]

    public TagVS getTag(String tagName) throws Exception{
        if(!tagMap[tagName]) {
            TagVS tag = TagVS.findWhere(name:tagName)
            if(!tag) throw new Exception("TagVS with name '${tagName}' not found")
            tagMap[(tagName)] = tag
        }
        return tagMap[tagName]
    }

    public initWeekPeriod() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        long beginCalc = System.currentTimeMillis()
        //we know this is launch every Monday after 00:00 so we just make sure to select a day from last week
        Date oneDayLastWeek = org.votingsystem.util.DateUtils.addDays(Calendar.getInstance().getTime(), -3)
        DateUtils.TimePeriod timePeriod = org.votingsystem.util.DateUtils.getWeekPeriod(oneDayLastWeek)
        DateUtils.TimePeriod currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
        String currentWeekStr = DateUtils.getDateStr(currentWeekPeriod.getDateFrom(), "dd MMM yyyy")
        String transactionMsgSubject =  messageSource.getMessage('initWeekMsg', [currentWeekStr].toArray(),
                systemService.getDefaultLocale())

        Map globalDataMap = [:]
        int numTotalUsers = UserVS.countByDateCancelledIsNullOrDateCancelledGreaterThanEquals(timePeriod.getDateFrom())
        log.debug("$methodName - Initializing week '$transactionMsgSubject' - numTotalUsers: '$numTotalUsers'")

        Map<String, File> weekReportFiles
        ScrollableResults scrollableResults = UserVS.createCriteria().scroll {//init week only with active users
            or {
                gt("dateCancelled", timePeriod.getDateFrom())
                isNull("dateCancelled")
            }
            inList("type", [UserVS.Type.USER, UserVS.Type.GROUP, UserVS.Type.REPRESENTATIVE])
        }

        while (scrollableResults.next()) {
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
            }

            weekReportFiles = filesService.getWeekReportFiles(timePeriod, userSubPath)
            //[baseDir:baseDir, reportsFile:new File("${baseDirPath}/balances.json"), systemReceipt:receiptFile]
            log.debug("$methodName - Making data for UserVS '$userVS.nif' - dir: '$weekReportFiles.baseDir'")
            Map<String, Map> currencyMap = balanceMap.balancesCash
            Set<Map.Entry<String, Map>> currencyEntries = currencyMap.entrySet()
            currencyEntries.each { currency ->
                Map<String, BigDecimal> tagVSMap = currency.getValue()
                for(Map.Entry<String, BigDecimal> tagVSEntry: tagVSMap.entrySet()) {
                    TagVS currentTagVS = getTag(tagVSEntry.key)
                    List<TransactionVS> transactionList = TransactionVS.createCriteria().list(offset: 0) {
                        ge("dateCreated", timePeriod.getDateFrom())
                        eq("type", TransactionVS.Type.VICKET_INIT_PERIOD)
                        eq("state", TransactionVS.State.OK)
                        eq("tag", currentTagVS)
                        eq("toUserVS", userVS)
                    }
                    if(!transactionList.isEmpty()) throw new ExceptionVS("REPEATED VICKET_INIT_PERIOD TransactionVS for " +
                            "UserVS: '${userVS.id}' - tag: '${tagVSEntry.key}' - timePeriod: '${timePeriod}'")
                    //Send TimeLimited incomes not exepnded to system
                    BigDecimal timeLimitedNotExpended = checkRemainingForTag(balanceMap.balancesFrom,
                            balanceMap.balancesTo, currentTagVS.getName(), currency.getKey())

                    BigDecimal amountResult = tagVSEntry.getValue().subtract(timeLimitedNotExpended)

                    String signedMessageSubject =  messageSource.getMessage('transactionvsForTagMsg',
                            [tagVSEntry.getKey()].toArray(), systemService.getDefaultLocale())
                    Map transactionData = [operation:TypeVS.VICKET_INIT_PERIOD , amount:amountResult, tag:tagVSEntry.getKey(),
                               timeLimitedNotExpended:timeLimitedNotExpended, toUserVS: userVS.name, toUserNIF:userVS.nif,
                               toUserId:userVS.id, toUserIBAN:[userVS.IBAN], UUID:UUID.randomUUID()]
                    ResponseVS responseVS = signatureVSService.getTimestampedSignedMimeMessage (systemService.getSystemUser().name,
                            userVS.getNif(), new JSONObject(transactionData).toString(), "${transactionMsgSubject} - ${signedMessageSubject}")

                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(
                            "${methodName} - error signing system transaction - ${responseVS.getMessage()}")

                    MessageSMIME messageSMIME = new MessageSMIME(userVS:systemService.getSystemUser(),
                            smimeMessage:responseVS.getSmimeMessage(), type:TypeVS.VICKET_INIT_PERIOD,
                            content:responseVS.getSmimeMessage().getBytes(),
                            base64ContentDigest:responseVS.getSmimeMessage().getContentDigestStr())
                    messageSMIME.save()

                   new TransactionVS(amount: amountResult, fromUserVS:userVS, fromUserIBAN: userVS.IBAN,
                           toUserIBAN: userVS.IBAN, messageSMIME:messageSMIME, toUserVS: userVS,
                           state:TransactionVS.State.OK, subject: signedMessageSubject, currencyCode: currency.getKey(),
                           type:TransactionVS.Type.VICKET_INIT_PERIOD, tag:currentTagVS).save()
                    if(timeLimitedNotExpended.compareTo(BigDecimal.ZERO) > 0) {
                        TransactionVS transactionVS = new TransactionVS(amount: timeLimitedNotExpended,
                                fromUserVS:userVS, fromUserIBAN: userVS.IBAN,
                                toUserIBAN: systemService.getSystemUser().IBAN, messageSMIME:messageSMIME,
                                toUserVS: systemService.getSystemUser(), state:TransactionVS.State.OK,
                                subject: signedMessageSubject, type:TransactionVS.Type.VICKET_INIT_PERIOD_TIME_LIMITED,
                                currencyCode: currency.getKey(), tag:currentTagVS).save()
                        UserVSAccount account = UserVSAccount.findWhere(userVS:userVS, tag:currentTagVS)
                        transactionVS.addAccountFromMovement(account, timeLimitedNotExpended)
                    }
                    File tagReceiptFile = new File("${((File)weekReportFiles.baseDir).getAbsolutePath()}/transaction_tag_${tagVSEntry.getKey()}.p7s")
                    responseVS.getSmimeMessage().writeTo(new FileOutputStream(tagReceiptFile))
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
        }
        calculatePeriod(timePeriod)
    }

    private BigDecimal checkRemainingForTag(Map balancesFrom, Map balancesTo, String tagName, String currencyCode) {
        BigDecimal result = BigDecimal.ZERO;
        if(balancesTo[currencyCode] && balancesTo[currencyCode][tagName]?.timeLimited)
            result = result.add(new BigDecimal(balancesTo[currencyCode][tagName].timeLimited))
        if(balancesFrom[currencyCode] && balancesFrom[currencyCode][tagName])
            result = result.subtract(new BigDecimal(balancesFrom[currencyCode][tagName]))
        if(result.compareTo(BigDecimal.ZERO) < 0) throw new ExceptionVS("Negative period balance for tag '$tagName' " +
                "'$currencyCode': ${result.toString()} ")
        return result;
    }

    //@Transactional
    public ResponseVS calculatePeriod(DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("$methodName - timePeriod: ${timePeriod.toString()}")

        long beginCalc = System.currentTimeMillis()
        int numTotalUsers = UserVS.countByDateCancelledIsNullOrDateCancelledGreaterThanEquals(timePeriod.getDateFrom())

        Map<String, File> weekReportFiles = filesService.getWeekReportFiles(timePeriod, null)
        File reportsFile = weekReportFiles.reportsFile

        List groupBalanceList = []
        List userBalanceList = []
        List bankVSBalanceList = []

        ScrollableResults scrollableResults = UserVS.createCriteria().scroll {//Check active users and users cancelled last week period
            or {
                isNull("dateCancelled")
                ge("dateCancelled", timePeriod.getDateFrom())
            }
            and {
                inList("type", [UserVS.Type.USER, UserVS.Type.GROUP, UserVS.Type.REPRESENTATIVE])
            }
        }
        while (scrollableResults.next()) {
            UserVS userVS = (UserVS) scrollableResults.get(0);

            if(userVS instanceof BankVS) bankVSBalanceList.add(bankVSService.getDataWithBalancesMap(userVS, timePeriod))
            else if(userVS instanceof GroupVS) groupBalanceList.add(groupVSService.getDataWithBalancesMap(userVS, timePeriod))
            else userBalanceList.add(userVSService.getDataWithBalancesMap(userVS, timePeriod))

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
        Map userBalances = [systemBalance:systemBalance, groupBalanceList:groupBalanceList,
                        userBalanceList:userBalanceList, bankVSBalanceList:bankVSBalanceList]
        Map resultMap = [userBalances:userBalances]
        //transactionslog.info(new JSON(dataMap) + ",");
        JSON userBalancesJSON = new JSON(resultMap)
        reportsFile.write(userBalancesJSON.toString())

        String subject =  messageSource.getMessage('periodBalancesReportMsgSubject',
                ["[${DateUtils.getDateStr(timePeriod.getDateFrom())} - ${DateUtils.getDateStr(timePeriod.getDateTo())}]"].toArray(),
                systemService.getDefaultLocale())
        ResponseVS responseVS = signatureVSService.getTimestampedSignedMimeMessage (systemService.getSystemUser().name,
                "", userBalancesJSON.toString(),subject)
        responseVS.getSmimeMessage().writeTo(new FileOutputStream(weekReportFiles.systemReceipt))
        String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
                System.currentTimeMillis() - beginCalc)
        log.debug("$methodName - numTotalUsers: '${numTotalUsers}' - finished in '${elapsedTimeStr}'")
        return responseVS
    }

    public Map genBalance(UserVS uservs, DateUtils.TimePeriod timePeriod) {
        if(UserVS.Type.SYSTEM == uservs.type) return systemService.genBalanceForSystem(timePeriod)
        else if(uservs instanceof BankVS) return bankVSService.getDataWithBalancesMap(uservs, timePeriod)
        else if(uservs instanceof GroupVS) return groupVSService.getDataWithBalancesMap(uservs, timePeriod)
        else return userVSService.getDataWithBalancesMap(uservs, timePeriod)
    }


}