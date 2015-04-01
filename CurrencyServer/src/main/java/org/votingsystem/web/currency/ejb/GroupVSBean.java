package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.votingsystem.model.*;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.util.TransactionVSUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

@Stateless
public class GroupVSBean {

    private static Logger log = Logger.getLogger(GroupVSBean.class.getSimpleName());

    @PersistenceContext private EntityManager em;
    @Inject
    DAOBean dao;
    @Inject IBANBean ibanBean;
    @Inject ConfigVS config;
    @Inject UserVSBean userVSBean;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject
    SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject SystemBean systemBean;
    @Inject TransactionVSBean transactionVSBean;


    public GroupVS cancelGroup(GroupVS groupVS, MessageSMIME messageSMIMEReq) throws Exception {
        UserVS signer = messageSMIMEReq.getUserVS();
        log.info("signer:" + signer.getNif());
        if(!groupVS.getRepresentative().getNif().equals(signer.getNif()) && !signatureBean.isUserAdmin(signer.getNif())) {
            throw new ExceptionVS("operation: " +  TypeVS.CURRENCY_GROUP_CANCEL.toString() +
                    " - userWithoutGroupPrivilegesErrorMsg - user: " + signer.getNif() + " - group: " + groupVS.getName());
        }
        GroupVSRequest request = new GroupVSRequest().getCancelRequest(messageSMIMEReq.getSMIME().getSignedContent());
        em.merge(groupVS.setState(UserVS.State.CANCELED));
        return groupVS;
    }

    public GroupVS editGroup(GroupVS groupVS, MessageSMIME messageSMIMEReq) throws Exception {
        UserVS signer = messageSMIMEReq.getUserVS();
        log.info("signer:" + signer.getNif());
        if(!groupVS.getRepresentative().getNif().equals(messageSMIMEReq.getUserVS().getNif()) &&
                !signatureBean.isUserAdmin(messageSMIMEReq.getUserVS().getNif())) {
            throw new ExceptionVS("operation: " +  TypeVS.CURRENCY_GROUP_EDIT.toString() +
                    " - userWithoutGroupPrivilegesErrorMsg - user: " + signer.getNif() + " - group: " + groupVS.getName());
        }
        GroupVSRequest request = new GroupVSRequest().getEditRequest(messageSMIMEReq.getSMIME().getSignedContent());
        if(request.id != groupVS.getId()) {
            throw new ExceptionVS("group id error - expected: " + groupVS.getId() + " - found: " + request.id);
        }
        em.merge(groupVS.setDescription(request.groupvsInfo));
        return groupVS;
    }

    public GroupVS saveGroup(MessageSMIME messageSMIMEReq) throws Exception {
        UserVS signer = messageSMIMEReq.getUserVS();
        log.info("signer:" + signer.getNif());
        GroupVSRequest request = new GroupVSRequest(messageSMIMEReq.getSMIME().getSignedContent());
        Query query = em.createNamedQuery("findGroupByName").setParameter("name", request.groupvsName.trim());
        GroupVS groupVS = dao.getSingleResult(GroupVS.class, query);
        if(groupVS == null) {
            throw new ExceptionVS(config.get("nameGroupRepeatedMsg", request.groupvsName));
        }
        currencyAccountBean.checkUserVSAccount(signer);
        groupVS = dao.persist(new GroupVS(request.groupvsName.trim(), UserVS.State.ACTIVE, signer,
                request.groupvsInfo, request.tagSet));
        groupVS.setIBAN(ibanBean.getIBAN(groupVS.getId()));
        dao.persist(new CurrencyAccount(groupVS, BigDecimal.ZERO, Currency.getInstance("EUR").getCurrencyCode(),
                config.getTag(TagVS.WILDTAG)));
        String fromUser = config.getServerName();
        String toUser = signer.getNif();
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(fromUser, toUser,
                messageSMIMEReq.getSMIME(), config.get("newGroupVSReceiptSubject"));
        messageSMIMEReq.setSMIME(receipt);
        return groupVS;
    }

    public SubscriptionVS subscribe(MessageSMIME messageSMIMEReq) throws Exception {
        SubscriptionVS subscriptionVS = null;
        UserVS signer = messageSMIMEReq.getUserVS();
        log.info("signer: " + signer.getNif());
        GroupVSRequest request = new GroupVSRequest().getSubscribeRequest(messageSMIMEReq.getSMIME().getSignedContent());
        GroupVS groupVS = dao.find(GroupVS.class, request.id);
        if(groupVS.getRepresentative().getNif().equals(signer.getNif())) {
            throw new ExceptionVS(config.get("representativeSubscribedErrorMsg", groupVS.getRepresentative().getNif(),
                    groupVS.getName()));
        }
        Query query = em.createNamedQuery("findSubscriptionByGroupAndUser").setParameter("groupVS", groupVS)
                .setParameter("userVS", signer);
        subscriptionVS = dao.getSingleResult(SubscriptionVS.class, query);
        if(subscriptionVS != null) {
            throw new ExceptionVS(config.get("userAlreadySubscribedErrorMsg", signer.getNif(), groupVS.getName()));
        }
        subscriptionVS = dao.persist(new SubscriptionVS(signer, groupVS, SubscriptionVS.State.PENDING, messageSMIMEReq));
        return subscriptionVS;
    }

    public Map getGroupVSDataMap(GroupVS groupVS) throws Exception {
        Map resultMap = new HashMap<>();
        resultMap.put("id", groupVS.getId());
        resultMap.put("IBAN", groupVS.getIBAN());
        resultMap.put("name", groupVS.getName());
        resultMap.put("description", groupVS.getDescription());
        resultMap.put("state", groupVS.getState().toString());
        resultMap.put("dateCreated", groupVS.getDateCreated());
        resultMap.put("representative", userVSBean.getUserVSDataMap(groupVS.getRepresentative(), false));
        resultMap.put("type", groupVS.getType().toString());
        if(groupVS.getTagVSSet() != null) {
            List<Map> tagList = new ArrayList<>();
            for(TagVS tag :  groupVS.getTagVSSet()) {
                Map tagMap = new HashMap<>();
                tagMap.put("id", tag.getId());
                tagMap.put("name", tag.getName());
                tagList.add(tagMap);
            }
            resultMap.put("tags", tagList);
        }
        resultMap.put("numActiveUsers", groupVS.getType().toString());
        resultMap.put("numPendingUsers", groupVS.getType().toString());
        Query query = em.createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
                .setParameter("state", SubscriptionVS.State.ACTIVE);
        resultMap.put("numActiveUsers", (long)query.getSingleResult());
        query = em.createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
                .setParameter("state", SubscriptionVS.State.PENDING);
        resultMap.put("numPendingUsers", (long)query.getSingleResult());
        return resultMap;
    }

    public Map getDataMap(GroupVS groupVS, DateUtils.TimePeriod timePeriod) throws Exception {
        Map resultMap = new HashMap<>();
        resultMap.put("timePeriod",timePeriod.getMap());
        resultMap.put("userVS", getGroupVSDataMap(groupVS));
        return resultMap;
    }
    
    public Map getDataWithBalancesMap(UserVS groupVS, DateUtils.TimePeriod timePeriod) throws Exception {
        Map resultMap = new HashMap<>();
        resultMap.put("timePeriod", timePeriod.getMap());
        resultMap.put("userVS", getGroupVSDataMap((GroupVS) groupVS));

        Map transactionListWithBalances = transactionVSBean.getTransactionListWithBalances(
                transactionVSBean.getTransactionFromList(groupVS, timePeriod), TransactionVS.Source.FROM);
        resultMap.put("transactionFromList", transactionListWithBalances.get("transactionList"));
        resultMap.put("balancesFrom", transactionListWithBalances.get("balances"));


        transactionListWithBalances = transactionVSBean.getTransactionListWithBalances(
                transactionVSBean.getTransactionToList(groupVS, timePeriod), TransactionVS.Source.TO);
        resultMap.put("transactionToList", transactionListWithBalances.get("transactionList"));
        resultMap.put("balancesTo", transactionListWithBalances.get("balances"));
        resultMap.put("balancesCash", TransactionVSUtils.balancesCash((Map<String, Map<String, Map>>)resultMap.get("balancesTo"),
                (Map<String, Map<String, BigDecimal>>)resultMap.get("balancesFrom")));

        currencyAccountBean.checkBalancesMap(groupVS, (Map<String, Map>) resultMap.get("balancesCash"));
        return resultMap;
    }

    private class GroupVSRequest {
        String groupvsName, groupvsInfo;
        TypeVS operation;
        Long id;
        Set<TagVS> tagSet = new HashSet<>();
        public GroupVSRequest() {}
        public GroupVSRequest(String signedContent) throws ExceptionVS, IOException {
            JsonNode dataJSON = new ObjectMapper().readTree(signedContent);
            if(dataJSON.get("operation") == null) throw new ValidationExceptionVS("missing param 'operation'");
            operation = TypeVS.valueOf(dataJSON.get("operation").asText());
            if(dataJSON.get("groupvsName") == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
            groupvsName = dataJSON.get("groupvsName").asText();
            if(dataJSON.get("groupvsInfo") == null) throw new ValidationExceptionVS("missing param 'groupvsInfo'");
            groupvsInfo = dataJSON.get("groupvsInfo").asText();
            if(TypeVS.CURRENCY_GROUP_NEW != operation)   throw new ValidationExceptionVS(
                    "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + operation.toString());
            Iterator<JsonNode> ite = ((ArrayNode)dataJSON.get("tags")).elements();
            while (ite.hasNext()) {
                String tagName = ite.next().asText();
                TagVS tagVS = config.getTag(tagName);
                if(tagVS != null) tagSet.add(tagVS);
                else throw new ValidationExceptionVS(tagName + " not found");
            }
        }

        public GroupVSRequest getCancelRequest(String signedContent) throws IOException, ValidationExceptionVS {
            JsonNode dataJSON = new ObjectMapper().readTree(signedContent);
            if(dataJSON.get("operation") == null) throw new ValidationExceptionVS("missing param 'operation'");
            operation = TypeVS.valueOf(dataJSON.get("operation").asText());
            if(TypeVS.CURRENCY_GROUP_CANCEL != operation) throw new ValidationExceptionVS(
                    "operation expected: 'CURRENCY_GROUP_CANCEL' - operation found: " + operation.toString());
            if(dataJSON.get("groupvsName") == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
            groupvsName = dataJSON.get("groupvsName").asText();
            if(dataJSON.get("id") == null) throw new ValidationExceptionVS("missing param 'id'");
            id = dataJSON.get("id").asLong();
            return this;
        }

        public GroupVSRequest getEditRequest(String signedContent) throws IOException, ValidationExceptionVS {
            JsonNode dataJSON = new ObjectMapper().readTree(signedContent);
            if(dataJSON.get("operation") == null) throw new ValidationExceptionVS("missing param 'operation'");
            operation = TypeVS.valueOf(dataJSON.get("operation").asText());
            if(TypeVS.CURRENCY_GROUP_NEW != operation) throw new ValidationExceptionVS(
                    "operation expected: 'CURRENCY_GROUP_NEW' - operation found: " + operation.toString());
            if(dataJSON.get("groupvsName") == null) throw new ValidationExceptionVS("missing param 'groupvsName'");
            groupvsName = dataJSON.get("groupvsName").asText();
            if(dataJSON.get("id") == null) throw new ValidationExceptionVS("missing param 'id'");
            id = dataJSON.get("id").asLong();
            if(dataJSON.get("groupvsInfo") == null) throw new ValidationExceptionVS("missing param 'groupvsInfo'");
            groupvsInfo = dataJSON.get("groupvsInfo").asText();
            return this;
        }

        public GroupVSRequest getSubscribeRequest(String signedContent) throws IOException, ValidationExceptionVS {
            JsonNode dataJSON = new ObjectMapper().readTree(signedContent);
            if(dataJSON.get("operation") == null) throw new ValidationExceptionVS("missing param 'operation'");
            operation = TypeVS.valueOf(dataJSON.get("operation").asText());
            if(TypeVS.CURRENCY_GROUP_SUBSCRIBE != operation) throw new ValidationExceptionVS(
                    "operation expected: 'CURRENCY_GROUP_SUBSCRIBE' - operation found: " + operation.toString());
            if(dataJSON.get("groupvs") == null || dataJSON.get("groupvs").get("id") == null)
                throw new ValidationExceptionVS("missing param 'groupvs.id'");
            id = dataJSON.get("groupvs").get("id").asLong();
            return this;
        }

    }
    
}
