package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.web.cdi.ConfigVS;

import javax.ejb.Stateless;
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
    public void checkCurrencyRequest(DateUtils.TimePeriod timePeriod) {

    }

    //Backup user transactions for timePeriod
    public Map backupUserVSTransactionVSList (UserVS userVS, DateUtils.TimePeriod timePeriod) throws IOException {
        String lapsePath = DateUtils.getDirPath(timePeriod.getDateFrom());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MMM_dd");
        lapsePath = "/" + formatter.format(timePeriod.getDateFrom()) + "__" +  formatter.format(timePeriod.getDateTo());
        File backupDir = new File(config.getProperty("vs.backupBasePath") + "/userTransactionHistory" + lapsePath);
        backupDir.mkdirs();
        log.info( "user: " + userVS.getId() + " - timePeriod: " + timePeriod.toString() + " - backupDir: " + backupDir.getAbsolutePath());
        //Expenses
        Query query = em.createNamedQuery("findTransByFromUserAndTransactionParentNullAndDateCreatedBetween")
                .setParameter("fromUserVS", userVS).setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo());
        List<TransactionVS> transactionList = query.getResultList();
        List<Map> transactionFromList = new ArrayList<>();
        Map<String, Map> balancesMap = new HashMap<>();
        for(TransactionVS transaction : transactionList) {
            Map<String, Map<String, BigDecimal>> currencyMap = null;
            if((currencyMap = balancesMap.get(transaction.getCurrencyCode())) != null) {
                Map<String, BigDecimal> tagMap = balancesMap.get(transaction.getCurrencyCode());
                BigDecimal tagAccumulated = null;
                if((tagAccumulated = tagMap.get(transaction.getTag().getName())) != null) {
                    tagMap.put(transaction.getTag().getName(), tagAccumulated.add(transaction.getAmount()));
                } else tagMap.put(transaction.getTag().getName(), transaction.getAmount());
            } else {
                Map<String, BigDecimal> tagMap = new HashMap<>();
                tagMap.put(transaction.getTag().getName(), transaction.getAmount());
                currencyMap.put(transaction.getCurrencyCode(), tagMap);
            }
            Map transactionVSMap = transactionVSBean.getTransactionMap(transaction);
            MessageSMIME messageSMIME = transaction.getMessageSMIME();
            //String messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${messageSMIME.id}"
            //transactionVSMap[messageSMIMEURL] = Base64.getUrlEncoder().encodeToString(messageSMIME.content)
            transactionVSMap.put("messageSMIME", Base64.getUrlEncoder().encodeToString(messageSMIME.getContent()));
            transactionFromList.add(transactionVSMap);
        }
        Map resultMap = new HashMap<>();
        resultMap.put("transactionFromList", transactionFromList);
        resultMap.put("balancesFrom", balancesMap);
        //incomes
        //findTransByToUserAndDateCreatedBetween
        query = em.createNamedQuery("findTransByToUserAndDateCreatedBetween").setParameter("toUserVS", userVS)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo",timePeriod.getDateTo());
        transactionList = query.getResultList();
        List<Map> transactionToList = new ArrayList<>();
        for(TransactionVS transaction : transactionList) {
            Map<String, Map<String, BigDecimal>> currencyMap = null;
            if((currencyMap = balancesMap.get(transaction.getCurrencyCode())) != null) {
                Map<String, BigDecimal> tagMap = balancesMap.get(transaction.getCurrencyCode());
                BigDecimal tagAccumulated = null;
                if((tagAccumulated = tagMap.get(transaction.getTag().getName())) != null) {
                    tagMap.put(transaction.getTag().getName(), tagAccumulated.add(transaction.getAmount()));
                } else tagMap.put(transaction.getTag().getName(), transaction.getAmount());
            } else {
                Map<String, BigDecimal> tagMap = new HashMap<>();
                tagMap.put(transaction.getTag().getName(), transaction.getAmount());
                balancesMap.put(transaction.getCurrencyCode(), currencyMap);
            }
            Map transactionVSMap = transactionVSBean.getTransactionMap(transaction);
            MessageSMIME messageSMIME = transaction.getMessageSMIME();
            transactionVSMap.put("messageSMIME", Base64.getUrlEncoder().encodeToString(messageSMIME.getContent()));
            transactionToList.add(transactionVSMap);
        }
        resultMap.put("transactionToList", transactionToList);
        resultMap.put("balancesTo", balancesMap);

        File userHistoryFile = new File(backupDir.getAbsolutePath() + "/" + userVS.getNif() + ".json");
        Files.write(Paths.get(userHistoryFile.getAbsolutePath()), new ObjectMapper().writeValueAsString(resultMap).getBytes());
        log.info("user: " + userVS.getId() + " - userHistoryFile:" + userHistoryFile.getAbsolutePath());
        return resultMap;
    }
}
