package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class TransactionVSGroupVSBean {

    private static Logger log = Logger.getLogger(TransactionVSGroupVSBean.class.getSimpleName());

    @Inject SignatureBean signatureBean;
    @Inject WalletBean walletBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject MessagesBean messages;


    public String processTransactionVS(TransactionVSBean.TransactionVSRequest request) throws Exception {
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                request.groupVS.getIBAN(), request.tag, request.amount, request.currencyCode);
        if(request.transactionType == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            return processTransactionVSForAllMembers(request, accountFromMovements);
        } else {
            BigDecimal userPart = request.amount.divide(request.numReceptors, 4, RoundingMode.FLOOR);
            if(request.transactionType != TransactionVS.Type.FROM_GROUP_TO_MEMBER ||
                    request.transactionType != TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                throw new ExceptionVS("unknown transaction: " + request.transactionType.toString());
            }
            TransactionVS transactionParent = dao.persist(TransactionVS.USERVS(request.groupVS, null, request.transactionType,
                    accountFromMovements, request.amount, request.currencyCode, request.subject, request.validTo,
                    request.messageSMIME, request.tag));
            for(UserVS toUser: request.toUserVSList) {
                dao.persist(TransactionVS.generateTriggeredTransaction(
                        transactionParent, userPart, toUser, toUser.getIBAN()));
            }
            log.info("transactionType: " + request.transactionType.toString() + " - num. receptors: " +
                    request.toUserVSList.size()  + " - TransactionVS parent id: " + transactionParent.getId() +
                    " - amount: " + request.amount.toString());
            if(request.transactionType == TransactionVS.Type.FROM_GROUP_TO_MEMBER) {
                return messages.get("transactionVSFromGroupToMemberOKMsg", request.amount + " " + request.currencyCode,
                        request.toUserVSList.iterator().next().getNif());
            } else if (request.transactionType == TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                return messages.get("transactionVSFromGroupToMemberGroupOKMsg", request.amount + " " + request.currencyCode);
            } else return null;
        }
    }

    private String processTransactionVSForAllMembers(TransactionVSBean.TransactionVSRequest request,
                                 Map<CurrencyAccount, BigDecimal> accountFromMovements) throws Exception {
        BigDecimal userPart = request.amount.divide(request.numReceptors, 2, RoundingMode.FLOOR);
        TransactionVS.Type transactionVSType = TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS;
        TransactionVS transactionParent = dao.persist(TransactionVS.USERVS(request.groupVS, null, transactionVSType,
                accountFromMovements, request.amount, request.currencyCode, request.subject, request.validTo,
                request.messageSMIME, request.tag));
        Query query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndState").setParameter("groupVS", request.groupVS)
                .setParameter("state", SubscriptionVS.State.ACTIVE);
        List<SubscriptionVS> subscriptionList = query.getResultList();
        for(SubscriptionVS subscription : subscriptionList) {
            SMIMEMessage receipt = request.signReceptorData(request.messageSMIME.getId(), subscription.getUserVS().getNif(),
                    subscriptionList.size(), userPart);
            MessageSMIME messageSMIMEReceipt = dao.persist(new MessageSMIME(receipt, TypeVS.FROM_GROUP_TO_ALL_MEMBERS,
                    request.messageSMIME));
            dao.persist(TransactionVS.generateTriggeredTransaction(transactionParent, userPart, subscription.getUserVS(),
                    subscription.getUserVS().getIBAN()));
        }
        log.info("transactionVS: " + transactionParent.getId() + " - operation: " + request.operation.toString());
        return messages.get("transactionVSFromGroupToAllMembersGroupOKMsg", request.amount.toString() + " " + request.currencyCode);
    }

}
