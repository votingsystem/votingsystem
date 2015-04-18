package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.GroupVS;
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
import java.util.ArrayList;
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


    public ResultListDto<TransactionVSDto> processTransactionVS(TransactionVSDto request, GroupVS groupVS) throws Exception {
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                groupVS.getIBAN(), request.getTag(), request.getAmount(), request.getCurrencyCode());
        if(request.getType() == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            return processTransactionVSForAllMembers(request, accountFromMovements, groupVS);
        } else {
            ResultListDto<TransactionVSDto> resultListDto = null;
            List<TransactionVSDto> resultList = new ArrayList<>();
            BigDecimal numReceptors = new BigDecimal(request.getNumReceptors());
            BigDecimal userPart = request.getAmount().divide(numReceptors, 4, RoundingMode.FLOOR);
            if(request.getType() != TransactionVS.Type.FROM_GROUP_TO_MEMBER ||
                    request.getType() != TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                throw new ExceptionVS("unknown transaction: " + request.getType().toString());
            }
            TransactionVS transactionParent = dao.persist(request.getTransactionVS(groupVS, null, accountFromMovements));
            for(UserVS toUser: request.getToUserVSList()) {
                TransactionVS triggeredTransaction = dao.persist(TransactionVS.generateTriggeredTransaction(
                        transactionParent, userPart, toUser, toUser.getIBAN()));
                resultList.add(new TransactionVSDto(triggeredTransaction));
            }
            log.info("transactionType: " + request.getType().toString() + " - num. receptors: " +
                    request.getToUserVSList().size()  + " - TransactionVS parent id: " + transactionParent.getId() +
                    " - amount: " + request.getAmount().toString());
            resultListDto = new ResultListDto(resultList, request.getOperation());
            if(request.getType() == TransactionVS.Type.FROM_GROUP_TO_MEMBER) {
                resultListDto.setMessage(messages.get("transactionVSFromGroupToMemberOKMsg", request.getAmount() + " " +
                        request.getCurrencyCode(), request.getToUserVSList().iterator().next().getNif()));
            } else if (request.getType() == TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                resultListDto.setMessage(messages.get("transactionVSFromGroupToMemberGroupOKMsg", request.getAmount() + " " +
                        request.getCurrencyCode()));
            }
            return resultListDto;
        }
    }

    private ResultListDto<TransactionVSDto> processTransactionVSForAllMembers(TransactionVSDto request,
                                 Map<CurrencyAccount, BigDecimal> accountFromMovements, GroupVS groupVS) throws Exception {
        BigDecimal numReceptors = new BigDecimal(request.getNumReceptors());
        BigDecimal userPart = request.getAmount().divide(numReceptors, 2, RoundingMode.FLOOR);
        TransactionVS transactionParent = dao.persist(request.getTransactionVS(groupVS, null, accountFromMovements));
        Query query = dao.getEM().createQuery("SELECT s FROM SubscriptionVS s WHERE s.groupVS =:groupVS AND s.state =:state")
                .setParameter("groupVS", groupVS).setParameter("state", SubscriptionVS.State.ACTIVE);
        List<SubscriptionVS> subscriptionList = query.getResultList();
        List<TransactionVSDto> resultList = new ArrayList<>();
        for(SubscriptionVS subscription : subscriptionList) {
            String toUserNIF = subscription.getUserVS().getNif();
            TransactionVSDto triggeredDto = request.getGroupVSChild(
                    toUserNIF, userPart, subscriptionList.size(), config.getRestURL());
            SMIMEMessage receipt = signatureBean.getSMIME(signatureBean.getSystemUser().getNif(),
                    toUserNIF, JSON.getMapper().writeValueAsString(triggeredDto), request.getOperation().toString(), null);
            MessageSMIME messageSMIMEReceipt = dao.persist(new MessageSMIME(receipt, TypeVS.FROM_GROUP_TO_ALL_MEMBERS,
                    request.getTransactionVSSMIME()));
            TransactionVS triggeredTransaction = dao.persist(TransactionVS.generateTriggeredTransaction(transactionParent,
                    userPart, subscription.getUserVS(), subscription.getUserVS().getIBAN()));
            resultList.add(new TransactionVSDto(triggeredTransaction));
        }
        log.info("transactionVS: " + transactionParent.getId() + " - operation: " + request.getOperation().toString());
        ResultListDto<TransactionVSDto> resultDto = new ResultListDto(resultList);
        resultDto.setMessage(messages.get("transactionVSFromGroupToAllMembersGroupOKMsg", request.getAmount().toString() + " " +
                request.getCurrencyCode()));
        return resultDto;
    }

}
