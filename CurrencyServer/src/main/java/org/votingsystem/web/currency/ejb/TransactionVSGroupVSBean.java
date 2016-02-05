package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class TransactionVSGroupVSBean {

    private static Logger log = Logger.getLogger(TransactionVSGroupVSBean.class.getName());

    @Inject SignatureBean signatureBean;
    @Inject WalletBean walletBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject TransactionVSBean transactionVSBean;


    public ResultListDto<TransactionVSDto> processTransactionVS(TransactionVSDto request,TagVS tagVS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        GroupVS groupVS = validateRequest(request);
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                groupVS.getIBAN(), tagVS, request.getAmount(), request.getCurrencyCode());
        if(request.getType() == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            return processTransactionVSForAllMembers(request, accountFromMovements, groupVS, tagVS);
        } else {
            List<TransactionVSDto> resultList = new ArrayList<>();
            BigDecimal numReceptors = new BigDecimal(request.getNumReceptors());
            BigDecimal userPart = request.getAmount().divide(numReceptors, 4, RoundingMode.FLOOR);
            if(!(request.getType() == TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP)) {
                throw new ExceptionVS("unknown transaction: " + request.getType().toString());
            }
            TransactionVS transactionParent =
                    dao.persist(request.getTransactionVS(groupVS, null, accountFromMovements, tagVS));
            transactionVSBean.updateCurrencyAccounts(transactionParent);
            ObjectMapper mapper = JSON.getMapper();
            for(UserVS toUser: request.getToUserVSList()) {
                TransactionVS triggeredTransaction = TransactionVS.generateTriggeredTransaction(
                        transactionParent, userPart, toUser, toUser.getIBAN());
                SMIMEMessage receipt = signatureBean.getSMIME(signatureBean.getSystemUser().getNif(),
                        toUser.getNif(), mapper.writeValueAsString(new TransactionVSDto(triggeredTransaction)),
                        request.getOperation().toString(), null);
                receipt.setHeader("TypeVS", request.getType().toString());
                MessageSMIME messageSMIMEReceipt = dao.persist(new MessageSMIME(receipt, TypeVS.FROM_GROUP_TO_ALL_MEMBERS,
                        request.getTransactionVSSMIME()));
                triggeredTransaction.setMessageSMIME(messageSMIMEReceipt);
                dao.persist(triggeredTransaction);
                transactionVSBean.updateCurrencyAccounts(triggeredTransaction);
                resultList.add(new TransactionVSDto(triggeredTransaction));
            }
            log.info("transactionType: " + request.getType().toString() + " - num. receptors: " +
                    request.getToUserVSList().size()  + " - TransactionVS parent id: " + transactionParent.getId() +
                    " - amount: " + request.getAmount().toString());
            ResultListDto<TransactionVSDto> resultListDto = new ResultListDto(resultList, request.getOperation());
            if (request.getType() == TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                resultListDto.setMessage(messages.get("transactionVSFromGroupToMemberGroupOKMsg", request.getAmount() +
                        " " + request.getCurrencyCode()));
                resultListDto.setStatusCode(ResponseVS.SC_OK);
            }
            return resultListDto;
        }
    }

    public GroupVS validateRequest(TransactionVSDto dto) throws ValidationExceptionVS {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Query query = dao.getEM().createNamedQuery("findUserByRepresentativeAndIBAN").setParameter(
                "representative", dto.getSigner()).setParameter("IBAN", dto.getFromUserIBAN());
        GroupVS groupVS = dao.getSingleResult(GroupVS.class, query);
        if(groupVS == null) {
            throw new ValidationExceptionVS(messages.get(
                    "groupNotFoundByIBANErrorMsg", dto.getFromUserIBAN(), dto.getSigner().getNif()));
        }
        if(dto.getType() != TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            List<UserVS> toUserVSList = new ArrayList<>();
            for(String groupUserIBAN : dto.getToUserIBAN()) {
                query = dao.getEM().createQuery("SELECT s FROM SubscriptionVS s WHERE s.groupVS =:groupVS AND " +
                        "s.state =:state AND s.userVS.IBAN =:IBAN").setParameter("groupVS", groupVS)
                        .setParameter("state", SubscriptionVS.State.ACTIVE)
                        .setParameter("IBAN", groupUserIBAN);
                SubscriptionVS subscription = dao.getSingleResult(SubscriptionVS.class, query);
                if(subscription == null) throw new ValidationExceptionVS(messages.get("groupUserNotFoundByIBANErrorMsg",
                        groupUserIBAN, groupVS.getName()));
                toUserVSList.add(subscription.getUserVS());
            }
            if(toUserVSList.isEmpty()) throw new ValidationExceptionVS("transaction without valid receptors");
            dto.setToUserVSList(toUserVSList);
        } else if (dto.getType() == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            query = dao.getEM().createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
                    .setParameter("state", SubscriptionVS.State.ACTIVE);
            dto.setNumReceptors(((Long)query.getSingleResult()).intValue());
        }
        return groupVS;
    }

    private ResultListDto<TransactionVSDto> processTransactionVSForAllMembers(TransactionVSDto request,
                 Map<CurrencyAccount, BigDecimal> accountFromMovements, GroupVS groupVS, TagVS tagVS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        BigDecimal numReceptors = new BigDecimal(request.getNumReceptors());
        BigDecimal userPart = request.getAmount().divide(numReceptors, 2, RoundingMode.FLOOR);
        TransactionVS transactionParent = dao.persist(request.getTransactionVS(groupVS, null, accountFromMovements, tagVS));
        transactionVSBean.updateCurrencyAccounts(transactionParent);
        Query query = dao.getEM().createQuery("SELECT s FROM SubscriptionVS s WHERE s.groupVS =:groupVS AND s.state =:state")
                .setParameter("groupVS", groupVS).setParameter("state", SubscriptionVS.State.ACTIVE);
        List<SubscriptionVS> subscriptionList = query.getResultList();
        if(subscriptionList.isEmpty()) throw new ValidationExceptionVS("the group has no active users");
        List<TransactionVSDto> resultList = new ArrayList<>();
        ObjectMapper mapper = JSON.getMapper();
        for(SubscriptionVS subscription : subscriptionList) {
            String toUserNIF = subscription.getUserVS().getNif();
            TransactionVSDto triggeredDto = request.getGroupVSChild(
                    toUserNIF, userPart, subscriptionList.size(), config.getContextURL());
            SMIMEMessage receipt = signatureBean.getSMIME(signatureBean.getSystemUser().getNif(),
                    toUserNIF, mapper.writeValueAsString(triggeredDto), request.getOperation().toString(), null);
            receipt.setHeader("TypeVS", request.getType().toString());
            MessageSMIME messageSMIMEReceipt = dao.persist(new MessageSMIME(receipt, TypeVS.FROM_GROUP_TO_ALL_MEMBERS,
                    request.getTransactionVSSMIME()));
            TransactionVS triggeredTransaction = dao.persist(TransactionVS.generateTriggeredTransaction(transactionParent,
                    userPart, subscription.getUserVS(), subscription.getUserVS().getIBAN()));
            resultList.add(new TransactionVSDto(triggeredTransaction));
            transactionVSBean.updateCurrencyAccounts(triggeredTransaction);
        }
        log.info("transactionVS: " + transactionParent.getId() + " - operation: " + request.getOperation().toString());
        ResultListDto<TransactionVSDto> resultDto = new ResultListDto(resultList);
        MessageSMIME requestMessageSMIME = request.getTransactionVSSMIME();
        SMIMEMessage parentReceipt = signatureBean.getSMIMEMultiSigned(request.getSigner().getNif(),
                requestMessageSMIME.getSMIME(), messages.get("fromGroupVSToAllMembersLbl"));
        parentReceipt.setHeader("TypeVS", request.getType().toString());
        dao.merge(requestMessageSMIME.setSMIME(parentReceipt));
        resultDto.setMessage(messages.get("transactionVSFromGroupToAllMembersGroupOKMsg",
                request.getAmount().toString() + " " + request.getCurrencyCode()));
        return resultDto;
    }

}