package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.dto.currency.TransactionVSPartDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
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


    public String processTransactionVS(TransactionVSDto request) throws Exception {
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                request.getGroupVS().getIBAN(), request.getTag(), request.getAmount(), request.getCurrencyCode());
        if(request.getType() == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            return processTransactionVSForAllMembers(request, accountFromMovements);
        } else {
            BigDecimal numReceptors = new BigDecimal(request.getNumReceptors());
            BigDecimal userPart = request.getAmount().divide(numReceptors, 4, RoundingMode.FLOOR);
            if(request.getType() != TransactionVS.Type.FROM_GROUP_TO_MEMBER ||
                    request.getType() != TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                throw new ExceptionVS("unknown transaction: " + request.getType().toString());
            }
            TransactionVS transactionParent = dao.persist(TransactionVS.USERVS(request.getGroupVS(), null, request.getType(),
                    accountFromMovements, request.getAmount(), request.getCurrencyCode(), request.getSubject(),
                    request.getValidTo(), request.getTransactionVSSMIME(), request.getTag()));
            for(UserVS toUser: request.getToUserVSList()) {
                dao.persist(TransactionVS.generateTriggeredTransaction(
                        transactionParent, userPart, toUser, toUser.getIBAN()));
            }
            log.info("transactionType: " + request.getType().toString() + " - num. receptors: " +
                    request.getToUserVSList().size()  + " - TransactionVS parent id: " + transactionParent.getId() +
                    " - amount: " + request.getAmount().toString());
            if(request.getType() == TransactionVS.Type.FROM_GROUP_TO_MEMBER) {
                return messages.get("transactionVSFromGroupToMemberOKMsg", request.getAmount() + " " +
                        request.getCurrencyCode(), request.getToUserVSList().iterator().next().getNif());
            } else if (request.getType() == TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                return messages.get("transactionVSFromGroupToMemberGroupOKMsg", request.getAmount() + " " +
                        request.getCurrencyCode());
            } else return null;
        }
    }

    private String processTransactionVSForAllMembers(TransactionVSDto request,
                                 Map<CurrencyAccount, BigDecimal> accountFromMovements) throws Exception {
        BigDecimal numReceptors = new BigDecimal(request.getNumReceptors());
        BigDecimal userPart = request.getAmount().divide(numReceptors, 2, RoundingMode.FLOOR);
        TransactionVS.Type transactionVSType = TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS;
        TransactionVS transactionParent = dao.persist(TransactionVS.USERVS(request.getGroupVS(), null, transactionVSType,
                accountFromMovements, request.getAmount(), request.getCurrencyCode(), request.getSubject(),
                request.getValidTo(), request.getTransactionVSSMIME(), request.getTag()));
        Query query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndState").setParameter(
                "groupVS", request.getGroupVS()).setParameter("state", SubscriptionVS.State.ACTIVE);
        List<SubscriptionVS> subscriptionList = query.getResultList();
        for(SubscriptionVS subscription : subscriptionList) {
            SMIMEMessage receipt = signReceptorData(request, subscription.getUserVS().getNif(),
                    subscriptionList.size(), userPart);
            MessageSMIME messageSMIMEReceipt = dao.persist(new MessageSMIME(receipt, TypeVS.FROM_GROUP_TO_ALL_MEMBERS,
                    request.getTransactionVSSMIME()));
            dao.persist(TransactionVS.generateTriggeredTransaction(transactionParent, userPart, subscription.getUserVS(),
                    subscription.getUserVS().getIBAN()));
        }
        log.info("transactionVS: " + transactionParent.getId() + " - operation: " + request.getOperation().toString());
        return messages.get("transactionVSFromGroupToAllMembersGroupOKMsg", request.getAmount().toString() + " " +
                request.getCurrencyCode());
    }

    SMIMEMessage signReceptorData(TransactionVSDto dto, String toUserNif, int numReceptors,
                                  BigDecimal userPart) throws Exception {
        TransactionVSPartDto transactionVSPartDto = new TransactionVSPartDto(dto.getTransactionVSSMIME().getId(),
                toUserNif, numReceptors, userPart);
        return signatureBean.getSMIME(signatureBean.getSystemUser().getNif(),
                toUserNif, JSON.getMapper().writeValueAsString(transactionVSPartDto), dto.getType().toString(), null);
    }

}
