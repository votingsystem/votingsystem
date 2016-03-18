package org.votingsystem.web.currency.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.*;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Group;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.currency.cdi.ConfigVSImpl;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.currency.util.ReportFiles;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.File;
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

    private static Logger log = Logger.getLogger(AuditBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject TransactionBean transactionBean;
    @Inject CMSBean cmsBean;
    @Inject BalancesBean balancesBean;

    //Check that the sum of all issued Currency match with valid request
    public void checkCurrencyRequest(Interval timePeriod) { }

    //Backup user transactions for timePeriod
    public BalancesDto backupUserTransactionList(User user, Interval timePeriod) throws IOException {
        String lapsePath = DateUtils.getDirPath(timePeriod.getDateFrom());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MMM_dd");
        lapsePath = "/" + formatter.format(timePeriod.getDateFrom()) + "__" +  formatter.format(timePeriod.getDateTo());
        File backupDir = new File(config.getServerDir().getAbsolutePath() + "/backups/userTransactionHistory" + lapsePath);
        backupDir.mkdirs();
        log.info( "user: " + user.getId() + " - timePeriod: " + timePeriod.toString() + " - backupDir: " + backupDir.getAbsolutePath());
        //Expenses
        Query query = dao.getEM().createNamedQuery("findTransByFromUserAndTransactionParentNullAndDateCreatedBetween")
                .setParameter("fromUser", user).setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo());
        List<Transaction> transactionList = query.getResultList();
        List<TransactionDto> transactionFromList = new ArrayList<>();
        Map<String, Map<String, BigDecimal>> balancesFromMap = new HashMap<>();
        for(Transaction transaction : transactionList) {
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
            TransactionDto dto = transactionBean.getTransactionDto(transaction);
            CMSMessage cmsMessage = transaction.getCmsMessage();
            dto.setCmsMessagePEM(Base64.getUrlEncoder().encodeToString(cmsMessage.getContentPEM()));
            transactionFromList.add(dto);
        }
        BalancesDto balancesDto = BalancesDto.FROM(transactionFromList, balancesFromMap);
        query = dao.getEM().createNamedQuery("findTransByToUserAndDateCreatedBetween").setParameter("toUser", user)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo",timePeriod.getDateTo());
        transactionList = query.getResultList();
        List<TransactionDto> transactionToList = new ArrayList<>();
        Map<String, Map<String, IncomesDto>> balancesToMap = new HashMap<>();
        for(Transaction transaction : transactionList) {
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
            TransactionDto transactionDto = transactionBean.getTransactionDto(transaction);
            CMSMessage cmsMessage = transaction.getCmsMessage();
            transactionDto.setCmsMessagePEM(Base64.getUrlEncoder().encodeToString(cmsMessage.getContentPEM()));
            transactionToList.add(transactionDto);
        }
        balancesDto.setTo(transactionToList, balancesToMap);
        File userHistoryFile = new File(backupDir.getAbsolutePath() + "/" + user.getNif() + ".json");
        JSON.getMapper().writeValue(userHistoryFile, balancesDto);
        log.info("user: " + user.getId() + " - userHistoryFile:" + userHistoryFile.getAbsolutePath());
        return balancesDto;
    }

    public void initWeekPeriod(Calendar requestDate) throws IOException {
        MessagesVS messages = new MessagesVS(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        long beginCalc = System.currentTimeMillis();
        //we know this is launch every Monday after 00:00 so we just make sure to select a day from last week
        Interval timePeriod = DateUtils.getWeekPeriod(DateUtils.getDayFromPreviousWeek(requestDate));
        String transactionSubject =  messages.get("initWeekMsg", DateUtils.getDayWeekDateStr(timePeriod.getDateTo(), "HH:mm"));
        List<User.State> notActiveList = Arrays.asList(User.State.SUSPENDED, User.State.CANCELED);
        List<User.Type> userTypeList = Arrays.asList(User.Type.GROUP, User.Type.USER);
        Query query = dao.getEM().createQuery("SELECT COUNT(u) FROM User u WHERE u.type in :typeList and(" +
                "u.state = 'ACTIVE' or (u.state in :notActiveList and(u.dateCancelled >=:dateCancelled)))")
                .setParameter("typeList", userTypeList)
                .setParameter("notActiveList", notActiveList)
                .setParameter("dateCancelled", timePeriod.getDateFrom());
        long numTotalUsers = (long)query.getSingleResult();
        log.info(transactionSubject + " - numTotalUsers:" + numTotalUsers + " - " + timePeriod.toString());
        int offset = 0;
        int pageSize = 100;
        List<User> userList;
        query = dao.getEM().createNamedQuery("findUserActiveOrCancelledAfterAndInList").setFirstResult(0).setMaxResults(pageSize)
                .setParameter("typeList", userTypeList)
                .setParameter("notActiveList", notActiveList)
                .setParameter("dateCancelled", timePeriod.getDateFrom());;
        while ((userList = query.getResultList()).size() > 0) {
            for (User user : userList) {
                try {
                    if(!initUserWeekPeriod(user, timePeriod, transactionSubject)) continue;
                } catch(Exception ex) {
                    LoggerVS.weekLog(Level.SEVERE, "user: " + user.getId()  + " - " +  ex.getMessage(), ex);
                }
            }
            if((offset % 2000) == 0) {
                //weekReportsBatchDir= weekReportsBaseDir + "/batch_" + batch++;
                //new File(weekReportsBatchDir).mkdirs()
            }
            dao.getEM().flush();
            dao.getEM().clear();
            log.info("processed " + (offset + userList.size()) + " of " + numTotalUsers + " - elapsedTime: " +
                    DateUtils.getElapsedTimeHoursMinutesMillis(System.currentTimeMillis() - beginCalc));
            offset += pageSize;
            query.setFirstResult(offset);
        }
    }

    public boolean initUserWeekPeriod(User user, Interval timePeriod, String transactionSubject)
            throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        BalancesDto balancesDto = null;
        String userSubPath;
        if(user instanceof Group) {
            balancesDto = balancesBean.getBalancesDto(user, timePeriod);
            userSubPath = "Group_"+ user.getId();
        } else if (user instanceof User) {
            balancesDto = balancesBean.getBalancesDto(user, timePeriod);
            //userSubPath = StringUtils.getUserDirPath(user.getNif());
            userSubPath = user.getNif();
        } else {
            log.info("user id: " + user.getId() + " is " + user.getClass().getSimpleName() + " - no init period");
            return false;
        }
        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getServerDir().getAbsolutePath(), userSubPath);
        log.info(format("initUserWeekPeriod - User ''{0}'' - dir ''{1}''", user.getId(),
                reportFiles.getBaseDir().getAbsolutePath()) );
        Map<String, Map<String, BigDecimal>> currencyMap = balancesDto.getBalancesCash();
        Query query = null;
        for(String currencyCode: currencyMap.keySet()) {
            for(Map.Entry<String, BigDecimal> tagVSEntry:  currencyMap.get(currencyCode).entrySet()) {
                TagVS currentTagVS = config.getTag(tagVSEntry.getKey());
                query = dao.getEM().createQuery("SELECT count (t) FROM Transaction t WHERE t.toUser =:toUser and " +
                        "t.state =:state and t.type =:type and t.tag =:tag and t.dateCreated >=:dateTo")
                        .setParameter("toUser", user).setParameter("state", Transaction.State.OK)
                        .setParameter("type", Transaction.Type.CURRENCY_PERIOD_INIT)
                        .setParameter("tag", currentTagVS).setParameter("dateTo", timePeriod.getDateTo());

                long numInitPeriodTransaction = (long) query.getSingleResult();
                if(numInitPeriodTransaction > 0) throw new ExceptionVS("REPEATED CURRENCY_PERIOD_INIT Transaction for " +
                        "User:" + user.getId() + " - tag: " + tagVSEntry.getKey() + " - timePeriod:" + timePeriod);
                BigDecimal timeLimitedNotExpended = balancesDto.getTimeLimitedNotExpended(currencyCode, currentTagVS.getName());
                BigDecimal amountResult = tagVSEntry.getValue().subtract(timeLimitedNotExpended);
                String signedMessageSubject =  messages.get("tagInitPeriodMsg", tagVSEntry.getKey());
                byte[] contentToSign = JSON.getMapper().writeValueAsBytes(new InitPeriodTransactionDto(amountResult,
                        timeLimitedNotExpended, currencyCode, tagVSEntry.getKey(), user));
                CMSSignedMessage cmsSignedMessage = cmsBean.signDataWithTimeStamp(contentToSign);
                CMSMessage cmsMessage = dao.persist(new CMSMessage(cmsSignedMessage, cmsBean.getSystemUser(),
                        TypeVS.CURRENCY_PERIOD_INIT));
                dao.persist(new Transaction(user, user, amountResult, currencyCode, signedMessageSubject, cmsMessage,
                        Transaction.Type.CURRENCY_PERIOD_INIT, Transaction.State.OK, currentTagVS));
                if(timeLimitedNotExpended.compareTo(BigDecimal.ZERO) > 0) {
                    query = dao.getEM().createNamedQuery("findAccountByUserIBANAndStateAndCurrencyAndTag")
                            .setParameter("userIBAN", user.getIBAN()).setParameter("state", CurrencyAccount.State.ACTIVE)
                            .setParameter("currencyCode", currencyCode).setParameter("tag", currentTagVS);
                    CurrencyAccount account = dao.getSingleResult (CurrencyAccount.class, query);
                    Map accountFromMovements = new HashMap<>();
                    accountFromMovements.put(account, timeLimitedNotExpended);
                    Transaction transaction = dao.persist(new Transaction(user, cmsBean.getSystemUser(),
                            timeLimitedNotExpended, currencyCode,
                            signedMessageSubject, cmsMessage, Transaction.Type.CURRENCY_PERIOD_INIT_TIME_LIMITED,
                            Transaction.State.OK,currentTagVS ));
                    transaction.setAccountFromMovements(accountFromMovements);
                }
                File outputFile = reportFiles.getTagReceiptFile(tagVSEntry.getKey());
                log.info(currencyCode + " - " + currentTagVS.getName() + " - result: " + outputFile.getAbsolutePath());
                FileUtils.copyBytesToFile(cmsSignedMessage.toPEM(), outputFile);
            }
        }
        return true;
    }

    //@Transactional
    public PeriodResultDto signPeriodResult(Interval timePeriod) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        long beginCalc = System.currentTimeMillis();
        Query query = dao.getEM().createNamedQuery("countUserActiveByDateAndInList").setParameter("date", timePeriod.getDateFrom())
                .setParameter("inList", Arrays.asList(User.Type.USER, User.Type.GROUP, User.Type.BANK));
        long numTotalUsers = (long)query.getSingleResult();

        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getServerDir().getAbsolutePath(), null);
        PeriodResultDto periodResultDto = PeriodResultDto.init(timePeriod);
        int offset = 0;
        int pageSize = 100;
        List<User> userList;
        query = dao.getEM().createNamedQuery("findUserActiveOrCancelledAfterAndInList").setParameter("dateCancelled",
                timePeriod.getDateFrom()).setParameter("typeList", Arrays.asList(User.Type.USER, User.Type.GROUP, User.Type.BANK))
                .setFirstResult(0).setMaxResults(pageSize);
        while ((userList = query.getResultList()).size() > 0) {
            for (User user : userList) {
                try {
                    if(user instanceof Bank)
                        periodResultDto.addBankBalance(balancesBean.getBalancesDto(user, timePeriod));
                    else if(user instanceof Group)
                        periodResultDto.addGroupBalance(balancesBean.getBalancesDto(user, timePeriod));
                    else periodResultDto.addUserBalance(balancesBean.getBalancesDto(user, timePeriod));
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
        byte[] resultBalanceBytes = JSON.getMapper().writeValueAsBytes(periodResultDto);
        Files.write(Paths.get(reportFiles.getJsonFile().getAbsolutePath()), resultBalanceBytes);
        //String subjectSufix = "[" + DateUtils.getDateStr(timePeriod.getDateFrom()) + " - " +
        //        DateUtils.getDateStr(timePeriod.getDateTo()) + "]";
        CMSSignedMessage receipt = cmsBean.signDataWithTimeStamp(resultBalanceBytes);
        FileUtils.copyBytesToFile(receipt.toPEM(), reportFiles.getReceiptFile());
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


    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void cleanTempDir() throws IOException {
        log.info("cleanTempDir");
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            Date oneHourAgo = calendar.getTime();
            File tempDir = new File(config.getServerDir().getAbsolutePath() + ConfigVSImpl.getTempPath());
            tempDir.mkdirs();
            for (File file : tempDir.listFiles()) {
                if(new Date(file.lastModified()).compareTo(oneHourAgo) < 0) FileUtils.deleteRecursively(file);
            }
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); }
    }

    //@Schedule(dayOfWeek = "Mon", hour="0", minute = "0", second = "0")
    public void initWeekPeriod() throws IOException {
        checkCurrencyCanceled();
        initWeekPeriod(Calendar.getInstance());
    }

}
