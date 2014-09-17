package org.votingsystem.vicket.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.hibernate.ScrollableResults
import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.BankVS
import org.votingsystem.model.VicketTagVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.StringUtils
import org.votingsystem.vicket.model.TransactionVS

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
    def filesService
    def signatureVSService
    Map<String, VicketTagVS> tagMap = [:]

    public VicketTagVS getTag(String tagName) throws Exception{
        if(!tagMap[tagName]) {
            VicketTagVS tag = VicketTagVS.findWhere(name:tagName)
            if(!tag) throw new Exception("VicketTagVS with name '${tagName}' not found")
            tagMap[(tagName)] = tag
        } else return tagMap[tagName]
    }

    public initWeekPeriod() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        long beginCalc = System.currentTimeMillis()
        Locale defaultLocale = new Locale(grailsApplication.config.VotingSystem.defaultLocale)
        //we know this is launch every Monday after 00:00 so we just make sure to select a day from last week
        //we know this is launch every Monday at 00:00 so we just make sure to select a day from last week to select the period
        Date oneDayLastWeek = org.votingsystem.util.DateUtils.getDatePlus(-3)
        DateUtils.TimePeriod timePeriod = org.votingsystem.util.DateUtils.getWeekPeriod(oneDayLastWeek)
        DateUtils.TimePeriod currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
        Calendar cal = Calendar.getInstance();
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        String subject =  messageSource.getMessage('initWeekMsg', [week].toArray(), defaultLocale)

        Map globalDataMap = [:]
        int numTotalUsers = UserVS.countByDateCancelledIsNullOrDateCancelledGreaterThanEquals(timePeriod.getDateFrom())

        Map<String, File> weekReportFiles
        ScrollableResults scrollableResults = UserVS.createCriteria().scroll {//init week only with active users
            eq("state", UserVS.State.ACTIVE)
            not {
                eq("type", UserVS.Type.SYSTEM)
                eq("type", UserVS.Type.VICKET_SOURCE)
            }
        }

        while (scrollableResults.next()) {
            UserVS userVS = (UserVS) scrollableResults.get(0);
            Map balanceMap
            String userSubPath
            if(userVS instanceof GroupVS) {
                balanceMap = genBalanceForGroupVS(userVS, timePeriod)
                userSubPath = "GroupVS_${userVS.id}"
            } else if (userVS instanceof UserVS) {
                balanceMap = genBalanceForUserVS(userVS, timePeriod)
                userSubPath = StringUtils.getUserDirPath(userVS.getNif());
            }
            else throw new Exception("User type not valid for operation ${methodName} - UserVS id ${userVS.id}")

            weekReportFiles = filesService.getWeekReportFiles(timePeriod, userSubPath)
            //[baseDir:baseDir, reportsFile:new File("${baseDirPath}/balances.json"), systemReceipt:receiptFile]

            Map<String, Map> balanceResult = balanceMap.balanceResult
            Set<Map.Entry<String, Map>> mapEntries = balanceResult.entrySet()
            mapEntries.each { currency ->
                Map<String, BigDecimal> currencyMap = currency.getValue()
                Set<Map.Entry<String, BigDecimal>> currencyEntries = currencyMap.entrySet()
                currencyEntries.each {
                    String signedMessageSubject =  messageSource.getMessage('transactionvsForTagMsg', [it.getKey()].toArray(), defaultLocale)
                    Map transactionData = [amount:it.getValue(), tag:it.getKey(), toUserVS: userVS.name,
                               toUserNIF:userVS.nif, toUserId:userVS.id, toUserIBAN:userVS.IBAN, UUID:UUID.randomUUID()]
                    ResponseVS responseVS = signatureVSService.getTimestampedSignedMimeMessage (systemService.getSystemUser().name,
                            userVS.getNif(), new JSONObject(transactionData).toString(), "${subject} - ${signedMessageSubject}")

                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new Exception(
                            "${methodName} - error signing system transaction - ${responseVS.getMessage()}")

                    MessageSMIME messageSMIME = new MessageSMIME(userVS:systemService.getSystemUser(),
                            smimeMessage:responseVS.getSmimeMessage(), type:TypeVS.VICKET_INIT_PERIOD,
                            content:responseVS.getSmimeMessage().getBytes(),
                            base64ContentDigest:responseVS.getSmimeMessage().getContentDigestStr())
                    messageSMIME.save()

                    log.debug("====== it: ${it}")

                    TransactionVS transactionVS = new TransactionVS(amount: it.getValue(), fromUserVS:systemService.getSystemUser(),
                            fromUserIBAN: systemService.getSystemUser().IBAN, toUserIBAN: userVS.IBAN, validTo: currentWeekPeriod.dateTo,
                            messageSMIME:messageSMIME, toUserVS: userVS, state:TransactionVS.State.OK, subject:subject,
                            type:TransactionVS.Type.INIT_PERIOD, currencyCode: currency.getKey(), tag:getTag(it.getKey())).save()
                    File tagReceiptFile = new File("${((File)weekReportFiles.baseDir).getAbsolutePath()}/transaction_tag_${it.getKey()}.p7s")
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

    //@Transactional
    public ResponseVS calculatePeriod(DateUtils.TimePeriod timePeriod) {
        log.debug("calculatePeriod - timePeriod: ${timePeriod.toString()}")

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
            not {
                eq("type", UserVS.Type.SYSTEM)
            }
        }
        while (scrollableResults.next()) {
            UserVS userVS = (UserVS) scrollableResults.get(0);

            if(userVS instanceof BankVS) bankVSBalanceList.add(genBalanceForBankVS(userVS, timePeriod))
            else if(userVS instanceof GroupVS) groupBalanceList.add(genBalanceForGroupVS(userVS, timePeriod))
            else userBalanceList.add(genBalanceForUserVS(userVS, timePeriod))

            if((scrollableResults.getRowNumber() % 100) == 0) {
                String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
                        System.currentTimeMillis() - beginCalc)
                log.debug("userVS ${scrollableResults.getRowNumber()} of ${numTotalUsers} - ${elapsedTimeStr}");
                sessionFactory.currentSession.flush()
                sessionFactory.currentSession.clear()
            }
            if(((scrollableResults.getRowNumber() + 1) % 2000) == 0) {
                //accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
                //new File(accessRequestBaseDir).mkdirs()
            }

        }

        Map systemBalance = genBalanceForSystem(systemService.getSystemUser(), timePeriod)
        Map userBalances = [systemBalance:systemBalance, groupBalanceList:groupBalanceList,
                        userBalanceList:userBalanceList, bankVSBalanceList:bankVSBalanceList]
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
        String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
                System.currentTimeMillis() - beginCalc)
        log.debug("calculatePeriod - numTotalUsers: '${numTotalUsers}' - finished in '${elapsedTimeStr}'")
        return responseVS
    }

    public Map genBalance(UserVS uservs, DateUtils.TimePeriod timePeriod) {
        if(UserVS.Type.SYSTEM == uservs.type) return genBalanceForSystem(uservs, timePeriod)
        if(uservs instanceof BankVS) return genBalanceForBankVS(uservs, timePeriod)
        else if (uservs instanceof GroupVS) return genBalanceForGroupVS(uservs, timePeriod)
        else return genBalanceForUserVS(uservs, timePeriod)
    }

    private Map genBalanceForBankVS(BankVS bankVS, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("genBalanceForBankVS - id '${bankVS.id}'")
        Map dataMap = bankVSService.getDetailedDataMapWithBalances(bankVS, timePeriod)
        if(bankVS.state == UserVS.State.ACTIVE) {

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

    private Map genBalanceForSystem(UserVS systemUser, DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("genBalanceForSystem - timePeriod [${timePeriod.toString()}]")
        Map resultMap = userVSService.getUserVSDataMap(systemUser)

        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            isNull('transactionParent')
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        def transactionFromList = []
        Map<String, Map> balancesMap = [:]
        transactionList.each { transaction ->
            if(balancesMap[transaction.currencyCode]) {
                Map<String, BigDecimal> currencyMap = balancesMap[transaction.currencyCode]
                if(currencyMap[transaction.tag.name]) {
                    currencyMap[transaction.tag.name] = ((BigDecimal) currencyMap[transaction.tag.name]).add(transaction.amount)
                } else currencyMap[(transaction.tag.name)] = transaction.amount
            } else {
                Map<String, BigDecimal> currencyMap = [(transaction.tag.name):transaction.amount]
                balancesMap[(transaction.currencyCode)] = currencyMap
            }
            transactionFromList.add(transactionVSService.getTransactionMap(transaction))
        }
        resultMap.transactionFromList = transactionFromList
        resultMap.balancesFrom = balancesMap

        transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            isNotNull('transactionParent')
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }

        def transactionToList = []
        balancesMap = [:]
        transactionList.each { transaction ->
            if(balancesMap[transaction.currencyCode]) {
                Map<String, BigDecimal> currencyMap = balancesMap[transaction.currencyCode]
                if(currencyMap[transaction.tag.name]) {
                    currencyMap[transaction.tag.name] = ((BigDecimal) currencyMap[transaction.tag.name]).add(transaction.amount)
                } else currencyMap[(transaction.tag.name)] = transaction.amount
            } else {
                Map<String, BigDecimal> currencyMap = [(transaction.tag.name):transaction.amount]
                balancesMap[(transaction.currencyCode)] = currencyMap
            }
            transactionToList.add(transactionVSService.getTransactionMap(transaction))
        }

        resultMap.transactionToList = transactionToList
        resultMap.balancesTo = balancesMap

        return resultMap
    }

}