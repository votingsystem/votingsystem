package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.currency.util.ReportFiles;
import org.votingsystem.web.currency.util.TransactionVSUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class BalancesBean {

    private static Logger log = Logger.getLogger(BalancesBean.class.getSimpleName());

    private static final DateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    @Inject ConfigVS config;
    @Inject GroupVSBean groupVSBean;
    @Inject SignatureBean signatureBean;
    @Inject BankVSBean bankVSBean;
    @Inject UserVSBean userVSBean;
    @Inject DAOBean dao;
    @Inject SystemBean systemBean;
    @Inject MessagesBean messages;

    public void initWeekPeriod(Calendar requestDate) throws IOException {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        long beginCalc = System.currentTimeMillis();
        //we know this is launch every Monday after 00:00 so we just make sure to select a day from last week
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(DateUtils.getDayFromPreviousWeek(requestDate));
        String transactionMsgSubject =  messages.get("initWeekMsg", DateUtils.getDayWeekDateStr(timePeriod.getDateTo()));
        Query query = dao.getEM().createNamedQuery("countUserActiveOrCancelledAfter").setParameter("dateCancelled", timePeriod.getDateFrom());
        long numTotalUsers = (long)query.getSingleResult();
        log.info(transactionMsgSubject + " - numTotalUsers:" + numTotalUsers + " - " + timePeriod.toString());
        String dateFromPathPart = fileDateFormatter.format(timePeriod.getDateFrom());
        String dateToPathPart = fileDateFormatter.format(timePeriod.getDateTo());
        String reportsBasePath = config.getServerDir().getAbsolutePath() + "/backup/weekReports";
        String weekReportsLogPath = reportsBasePath + "/" + dateFromPathPart + "_" + dateToPathPart + "/initWeekPeriod.log";
        Logger weekPeriodLogger = LoggerVS.getLogger(weekReportsLogPath, "org.votingsysem.currency.initWeekPeriod");
        int offset = 0;
        int pageSize = 100;
        List<UserVS> userVSList;
        query = dao.getEM().createNamedQuery("findUserActiveOrCancelledAfterAndInList").setParameter("dateCancelled",
                timePeriod.getDateFrom()).setParameter("inList", Arrays.asList(UserVS.Type.USER, UserVS.Type.GROUP))
                .setFirstResult(0).setMaxResults(pageSize);
        while ((userVSList = query.getResultList()).size() > 0) {
            for (UserVS userVS : userVSList) {
                try {
                    if(!initUserVSWeekPeriod(userVS, timePeriod, transactionMsgSubject)) continue;
                } catch(Exception ex) {
                    weekPeriodLogger.log(Level.SEVERE, "userVS: " + userVS.getId() +  ex.getMessage(), ex);
                }
            }
            if((offset % 2000) == 0) {
                //accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
                //new File(accessRequestBaseDir).mkdirs()
            }
            dao.getEM().flush();
            dao.getEM().clear();
            offset += pageSize;
            query.setFirstResult(offset);
            String elapsedTime = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc);
            log.info("processed " + offset + " of " + numTotalUsers + " - elapsedTime: " + elapsedTime);
        }
    }

    public boolean initUserVSWeekPeriod(UserVS userVS, DateUtils.TimePeriod timePeriod, String transactionMsgSubject)
            throws Exception {
        Map balanceMap = null;
        String userSubPath;
        if(userVS instanceof GroupVS) {
            balanceMap = groupVSBean.getDataWithBalancesMap(userVS, timePeriod);
            userSubPath = "GroupVS_"+ userVS.getId();
        } else if (userVS instanceof UserVS) {
            balanceMap = userVSBean.getDataWithBalancesMap(userVS, timePeriod);
            //userSubPath = StringUtils.getUserDirPath(userVS.getNif());
            userSubPath = userVS.getNif();
        } else {
            log.info("userVS id: " + userVS.getId() + " is " + userVS.getClass().getSimpleName() + " - no init period");
            return false;
        }
        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getServerDir().getAbsolutePath(), userSubPath);
        log.info("$methodName - UserVS '$userVS.id' - dir: '$reportFiles.baseDir.absolutePath'");
        Map<String, Map> currencyMap = (Map<String, Map>) balanceMap.get("balancesCash");
        Query query = null;
        for(String currency: currencyMap.keySet()) {
            for(Object entry:  currencyMap.get(currency).entrySet()) {
                Map.Entry<String, BigDecimal> tagVSEntry = (Map.Entry<String, BigDecimal>)entry;
                TagVS currentTagVS = config.getTag(tagVSEntry.getKey());
                query = dao.getEM().createNamedQuery("findTransByToUserAndStateAndTypeAndTagAndDateCreatedAfter")
                        .setParameter("toUserVS", userVS).setParameter("state", TransactionVS.State.OK)
                        .setParameter("type", TransactionVS.Type.CURRENCY_INIT_PERIOD)
                        .setParameter("tag", currentTagVS).setParameter("dateFrom", timePeriod.getDateTo());

                long numInitPeriodTransaction = (long) query.getSingleResult();
                if(numInitPeriodTransaction > 0) throw new ExceptionVS("REPEATED CURRENCY_INIT_PERIOD TransactionVS for " +
                        "UserVS:" + userVS.getId() + " - tag: " + tagVSEntry.getKey() + " - timePeriod:" + timePeriod);
                //Send TimeLimited incomes not expended to system
                BigDecimal timeLimitedNotExpended = TransactionVSUtils.checkRemainingForTag(
                        (Map<String, Map<String, String>>)balanceMap.get("balancesFrom"),
                        (Map<String, Map<String, Map>>)balanceMap.get("balancesTo"), currentTagVS.getName(), currency);
                if(TagVS.WILDTAG.equals(currentTagVS.getName()) &&
                        timeLimitedNotExpended.compareTo(BigDecimal.ZERO) < 0) timeLimitedNotExpended = BigDecimal.ZERO;
                BigDecimal amountResult = tagVSEntry.getValue().subtract(timeLimitedNotExpended);
                String signedMessageSubject =  messages.get("tagInitPeriodMsg", tagVSEntry.getKey());
                SMIMEMessage smimeMessage = signatureBean.getSMIMETimeStamped (signatureBean.getSystemUser().getName(),
                        userVS.getNif(), TransactionVS.getInitPeriodTransactionVSData(amountResult,
                        timeLimitedNotExpended, tagVSEntry.getKey(), userVS).toString(),
                        transactionMsgSubject + " - " + signedMessageSubject);
                MessageSMIME messageSMIME = dao.persist(new MessageSMIME(smimeMessage, signatureBean.getSystemUser(),
                        TypeVS.CURRENCY_INIT_PERIOD));
                dao.persist(new TransactionVS(userVS, userVS, amountResult, currency, signedMessageSubject, messageSMIME,
                        TransactionVS.Type.CURRENCY_INIT_PERIOD, TransactionVS.State.OK, currentTagVS));
                if(timeLimitedNotExpended.compareTo(BigDecimal.ZERO) > 0) {
                    query = dao.getEM().createNamedQuery("findAccountByUserIBANAndStateAndCurrencyAndTag")
                            .setParameter("userIBAN", userVS.getIBAN()).setParameter("state", CurrencyAccount.State.ACTIVE)
                            .setParameter("currencyCode", currency).setParameter("tag", currentTagVS);
                    CurrencyAccount account = dao.getSingleResult (CurrencyAccount.class, query);
                    Map accountFromMovements = new HashMap<>();
                    accountFromMovements.put(account, timeLimitedNotExpended);
                    TransactionVS transactionVS = dao.persist(new TransactionVS(userVS, signatureBean.getSystemUser(),
                            timeLimitedNotExpended, currency,
                            signedMessageSubject, messageSMIME,TransactionVS.Type.CURRENCY_INIT_PERIOD_TIME_LIMITED,
                            TransactionVS.State.OK,currentTagVS ));
                    transactionVS.setAccountFromMovements(accountFromMovements);
                }
                File outputFile = reportFiles.getTagReceiptFile(tagVSEntry.getKey());
                log.info(currency + " - " + currentTagVS.getName() + " - result: " + outputFile.getAbsolutePath());
                smimeMessage.writeTo(new FileOutputStream(outputFile));
            }
        }
        return true;
    }

    //@Transactional
    public Map signPeriodResult(DateUtils.TimePeriod timePeriod) throws Exception {
        long beginCalc = System.currentTimeMillis();
        Query query = dao.getEM().createNamedQuery("countUserActiveByDateAndInList").setParameter("date", timePeriod.getDateFrom())
                .setParameter("inList", Arrays.asList(UserVS.Type.USER, UserVS.Type.GROUP, UserVS.Type.BANKVS));
        long numTotalUsers = (long)query.getSingleResult();

        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getServerDir().getAbsolutePath(), null);
        List groupVSBalanceList = new ArrayList<>();
        List userVSBalanceList = new ArrayList<>();
        List bankVSBalanceList = new ArrayList<>();
        int offset = 0;
        int pageSize = 100;
        List<UserVS> userVSList;
        query = dao.getEM().createNamedQuery("findUserActiveOrCancelledAfterAndInList").setParameter("dateCancelled",
                timePeriod.getDateFrom()).setParameter("inList", Arrays.asList(UserVS.Type.USER, UserVS.Type.GROUP, UserVS.Type.BANKVS))
                .setFirstResult(0).setMaxResults(pageSize);
        while ((userVSList = query.getResultList()).size() > 0) {
            for (UserVS userVS : userVSList) {
                try {
                    if(userVS instanceof BankVS)
                    bankVSBalanceList.add(bankVSBean.getDataWithBalancesMap((BankVS) userVS, timePeriod));
                    else if(userVS instanceof GroupVS) groupVSBalanceList.add(groupVSBean.getDataWithBalancesMap(userVS, timePeriod));
                    else userVSBalanceList.add(userVSBean.getDataWithBalancesMap(userVS, timePeriod));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            if((offset % 2000) == 0) {
                //accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
                //new File(accessRequestBaseDir).mkdirs()
            }
            dao.getEM().flush();
            dao.getEM().clear();
            offset += pageSize;
            query.setFirstResult(offset);
            String elapsedTime = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc);
            log.info("processed " + offset + " of " + numTotalUsers + " - elapsedTime: " + elapsedTime);
        }
        Map systemBalance = systemBean.genBalanceForSystem(timePeriod);
        Map userBalances = new HashMap<>();
        userBalances.put("systemBalance", systemBalance);
        userBalances.put("groupVSBalanceList", groupVSBalanceList);
        userBalances.put("userVSBalanceList", userVSBalanceList);
        userBalances.put("bankVSBalanceList", bankVSBalanceList);

        Map resultMap = new HashMap<>();
        resultMap.put("timePeriod",timePeriod.getMap(null));
        resultMap.put("userBalances", userBalances);

        String resultBalanceStr = new ObjectMapper().writeValueAsString(resultMap);
        Files.write(Paths.get(reportFiles.getJsonFile().getAbsolutePath()), resultBalanceStr.getBytes());
        String subjectSufix = "[" + DateUtils.getDateStr(timePeriod.getDateFrom()) + " - " + DateUtils.getDateStr(timePeriod.getDateTo()) + "]";
        String subject =  messages.get("periodBalancesReportMsgSubject", subjectSufix);
        SMIMEMessage receipt = signatureBean.getSMIMETimeStamped (signatureBean.getSystemUser().getName(), null,
                resultBalanceStr, subject);
        receipt.writeTo(new FileOutputStream(reportFiles.getReceiptFile()));
        String elapsedTime = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc);
        log.info("numTotalUsers:" + numTotalUsers + " - finished in: " + elapsedTime);
        return resultMap;
    }

    public Map genBalance(UserVS uservs, DateUtils.TimePeriod timePeriod) throws Exception {
        if(UserVS.Type.SYSTEM == uservs.getType()) return systemBean.genBalanceForSystem(timePeriod);
        else if(uservs instanceof BankVS) return bankVSBean.getDataWithBalancesMap((BankVS) uservs, timePeriod);
        else if(uservs instanceof GroupVS) return groupVSBean.getDataWithBalancesMap(uservs, timePeriod);
        else return userVSBean.getDataWithBalancesMap(uservs, timePeriod);
    }

}
