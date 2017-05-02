package org.currency.web.ejb;

import eu.europa.esig.dss.InMemoryDocument;
import org.currency.web.util.AuditLogger;
import org.currency.web.util.ReportFiles;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.PeriodResultDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.Transaction;
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
    @Inject private SignatureServiceEJB signatureService;

    //Backup user transactions for timePeriod
    public BalancesDto backupUserTransactionList(User user, Interval timePeriod) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MMM_dd");
        String lapsePath = DateUtils.getDirPath(timePeriod.getDateFrom().toLocalDateTime()) +
                "/" + formatter.format(timePeriod.getDateFrom()) + "__" +  formatter.format(timePeriod.getDateTo());
        File backupDir = new File(config.getApplicationDataPath() + "/backups/user-transaction-backup" + lapsePath);
        backupDir.mkdirs();
        log.info( "user: " + user.getId() + " - timePeriod: " + timePeriod.toString() + " - backupDir: " + backupDir.getAbsolutePath());
        //Expenses
        List<Transaction> transactionList = em.createNamedQuery(Transaction.FIND_TRANS_BY_FROM_USER_AND_STATE)
                .setParameter("fromUser", user)
                .setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("state", Transaction.State.OK).getResultList();

        List<TransactionDto> transactionFromList = new ArrayList<>();
        Map<CurrencyCode, BigDecimal> balancesFromMap = new HashMap<>();
        for(Transaction transaction : transactionList) {
            if((balancesFromMap.get(transaction.getCurrencyCode())) != null) {
                BigDecimal accumulated = balancesFromMap.get(transaction.getCurrencyCode()).add(transaction.getAmount());
                balancesFromMap.put(transaction.getCurrencyCode(), accumulated);
            } else {
                balancesFromMap.put(transaction.getCurrencyCode(), transaction.getAmount());
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
        Map<CurrencyCode, BigDecimal> balancesToMap = new HashMap<>();
        for(Transaction transaction : transactionList) {
            if((balancesToMap.get(transaction.getCurrencyCode())) != null) {
                BigDecimal accumulated = balancesToMap.get(transaction.getCurrencyCode()).add(transaction.getAmount());
                balancesToMap.put(transaction.getCurrencyCode(), accumulated);
            } else {
                balancesFromMap.put(transaction.getCurrencyCode(), transaction.getAmount());
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

    public void initWeekPeriod(LocalDateTime timeBegin) throws Exception {
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
        Map<CurrencyCode, BigDecimal> currencyMap = balancesDto.getBalancesCash();
        byte[] contentToSign = XML.getMapper().writeValueAsBytes(balancesDto);
        byte[] contentSigned = signatureService.signXAdES(contentToSign);
        SignatureParams signatureParams = new SignatureParams(null, User.Type.CURRENCY_SERVER,
                SignedDocumentType.CURRENCY_PERIOD_INIT).setWithTimeStampValidation(true);
        SignedDocument signedDocument = signatureService.validateXAdESAndSave(
                new InMemoryDocument(contentSigned), signatureParams);
        File outputFile = reportFiles.getReportFile();
        log.info("user UUID: " + user.getUUID() + " - outputFile: " + outputFile.getAbsolutePath());
        FileUtils.copyBytesToFile(signedDocument.getBody().getBytes(), outputFile);
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