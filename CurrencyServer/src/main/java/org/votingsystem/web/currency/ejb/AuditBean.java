package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.*;
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
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.currency.util.ReportFiles;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class AuditBean {

    private static Logger log = Logger.getLogger(AuditBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject TransactionVSBean transactionVSBean;
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
        MessagesVS messages = new MessagesVS(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        long beginCalc = System.currentTimeMillis();
        //we know this is launch every Monday after 00:00 so we just make sure to select a day from last week
        TimePeriod timePeriod = DateUtils.getWeekPeriod(DateUtils.getDayFromPreviousWeek(requestDate));
        String transactionSubject =  messages.get("initWeekMsg", DateUtils.getDayWeekDateStr(timePeriod.getDateTo()));
        List<UserVS.State> notActiveList = Arrays.asList(UserVS.State.SUSPENDED, UserVS.State.CANCELED);
        List<UserVS.Type> userVSTypeList = Arrays.asList(UserVS.Type.GROUP, UserVS.Type.USER);
        Query query = dao.getEM().createQuery("SELECT COUNT(u) FROM UserVS u WHERE u.type in :typeList and(" +
                "u.state = 'ACTIVE' or (u.state in :notActiveList and(u.dateCancelled >=:dateCancelled)))")
                .setParameter("typeList", userVSTypeList)
                .setParameter("notActiveList", notActiveList)
                .setParameter("dateCancelled", timePeriod.getDateFrom());
        long numTotalUsers = (long)query.getSingleResult();
        log.info(transactionSubject + " - numTotalUsers:" + numTotalUsers + " - " + timePeriod.toString());
        int offset = 0;
        int pageSize = 100;
        List<UserVS> userVSList;
        query = dao.getEM().createNamedQuery("findUserActiveOrCancelledAfterAndInList").setFirstResult(0).setMaxResults(pageSize)
                .setParameter("typeList", userVSTypeList)
                .setParameter("notActiveList", notActiveList)
                .setParameter("dateCancelled", timePeriod.getDateFrom());;
        while ((userVSList = query.getResultList()).size() > 0) {
            for (UserVS userVS : userVSList) {
                try {
                    if(!initUserVSWeekPeriod(userVS, timePeriod, transactionSubject)) continue;
                } catch(Exception ex) {
                    LoggerVS.weekLog(Level.SEVERE, "userVS: " + userVS.getId()  + " - " +  ex.getMessage(), ex);
                }
            }
            if((offset % 2000) == 0) {
                //weekReportsBatchDir= weekReportsBaseDir + "/batch_" + batch++;
                //new File(weekReportsBatchDir).mkdirs()
            }
            dao.getEM().flush();
            dao.getEM().clear();
            log.info("processed " + (offset + userVSList.size()) + " of " + numTotalUsers + " - elapsedTime: " +
                    DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc));
            offset += pageSize;
            query.setFirstResult(offset);
        }
    }

    public boolean initUserVSWeekPeriod(UserVS userVS, TimePeriod timePeriod, String transactionSubject)
            throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
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
        log.info(format("initUserVSWeekPeriod - UserVS ''{0}'' - dir ''{1}''", userVS.getId(),
                reportFiles.getBaseDir().getAbsolutePath()) );
        Map<String, Map<String, BigDecimal>> currencyMap = balancesDto.getBalancesCash();
        Query query = null;
        for(String currencyCode: currencyMap.keySet()) {
            for(Map.Entry<String, BigDecimal> tagVSEntry:  currencyMap.get(currencyCode).entrySet()) {
                TagVS currentTagVS = config.getTag(tagVSEntry.getKey());
                query = dao.getEM().createQuery("SELECT count (t) FROM TransactionVS t WHERE t.toUserVS =:toUserVS and " +
                        "t.state =:state and t.type =:type and t.tag =:tag and t.dateCreated >=:dateFrom")
                        .setParameter("toUserVS", userVS).setParameter("state", TransactionVS.State.OK)
                        .setParameter("type", TransactionVS.Type.CURRENCY_PERIOD_INIT)
                        .setParameter("tag", currentTagVS).setParameter("dateFrom", timePeriod.getDateFrom());

                long numInitPeriodTransaction = (long) query.getSingleResult();
                if(numInitPeriodTransaction > 0) throw new ExceptionVS("REPEATED CURRENCY_PERIOD_INIT TransactionVS for " +
                        "UserVS:" + userVS.getId() + " - tag: " + tagVSEntry.getKey() + " - timePeriod:" + timePeriod);
                BigDecimal timeLimitedNotExpended = balancesDto.getTimeLimitedNotExpended(currencyCode, currentTagVS.getName());
                BigDecimal amountResult = tagVSEntry.getValue().subtract(timeLimitedNotExpended);
                String signedMessageSubject =  messages.get("tagInitPeriodMsg", tagVSEntry.getKey());
                String signedContent = JSON.getMapper().writeValueAsString(new InitPeriodTransactionVSDto(amountResult,
                        timeLimitedNotExpended, currencyCode, tagVSEntry.getKey(), userVS));
                SMIMEMessage smimeMessage = signatureBean.getSMIMETimeStamped (signatureBean.getSystemUser().getName(),
                        userVS.getNif(), signedContent, transactionSubject + " - " + signedMessageSubject);
                MessageSMIME messageSMIME = dao.persist(new MessageSMIME(smimeMessage, signatureBean.getSystemUser(),
                        TypeVS.CURRENCY_PERIOD_INIT));
                dao.persist(new TransactionVS(userVS, userVS, amountResult, currencyCode, signedMessageSubject, messageSMIME,
                        TransactionVS.Type.CURRENCY_PERIOD_INIT, TransactionVS.State.OK, currentTagVS));
                if(timeLimitedNotExpended.compareTo(BigDecimal.ZERO) > 0) {
                    query = dao.getEM().createNamedQuery("findAccountByUserIBANAndStateAndCurrencyAndTag")
                            .setParameter("userIBAN", userVS.getIBAN()).setParameter("state", CurrencyAccount.State.ACTIVE)
                            .setParameter("currencyCode", currencyCode).setParameter("tag", currentTagVS);
                    CurrencyAccount account = dao.getSingleResult (CurrencyAccount.class, query);
                    Map accountFromMovements = new HashMap<>();
                    accountFromMovements.put(account, timeLimitedNotExpended);
                    TransactionVS transactionVS = dao.persist(new TransactionVS(userVS, signatureBean.getSystemUser(),
                            timeLimitedNotExpended, currencyCode,
                            signedMessageSubject, messageSMIME,TransactionVS.Type.CURRENCY_PERIOD_INIT_TIME_LIMITED,
                            TransactionVS.State.OK,currentTagVS ));
                    transactionVS.setAccountFromMovements(accountFromMovements);
                }
                File outputFile = reportFiles.getTagReceiptFile(tagVSEntry.getKey());
                log.info(currencyCode + " - " + currentTagVS.getName() + " - result: " + outputFile.getAbsolutePath());
                smimeMessage.writeTo(new FileOutputStream(outputFile));
            }
        }
        return true;
    }

    //@Transactional
    public PeriodResultDto signPeriodResult(TimePeriod timePeriod) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        long beginCalc = System.currentTimeMillis();
        Query query = dao.getEM().createNamedQuery("countUserActiveByDateAndInList").setParameter("date", timePeriod.getDateFrom())
                .setParameter("inList", Arrays.asList(UserVS.Type.USER, UserVS.Type.GROUP, UserVS.Type.BANKVS));
        long numTotalUsers = (long)query.getSingleResult();

        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getServerDir().getAbsolutePath(), null);
        PeriodResultDto periodResultDto = PeriodResultDto.init(timePeriod);
        int offset = 0;
        int pageSize = 100;
        List<UserVS> userVSList;
        query = dao.getEM().createNamedQuery("findUserActiveOrCancelledAfterAndInList").setParameter("dateCancelled",
                timePeriod.getDateFrom()).setParameter("typeList", Arrays.asList(UserVS.Type.USER, UserVS.Type.GROUP, UserVS.Type.BANKVS))
                .setFirstResult(0).setMaxResults(pageSize);
        while ((userVSList = query.getResultList()).size() > 0) {
            for (UserVS userVS : userVSList) {
                try {
                    if(userVS instanceof BankVS)
                        periodResultDto.addBankVSBalance(balancesBean.getBalancesDto(userVS, timePeriod));
                    else if(userVS instanceof GroupVS)
                        periodResultDto.addGroupVSBalance(balancesBean.getBalancesDto(userVS, timePeriod));
                    else periodResultDto.addUserVSBalance(balancesBean.getBalancesDto(userVS, timePeriod));
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            if((offset % 2000) == 0) {  //TODO sign batch
            }
            dao.getEM().flush();
            dao.getEM().clear();
            offset += pageSize;
            query.setFirstResult(offset);
            String elapsedTime = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc);
            log.info("processed " + offset + " of " + numTotalUsers + " - elapsedTime: " + elapsedTime);
        }
        periodResultDto.setSystemBalance(balancesBean.getSystemBalancesDto(timePeriod));

        String resultBalanceStr = JSON.getMapper().writeValueAsString(periodResultDto);
        Files.write(Paths.get(reportFiles.getJsonFile().getAbsolutePath()), resultBalanceStr.getBytes());
        String subjectSufix = "[" + DateUtils.getDateStr(timePeriod.getDateFrom()) + " - " + DateUtils.getDateStr(timePeriod.getDateTo()) + "]";
        String subject =  messages.get("periodBalancesReportMsgSubject", subjectSufix);
        SMIMEMessage receipt = signatureBean.getSMIMETimeStamped (signatureBean.getSystemUser().getName(), null,
                resultBalanceStr, subject);
        receipt.writeTo(new FileOutputStream(reportFiles.getReceiptFile()));
        String elapsedTime = DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc);
        log.info("numTotalUsers:" + numTotalUsers + " - finished in: " + elapsedTime);
        return periodResultDto;
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
