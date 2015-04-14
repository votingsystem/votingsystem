package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.*;
import org.votingsystem.model.currency.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.util.TransactionVSUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.*;
import java.util.Currency;
import java.util.logging.Logger;

@Stateless
public class GroupVSBean {

    private static Logger log = Logger.getLogger(GroupVSBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject MessagesBean messages;
    @Inject IBANBean ibanBean;
    @Inject ConfigVS config;
    @Inject UserVSBean userVSBean;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject SystemBean systemBean;
    @Inject TransactionVSBean transactionVSBean;


    public GroupVS cancelGroup(GroupVS groupVS, MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        log.info("signer:" + signer.getNif());
        if(!groupVS.getRepresentative().getNif().equals(signer.getNif()) && !signatureBean.isUserAdmin(signer.getNif())) {
            throw new ExceptionVS("operation: " +  TypeVS.CURRENCY_GROUP_CANCEL.toString() +
                    " - userWithoutGroupPrivilegesErrorMsg - user: " + signer.getNif() + " - group: " + groupVS.getName());
        }
        GroupVSDto request = messageSMIME.getSignedContent(GroupVSDto.class);
        request.validateCancelRequest();
        dao.merge(groupVS.setState(UserVS.State.CANCELED));
        return groupVS;
    }

    public GroupVS editGroup(GroupVS groupVS, MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        log.info("signer:" + signer.getNif());
        if(!groupVS.getRepresentative().getNif().equals(messageSMIME.getUserVS().getNif()) &&
                !signatureBean.isUserAdmin(messageSMIME.getUserVS().getNif())) {
            throw new ExceptionVS("operation: " +  TypeVS.CURRENCY_GROUP_EDIT.toString() +
                    " - userWithoutGroupPrivilegesErrorMsg - user: " + signer.getNif() + " - group: " + groupVS.getName());
        }
        GroupVSDto request = messageSMIME.getSignedContent(GroupVSDto.class);
        request.validateEditRequest();
        if(request.getId().longValue() != groupVS.getId().longValue()) {
            throw new ExceptionVS("group id error - expected: " + groupVS.getId() + " - found: " + request.getId());
        }
        dao.merge(groupVS.setDescription(request.getInfo()));
        return groupVS;
    }

    public GroupVS saveGroup(MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        log.info("signer:" + signer.getNif());
        GroupVSDto request = validateNewGroupRequest(messageSMIME.getSignedContent(GroupVSDto.class)) ;
        Query query = dao.getEM().createNamedQuery("findGroupByName").setParameter("name", request.getName().trim());
        GroupVS groupVS = dao.getSingleResult(GroupVS.class, query);
        if(groupVS == null) {
            throw new ExceptionVS(messages.get("nameGroupRepeatedMsg", request.getName()));
        }
        currencyAccountBean.checkUserVSAccount(signer);
        groupVS = dao.persist(new GroupVS(request.getName().trim(), UserVS.State.ACTIVE, signer,
                request.getInfo(), request.getTagSet()));
        groupVS.setIBAN(ibanBean.getIBAN(groupVS.getId()));
        dao.persist(new CurrencyAccount(groupVS, BigDecimal.ZERO, Currency.getInstance("EUR").getCurrencyCode(),
                config.getTag(TagVS.WILDTAG)));
        String fromUser = config.getServerName();
        String toUser = signer.getNif();
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(fromUser, toUser,
                messageSMIME.getSMIME(), messages.get("newGroupVSReceiptSubject"));
        messageSMIME.setSMIME(receipt);
        return groupVS;
    }

    private GroupVSDto validateNewGroupRequest(GroupVSDto groupVSDto) throws ValidationExceptionVS {
        groupVSDto.validateNewGroupRequest();
        if(groupVSDto.getTagSet() != null) {
            Set<TagVS> resultTagVSSet = new HashSet<>();
            for(TagVS tagVS: groupVSDto.getTagSet()) {
                TagVS tagVSDB = config.getTag(tagVS.getName());
                if(tagVSDB != null) resultTagVSSet.add(tagVSDB);
                else throw new ValidationExceptionVS(tagVS.getName() + " not found");
            }
            groupVSDto.setTagSet(resultTagVSSet);
        }
        return groupVSDto;
    }

    public SubscriptionVS subscribe(MessageSMIME messageSMIME) throws Exception {
        SubscriptionVS subscriptionVS = null;
        UserVS signer = messageSMIME.getUserVS();
        log.info("signer: " + signer.getNif());
        GroupVSDto request = validateNewGroupRequest(messageSMIME.getSignedContent(GroupVSDto.class)) ;
        request.validateSubscriptionRequest();
        GroupVS groupVS = dao.find(GroupVS.class, request.getId());
        if(groupVS.getRepresentative().getNif().equals(signer.getNif())) {
            throw new ExceptionVS(messages.get("representativeSubscribedErrorMsg", groupVS.getRepresentative().getNif(),
                    groupVS.getName()));
        }
        Query query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndUser").setParameter("groupVS", groupVS)
                .setParameter("userVS", signer);
        subscriptionVS = dao.getSingleResult(SubscriptionVS.class, query);
        if(subscriptionVS != null) {
            throw new ExceptionVS(messages.get("userAlreadySubscribedErrorMsg", signer.getNif(), groupVS.getName()));
        }
        subscriptionVS = dao.persist(new SubscriptionVS(signer, groupVS, SubscriptionVS.State.PENDING, messageSMIME));
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
        Query query = dao.getEM().createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
                .setParameter("state", SubscriptionVS.State.ACTIVE);
        resultMap.put("numActiveUsers", (long)query.getSingleResult());
        query = dao.getEM().createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
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

    
}
