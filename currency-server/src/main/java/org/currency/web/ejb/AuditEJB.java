package org.currency.web.ejb;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.dto.currency.*;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.currency.*;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.*;
import org.votingsystem.crypto.SignedDocumentType;
import org.currency.web.util.AuditLogger;
import org.currency.web.util.ReportFiles;
import org.votingsystem.dto.currency.*;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;
import org.votingsystem.xml.XML;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class AuditEJB {

    private static Logger log = Logger.getLogger(AuditEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private TransactionEJB transactionBean;
    @Inject private SignerInfoService signerInfoService;
    @Inject private BalancesEJB balancesBean;
    @Inject private SignatureService signatureService;

    //Backup user transactions for timePeriod
    public BalancesDto backupUserTransactionList(User user, Interval timePeriod) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MMM_dd");
        String lapsePath = DateUtils.getDirPath(timePeriod.getDateFrom().toLocalDateTime()) +
                "/" + formatter.format(timePeriod.getDateFrom()) + "__" +  formatter.format(timePeriod.getDateTo());
        File backupDir = new File(config.getApplicationDataPath() + "/backups/userTransactionHistory" + lapsePath);
        backupDir.mkdirs();
        log.info( "user: " + user.getId() + " - timePeriod: " + timePeriod.toString() + " - backupDir: " + backupDir.getAbsolutePath());
        //Expenses
        List<Transaction> transactionList = em.createNamedQuery(Transaction.FIND_TRANS_BY_FROM_USER_AND_TRANS_PARENT_NULL_AND_DATE_CREATED_BETWEEN)
                .setParameter("fromUser", user).setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo()).getResultList();

        List<TransactionDto> transactionFromList = new ArrayList<>();
        Map<CurrencyCode, Map<String, BigDecimal>> balancesFromMap = new HashMap<>();
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
            SignedDocument signedDocument = transaction.getSignedDocument();
            dto.setSignedDocumentBase64(Base64.getEncoder().encodeToString(signedDocument.getBody().getBytes()));
            transactionFromList.add(dto);
        }
        BalancesDto balancesDto = BalancesDto.FROM(transactionFromList, balancesFromMap);

        transactionList = em.createNamedQuery(Transaction.FIND_TRANS_BY_TO_USER_AND_DATE_CREATED_BETWEEN)
                .setParameter("toUser", user)
                .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                .setParameter("dateTo",timePeriod.getDateTo().toLocalDateTime()).getResultList();

        List<TransactionDto> transactionToList = new ArrayList<>();
        Map<CurrencyCode, Map<String, IncomesDto>> balancesToMap = new HashMap<>();
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
            SignedDocument signedDocument = transaction.getSignedDocument();
            transactionDto.setSignedDocumentBase64(Base64.getEncoder().encodeToString(signedDocument.getBody().getBytes()));
            transactionToList.add(transactionDto);
        }
        balancesDto.setTo(transactionToList, balancesToMap);
        File userHistoryFile = new File(backupDir.getAbsolutePath() + "/" + user.getNumIdAndType() + ".xml");
        XML.getMapper().writeValue(userHistoryFile, balancesDto);
        log.info("user: " + user.getId() + " - userHistoryFile:" + userHistoryFile.getAbsolutePath());
        return balancesDto;
    }

    public void initWeekPeriod(LocalDateTime requestDate) throws Exception {
        LocalDateTime timeBegin = LocalDateTime.now();
        //we know this is launch every Monday after 00:00 so we just make sure to select a day from last week
        Interval timePeriod = DateUtils.getWeekPeriod(timeBegin.minus(7, ChronoUnit.DAYS));
        String transactionSubject =  Messages.currentInstance().get("initWeekMsg", DateUtils.getDateStr(timePeriod.getDateTo()));
        List<User.Type> userTypeList = Arrays.asList(User.Type.USER);
        Query query = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.type in :typeList and(" +
                "u.state = 'ACTIVE' or (u.state not 'ACTIVE' and(u.dateCancelled >=:dateCancelled)))")
                .setParameter("typeList", userTypeList)
                .setParameter("dateCancelled", timePeriod.getDateFrom());
        long numTotalUsers = (long)query.getSingleResult();
        log.info(transactionSubject + " - numTotalUsers:" + numTotalUsers + " - " + timePeriod.toString());
        int offset = 0;
        int pageSize = 100;
        List<User> userList;
        query = em.createNamedQuery(User.FIND_USER_IN_LIST_ACTIVE_OR_CANCELED_AFTER)
                .setParameter("typeList", userTypeList)
                .setParameter("dateCancelled", timePeriod.getDateFrom())
                .setFirstResult(offset).setMaxResults(pageSize);
        while ((userList = query.getResultList()).size() > 0) {
            for (User user : userList) {
                try {
                    if(!initUserWeekPeriod(user, timePeriod, transactionSubject)) continue;
                } catch(Exception ex) {
                    AuditLogger.weekLog(Level.SEVERE, "user: " + user.getId()  + " - " +  ex.getMessage(), ex);
                }
            }
            if((offset % 2000) == 0) {
                //weekReportsBatchDir= weekReportsBaseDir + "/batch_" + batch++;
                //new File(weekReportsBatchDir).mkdirs()
            }
            em.flush();
            em.clear();
            offset += pageSize;
            query.setFirstResult(offset);
            LocalDateTime iterationDate = LocalDateTime.now();
            log.info("processed " + (offset + userList.size()) + " of " + numTotalUsers + " - elapsedTime: " +
                    timeBegin.until(iterationDate, ChronoUnit.SECONDS) + " seconds");
        }
        signPeriodResult(timePeriod);
    }

    public boolean initUserWeekPeriod(User user, Interval timePeriod, String transactionSubject) throws Exception {
        BalancesDto balancesDto = balancesBean.getBalancesDto(user, timePeriod);
        //userSubPath = StringUtils.getUserDirPath(user.getNif());
        String userSubPath = user.getNumIdAndType();
        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getApplicationDataPath(), userSubPath);
        log.info(format("initUserWeekPeriod - User ''{0}'' - dir ''{1}''", user.getId(),
                reportFiles.getBaseDir().getAbsolutePath()) );
        Map<CurrencyCode, Map<String, BigDecimal>> currencyMap = balancesDto.getBalancesCash();
        Query query = null;
        for(CurrencyCode currencyCode: currencyMap.keySet()) {
            for(Map.Entry<String, BigDecimal> tagEntry:  currencyMap.get(currencyCode).entrySet()) {
                Tag currentTag = config.getTag(tagEntry.getKey());
                query = em.createQuery("SELECT count (t) FROM Transaction t WHERE t.toUser =:toUser and " +
                        "t.state =:state and t.type =:type and t.tag =:tag and t.dateCreated >=:dateTo")
                        .setParameter("toUser", user)
                        .setParameter("state", Transaction.State.OK)
                        .setParameter("type", Transaction.Type.CURRENCY_PERIOD_INIT)
                        .setParameter("tag", currentTag)
                        .setParameter("dateTo", timePeriod.getDateTo());
                long numTransactions = (long) query.getSingleResult();
                if(numTransactions > 0) throw new ValidationException("REPEATED CURRENCY_PERIOD_INIT Transaction for " +
                        "User:" + user.getId() + " - tag: " + tagEntry.getKey() + " - timePeriod:" + timePeriod);
                BigDecimal timeLimitedNotExpended = balancesDto.getTimeLimitedNotExpended(currencyCode, currentTag.getName());
                BigDecimal amountResult = tagEntry.getValue().subtract(timeLimitedNotExpended);
                String messageSubject =  Messages.currentInstance().get("tagInitPeriodMsg", tagEntry.getKey());
                byte[] contentToSign = XML.getMapper().writeValueAsBytes(new InitPeriodTransactionDto(amountResult,
                        timeLimitedNotExpended, currencyCode, tagEntry.getKey(), user));
                byte[] contentSigned = signatureService.signXAdES(contentToSign);
                SignatureParams signatureParams = new SignatureParams(null, User.Type.CURRENCY_SERVER,
                        SignedDocumentType.CURRENCY_PERIOD_INIT).setWithTimeStampValidation(true);
                SignedDocument signedDocument = signatureService.validateXAdESAndSave(
                        new InMemoryDocument(contentSigned), signatureParams);

                if(timeLimitedNotExpended.compareTo(BigDecimal.ZERO) > 0) {
                    List<CurrencyAccount> currencyAccounts =
                            em.createNamedQuery(CurrencyAccount.FIND_BY_USER_IBAN_AND_TAG_AND_CURRENCY_CODE_AND_STATE)
                            .setParameter("userIBAN", user.getIBAN()).setParameter("state", CurrencyAccount.State.ACTIVE)
                            .setParameter("currencyCode", currencyCode).setParameter("tag", currentTag).getResultList();
                    CurrencyAccount account = currencyAccounts.iterator().next();
                    Map accountFromMovements = new HashMap<>();
                    accountFromMovements.put(account, timeLimitedNotExpended);
                    Transaction transaction = new Transaction(user, config.getSystemUser(),
                            timeLimitedNotExpended, currencyCode, messageSubject, signedDocument,
                            Transaction.Type.CURRENCY_PERIOD_INIT_TIME_LIMITED, Transaction.State.OK,currentTag);
                    em.persist(transaction);
                    transaction.setAccountFromMovements(accountFromMovements);
                }
                File outputFile = reportFiles.getTagReceiptFile(tagEntry.getKey());
                log.info(currencyCode + " - " + currentTag.getName() + " - result: " + outputFile.getAbsolutePath());
                FileUtils.copyBytesToFile(signedDocument.getBody().getBytes(), outputFile);
            }
        }
        return true;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public PeriodResultDto signPeriodResult(Interval timePeriod) throws Exception {
        LocalDateTime timeBegin = LocalDateTime.now();
        long numTotalUsers = (long)em.createNamedQuery(User.COUNT_USER_ACTIVE_BY_DATE_AND_IN_LIST)
                .setParameter("date", timePeriod.getDateFrom())
                .setParameter("inList", Arrays.asList(User.Type.USER, User.Type.BANK)).getSingleResult();
        ReportFiles reportFiles = ReportFiles.CURRENCY_PERIOD(timePeriod, config.getApplicationDataPath());
        PeriodResultDto periodResultDto = new PeriodResultDto(timePeriod);
        int offset = 0;
        int pageSize = 100;
        List<User> userList;
        Query query = em.createNamedQuery(User.FIND_USER_IN_LIST_ACTIVE_OR_CANCELED_AFTER).setParameter("dateCancelled",
                timePeriod.getDateFrom())
                .setParameter("typeList", Arrays.asList(User.Type.USER, User.Type.BANK))
                .setFirstResult(offset).setMaxResults(pageSize);
        while ((userList = query.getResultList()).size() > 0) {
            for (User user : userList) {
                try {
                    if(user instanceof Bank)
                            periodResultDto.addBankBalance(balancesBean.getBalancesDto(user, timePeriod));
                    else periodResultDto.addUserBalance(balancesBean.getBalancesDto(user, timePeriod));
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            if((offset % 2000) == 0) {  //TODO sign batch
            }
            em.flush();
            em.clear();
            offset += pageSize;
            query.setFirstResult(offset);
            log.info("processed " + offset + " of " + numTotalUsers + " - in: " +
                    timeBegin.until(LocalDateTime.now(), ChronoUnit.SECONDS) + " seconds");
        }
        periodResultDto.setSystemBalance(balancesBean.getSystemBalancesDto(timePeriod));
        byte[] resultBalanceBytes = XML.getMapper().writeValueAsBytes(periodResultDto);
        Files.write(Paths.get(reportFiles.getReportFile().getAbsolutePath()), resultBalanceBytes);
        byte[] receiptBytes = signatureService.signXAdES(resultBalanceBytes);
        FileUtils.copyBytesToFile(receiptBytes, reportFiles.getReceiptFile());
        log.info("numTotalUsers:" + numTotalUsers + " - finished in: " + timeBegin.until(LocalDateTime.now(), ChronoUnit.MINUTES) +
                " - reportFiles: " + reportFiles.getReportFile().getAbsolutePath());
        return periodResultDto;
    }

    public void checkLapsedCurrency() {
        ZonedDateTime date = ZonedDateTime.now();
        log.info("date: " + date);
        List<org.votingsystem.model.currency.Currency> currencyList = em.createQuery(
                "select c from Currency c where c.validTo <=:validTo and c.state =:state")
                .setParameter("validTo", date.toLocalDateTime())
                .setParameter("state", Currency.State.OK).getResultList();
        for(Currency currency :currencyList) {
            em.merge(currency.setState(Currency.State.LAPSED));
            log.log(Level.FINE, "LAPSED currency id: " + currency.getId() + " - value: " + currency.getAmount());
        }
    }

    //@Schedule(dayOfWeek = "Mon", hour="0", minute = "0", second = "0")
    public void initWeekPeriod() throws Exception {
        checkLapsedCurrency();
        initWeekPeriod(LocalDateTime.now());
    }

}