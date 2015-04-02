package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.SubscriptionVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.util.TransactionVSUtils;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

@Stateless
public class UserVSBean {

    private static Logger log = Logger.getLogger(UserVSBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject MessagesBean messages;
    @Inject ConfigVS config;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject UserVSBean userVSBean;
    @Inject SystemBean systemBean;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject TransactionVSBean transactionVSBean;
    
    
    public UserVS saveUser(MessageSMIME messageSMIMEReq) throws Exception {
        UserVS signer = messageSMIMEReq.getUserVS();
        if(!signatureBean.isUserAdmin(signer.getNif())) throw new ExceptionVS(messages.get("userWithoutPrivilegesErrorMsg",
                signer.getNif(), TypeVS.CERT_CA_NEW.toString()));
        ObjectNode dataJSON =(ObjectNode)  new ObjectMapper().readTree(messageSMIMEReq.getSMIME().getSignedContent());
        if (dataJSON.get("info") == null || dataJSON.get("certChainPEM") == null || dataJSON.get("operation") == null ||
                (TypeVS.CERT_USER_NEW != TypeVS.valueOf(dataJSON.get("operation").asText()))) {
            throw new ExceptionVS(messages.get("paramsErrorMsg"));
        }
        Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(
                dataJSON.get("certChainPEM").asText().getBytes());
        UserVS newUser = UserVS.getUserVS(certChain.iterator().next());
        signatureBean.verifyUserCertificate(newUser);
        newUser = subscriptionVSBean.checkUser(newUser);
        dao.merge(newUser.setState(UserVS.State.ACTIVE).setReason(dataJSON.get("info").asText()));
        return newUser;
    }

    public Map getSubscriptionVSDataMap(SubscriptionVS subscriptionVS){
        Map resultMap = new HashMap<>();
        resultMap.put("id", subscriptionVS.getId());
        resultMap.put("state", subscriptionVS.getState().toString());
        resultMap.put("dateCreated", subscriptionVS.getDateCreated());
        resultMap.put("dateActivated", subscriptionVS.getDateActivated());
        resultMap.put("dateCancelled", subscriptionVS.getDateCancelled());
        resultMap.put("lastUpdated", subscriptionVS.getLastUpdated());
        Map userDataMap = new HashMap<>();
        userDataMap.put("id", subscriptionVS.getUserVS().getId());
        userDataMap.put("IBAN", subscriptionVS.getUserVS().getIBAN());
        userDataMap.put("NIF", subscriptionVS.getUserVS().getNif());
        userDataMap.put("name", subscriptionVS.getUserVS().getFirstName() + " " + subscriptionVS.getUserVS().getLastName());
        resultMap.put("uservs", userDataMap);
        Map groupDataMap = new HashMap<>();
        groupDataMap.put("id", subscriptionVS.getGroupVS().getId());
        groupDataMap.put("name", subscriptionVS.getGroupVS().getName());
        resultMap.put("groupvs", groupDataMap);
        return resultMap;
    }

    public Map getSubscriptionVSDetailedDataMap(SubscriptionVS subscriptionVS){
        String subscriptionMessageURL = config.getContextURL() + "/messageSMIME/" + subscriptionVS.getSubscriptionSMIME().getId();
        List<String> adminMessages = new ArrayList<>();
        for(MessageSMIME messageSMIME : subscriptionVS.getAdminMessageSMIMESet()) {
            adminMessages.add(config.getContextURL() + "/messageSMIME/" + messageSMIME.getId());
        }
        Map resultMap = new HashMap<>();
        resultMap.put("id", subscriptionVS.getId());
        resultMap.put("state", subscriptionVS.getState().toString());
        resultMap.put("dateCreated", subscriptionVS.getDateCreated());
        resultMap.put("dateActivated", subscriptionVS.getDateActivated());
        resultMap.put("dateCancelled", subscriptionVS.getDateCancelled());
        resultMap.put("lastUpdated", subscriptionVS.getLastUpdated());
        resultMap.put("messageURL", subscriptionMessageURL);
        resultMap.put("adminMessages", adminMessages);
        Map userDataMap = new HashMap<>();
        userDataMap.put("id", subscriptionVS.getUserVS().getId());
        userDataMap.put("IBAN", subscriptionVS.getUserVS().getIBAN());
        userDataMap.put("NIF", subscriptionVS.getUserVS().getNif());
        userDataMap.put("name", subscriptionVS.getUserVS().getFirstName() + " " + subscriptionVS.getUserVS().getLastName());
        resultMap.put("uservs", userDataMap);
        Map groupDataMap = new HashMap<>();
        groupDataMap.put("id", subscriptionVS.getGroupVS().getId());
        groupDataMap.put("name", subscriptionVS.getGroupVS().getName());
        resultMap.put("groupvs", groupDataMap);
        return resultMap;
    }

    public Map getUserVSBasicDataMap(UserVS userVS){
        Map resultMap = new HashMap<>();
        resultMap.put("name", userVS.getName());
        resultMap.put("nif", userVS.getNif());
        return resultMap;
    }
    
    public Map getUserVSDataMap(UserVS userVS, boolean withCerts) throws Exception {
        Map resultMap = new HashMap<>();
        if(withCerts) {
            Query query = dao.getEM().createNamedQuery("findCertByUserAndState").setParameter("userVS", userVS)
                    .setParameter("state", CertificateVS.State.OK);
            List<CertificateVS> certificates = query.getResultList();
            List<Map> certList = new ArrayList<>();
            for(CertificateVS certificateVS : certificates) {
                Map certDataMap = new HashMap<>();
                certDataMap.put("serialNumber", certificateVS.getX509Cert().getSerialNumber());
                certDataMap.put("pemCert", new String(CertUtils.getPEMEncoded (certificateVS.getX509Cert()), "UTF-8"));
                certList.add(certDataMap);
            }
            resultMap.put("certificateList", certList);
        }
        resultMap.put("id", userVS.getId());
        resultMap.put("nif", userVS.getNif());
        resultMap.put("name", userVS.getName());
        resultMap.put("firstName", userVS.getFirstName());
        resultMap.put("lastName", userVS.getLastName());
        resultMap.put("IBAN", userVS.getIBAN());
        resultMap.put("state", userVS.getState().toString());
        resultMap.put("type", userVS.getType().toString());
        resultMap.put("reason", userVS.getReason());
        resultMap.put("description", userVS.getDescription());
        resultMap.put("connectedDevices", SessionVSManager.getInstance().connectedDeviceMap(userVS.getId()));
        return resultMap;
    }
    
    public Map getDataWithBalancesMap(UserVS userVS, DateUtils.TimePeriod timePeriod, boolean withResultCheck) throws Exception {
        Map resultMap = new HashMap<>();
        resultMap.put("timePeriod", timePeriod.getMap());
        resultMap.put("userVS", getUserVSDataMap(userVS, false));
        Map transactionListWithBalances = transactionVSBean.getTransactionListWithBalances(
                transactionVSBean.getTransactionFromList(userVS, timePeriod), TransactionVS.Source.FROM);
        resultMap.put("transactionFromList", transactionListWithBalances.get("transactionList"));
        resultMap.put("balancesFrom", transactionListWithBalances.get("balances"));

        transactionListWithBalances = transactionVSBean.getTransactionListWithBalances(
                transactionVSBean.getTransactionToList(userVS, timePeriod), TransactionVS.Source.TO);
        resultMap.put("transactionToList", transactionListWithBalances.get("transactionList"));
        resultMap.put("balancesTo", transactionListWithBalances.get("balances"));
        resultMap.put("resultMap", transactionListWithBalances.get("balances"));
        resultMap.put("balancesCash", TransactionVSUtils.balancesCash((Map<String, Map<String, Map>>) resultMap.get("balancesTo"),
                (Map<String, Map<String, BigDecimal>>) resultMap.get("balancesFrom")));
        //Map<String, Map<String, Map>> balancesTo, Map<String, Map<String, BigDecimal>> balancesFrom
        if(withResultCheck && UserVS.Type.SYSTEM != userVS.getType() && timePeriod.isCurrentWeekPeriod())
            currencyAccountBean.checkBalancesMap(userVS, (Map<String, Map>) resultMap.get("balancesCash"));
        return resultMap;
    }

    public Map getDataWithBalancesMap(UserVS userVS, DateUtils.TimePeriod timePeriod) throws Exception {
        return getDataWithBalancesMap(userVS, timePeriod, true);
    }
    
}