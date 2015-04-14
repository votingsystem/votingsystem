package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.cdi.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class AuditingBean {

    private static Logger log = Logger.getLogger(AuditingBean.class.getSimpleName());

    @PersistenceContext private EntityManager em;
    @Inject ConfigVS config;
    @Inject TransactionVSBean transactionVSBean;

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
        Query query = em.createNamedQuery("findTransByFromUserAndTransactionParentNullAndDateCreatedBetween")
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
        //incomes
        query = em.createNamedQuery("findTransByToUserAndDateCreatedBetween").setParameter("toUserVS", userVS)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo",timePeriod.getDateTo());
        transactionList = query.getResultList();
        List<TransactionVSDto> transactionToList = new ArrayList<>();
        Map<String, Map<String, Map>> balancesToMap = new HashMap<>();
        for(TransactionVS transaction : transactionList) {
            Map<String, Map> currencyMap = null;
            /*if((currencyMap = balancesToMap.get(transaction.getCurrencyCode())) != null) {
                Map<String, Map> tagMap = balancesToMap.get(transaction.getCurrencyCode());
                BigDecimal tagAccumulated = null;
                if((tagAccumulated = tagMap.get(transaction.getTag().getName())) != null) {
                    tagMap.put(transaction.getTag().getName(), tagAccumulated.add(transaction.getAmount()));
                } else tagMap.put(transaction.getTag().getName(), transaction.getAmount());
            } else {
                Map<String, BigDecimal> tagMap = new HashMap<>();
                tagMap.put(transaction.getTag().getName(), transaction.getAmount());
                balancesToMap.put(transaction.getCurrencyCode(), currencyMap);
            }*/
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


}
