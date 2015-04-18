package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.dto.currency.InitPeriodTransactionVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;
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
import java.util.Currency;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class AuditBean {

    private static Logger log = Logger.getLogger(AuditBean.class.getSimpleName());

    private static final DateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject TransactionVSBean transactionVSBean;
    @Inject MessagesBean messages;
    @Inject SignatureBean signatureBean;
    @Inject BalancesBean balancesBean;

    //Check that the sum of all issued Currency match with valid request
    public void checkCurrencyRequest(TimePeriod timePeriod) { }

    //Backup user transactions for timePeriod
    public BalancesDto backupUserVSTransactionVSList (UserVS userVS, TimePeriod timePeriod) throws IOException {
        String lapsePath = DateUtils.getDirPath(timePeriod.getDateFrom());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MMM_dd");
        lapsePath = "/" + formatter.format(timePeriod.getDateFrom()) + "__" +  formatter.format(timePeriod.getDateTo());
        File backupDir = new File(config.getServerDir().getAbsolutePath() + "/backups/userTransactionHistory" + lapsePath);
        backupDir.mkdirs();
        log.info( "user: " + userVS.getId() + " - timePeriod: " + timePeriod.toString() + " - backupDir: " + backupDir.getAbsolutePath());
        //Expenses
        Query query = dao.getEM().createNamedQuery("findTransByFromUserAndTransactionParentNullAndDateCreatedBetween")
                .setParameter("fromUserVS", userVS).setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo());
        List<TransactionVS> transactionList = query.getResultList();
        List<TransactionVSDto> transactionFromList = new ArrayList<>();
        Map<String, Map<String, BigDecimal>> balancesFromMap = new HashMap<>();
        for(TransactionVS transaction : transactionList) {
            Map<String, BigDecimal> currencyMap = null;
            if((currencyMap = balancesFromMap.get(transaction.getCurrencyCode())) != null) {
                Map<String, BigDecimal> tagMap = balancesFromMap.get(transaction.getCurrencyCode());
                BigDecimal tagAccumulated = null;
                if((tagAccumulated = tagMap.get(transaction.getTag().getName())) != null) {
                    tagMap.put(transaction.getTag().getName(), tagAccumulated.add(transaction.getAmount()));
                } else tagMap.put(transaction.getTag().getName(), transaction.getAmount());
            } else {
                Map<String, BigDecimal> tagMap = new HashMap<>();
                tagMap.put(transaction.getTag().getName(), transaction.getAmount());
                balancesFromMap.put(transaction.getCurrencyCode(), tagMap);
            }
            TransactionVSDto dto = transactionVSBean.getTransactionDto(transaction);
            MessageSMIME messageSMIME = transaction.getMessageSMIME();
            dto.setMessageSMIME(Base64.getUrlEncoder().encodeToString(messageSMIME.getContent()));
            transactionFromList.add(dto);
        }
        BalancesDto balancesDto = BalancesDto.FROM(transactionFromList, balancesFromMap);
        query = dao.getEM().createNamedQuery("findTransByToUserAndDateCreatedBetween").setParameter("toUserVS", userVS)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo",timePeriod.getDateTo());
        transactionList = query.getResultList();
        List<TransactionVSDto> transactionToList = new ArrayList<>();
        Map<String, Map<String, IncomesDto>> balancesToMap = new HashMap<>();
        for(TransactionVS transaction : transactionList) {
            Map<String, IncomesDto> currencyMap = null;
            if((currencyMap = balancesToMap.get(transaction.getCurrencyCode())) != null) {
                IncomesDto tagAccumulated = null;
                if((tagAccumulated = currencyMap.get(transaction.getTag().getName())) != null) {
                    currencyMap.put(transaction.getTag().getName(), tagAccumulated.add(transaction));
                } else currencyMap.put(transaction.getTag().getName(), new IncomesDto(transaction));
            } else {
                currencyMap = new HashMap<>();
                currencyMap.put(transaction.getTag().getName(), new IncomesDto(transaction));
                balancesToMap.put(transaction.getCurrencyCode(), currencyMap);
            }
            TransactionVSDto transactionDto = transactionVSBean.getTransactionDto(transaction);
            MessageSMIME messageSMIME = transaction.getMessageSMIME();
            transactionDto.setMessageSMIME(Base64.getUrlEncoder().encodeToString(messageSMIME.getContent()));
            transactionToList.add(transactionDto);
        }
        balancesDto.setTo(transactionToList, balancesToMap);
        File userHistoryFile = new File(backupDir.getAbsolutePath() + "/" + userVS.getNif() + ".json");
        JSON.getMapper().writeValue(userHistoryFile, balancesDto);
        log.info("user: " + userVS.getId() + " - userHistoryFile:" + userHistoryFile.getAbsolutePath());
        return balancesDto;
    }

    public void initWeekPeriod(Calendar requestDate) throws IOException {
        long beginCalc = System.currentTimeMillis();
        //we know this is launch every Monday after 00:00 so we just make sure to select a day from last week
        TimePeriod timePeriod = DateUtils.getWeekPeriod(DateUtils.getDayFromPreviousWeek(requestDate));
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

    public boolean initUserVSWeekPeriod(UserVS userVS, TimePeriod timePeriod, String transactionMsgSubject)
            throws Exception {
        BalancesDto balancesDto = null;
        String userSubPath;
        if(userVS instanceof GroupVS) {
            balancesDto = balancesBean.getBalancesDto(userVS, timePeriod);
            userSubPath = "GroupVS_"+ userVS.getId();
        } else if (userVS instanceof UserVS) {
            balancesDto = balancesBean.getBalancesDto(userVS, timePeriod);
            //userSubPath = StringUtils.getUserDirPath(userVS.getNif());
            userSubPath = userVS.getNif();
        } else {
            log.info("userVS id: " + userVS.getId() + " is " + userVS.getClass().getSimpleName() + " - no init period");
            return false;
        }
        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getServerDir().getAbsolutePath(), userSubPath);
        log.info("$methodName - UserVS '$userVS.id' - dir: '$reportFiles.baseDir.absolutePath'");
        Map<String, Map<String, BigDecimal>> currencyMap = balancesDto.getBalancesCash();
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
                        balancesDto.getBalancesFrom(), balancesDto.getBalancesTo(), currentTagVS.getName(), currency);
                if(TagVS.WILDTAG.equals(currentTagVS.getName()) &&
                        timeLimitedNotExpended.compareTo(BigDecimal.ZERO) < 0) timeLimitedNotExpended = BigDecimal.ZERO;
                BigDecimal amountResult = tagVSEntry.getValue().subtract(timeLimitedNotExpended);
                String signedMessageSubject =  messages.get("tagInitPeriodMsg", tagVSEntry.getKey());
                String signedContent = JSON.getMapper().writeValueAsString(new InitPeriodTransactionVSDto(amountResult,
                        timeLimitedNotExpended, tagVSEntry.getKey(), userVS));
                SMIMEMessage smimeMessage = signatureBean.getSMIMETimeStamped (signatureBean.getSystemUser().getName(),
                        userVS.getNif(), signedContent, transactionMsgSubject + " - " + signedMessageSubject);
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
    public Map signPeriodResult(TimePeriod timePeriod) throws Exception {
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
                        bankVSBalanceList.add(balancesBean.getBalancesDto(userVS, timePeriod));
                    else if(userVS instanceof GroupVS) groupVSBalanceList.add(balancesBean.getBalancesDto(userVS, timePeriod));
                    else userVSBalanceList.add(balancesBean.getBalancesDto(userVS, timePeriod));
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
        BalancesDto systemBalance = balancesBean.getSystemBalancesDto(timePeriod);
        Map userBalances = new HashMap<>();
        userBalances.put("systemBalance", systemBalance);
        userBalances.put("groupVSBalanceList", groupVSBalanceList);
        userBalances.put("userVSBalanceList", userVSBalanceList);
        userBalances.put("bankVSBalanceList", bankVSBalanceList);

        Map resultMap = new HashMap<>();
        resultMap.put("timePeriod",timePeriod);
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

    public void checkCurrencyCanceled() {
        Date date = new Date();
        log.info("checkCurrencyCanceled - date: " + date);
        Query query = dao.getEM().createQuery("select c from Currency c where c.validTo <=:validTo and c.state =:state")
                .setParameter("validTo", date).setParameter("state", org.votingsystem.model.currency.Currency.State.OK);
        List<org.votingsystem.model.currency.Currency> currencyList = query.getResultList();
        for(org.votingsystem.model.currency.Currency currency :currencyList) {
            dao.merge(currency.setState(org.votingsystem.model.currency.Currency.State.LAPSED));
            log.log(Level.FINE, "LAPSED currency id: " + currency.getId() + " - value: " + currency.getAmount());
        }
    }
}
