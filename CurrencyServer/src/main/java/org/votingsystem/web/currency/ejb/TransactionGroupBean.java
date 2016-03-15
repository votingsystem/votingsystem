package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Group;
import org.votingsystem.model.currency.Subscription;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
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
public class TransactionGroupBean {

    private static Logger log = Logger.getLogger(TransactionGroupBean.class.getName());

    @Inject CMSBean cmsBean;
    @Inject WalletBean walletBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject
    TransactionBean transactionBean;


    public ResultListDto<TransactionDto> processTransaction(TransactionDto request, TagVS tagVS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Group group = validateRequest(request);
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                group.getIBAN(), tagVS, request.getAmount(), request.getCurrencyCode());
        if(request.getType() == Transaction.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            return processTransactionForAllMembers(request, accountFromMovements, group, tagVS);
        } else {
            List<TransactionDto> resultList = new ArrayList<>();
            BigDecimal numReceptors = new BigDecimal(request.getNumReceptors());
            BigDecimal userPart = request.getAmount().divide(numReceptors, 4, RoundingMode.FLOOR);
            if(!(request.getType() == Transaction.Type.FROM_GROUP_TO_MEMBER_GROUP)) {
                throw new ExceptionVS("unknown transaction: " + request.getType().toString());
            }
            Transaction transactionParent =
                    dao.persist(request.getTransaction(group, null, accountFromMovements, tagVS));
            transactionBean.updateCurrencyAccounts(transactionParent);
            ObjectMapper mapper = JSON.getMapper();
            for(User toUser: request.getToUserList()) {
                Transaction triggeredTransaction = Transaction.generateTriggeredTransaction(
                        transactionParent, userPart, toUser, toUser.getIBAN());
                CMSSignedMessage receipt = cmsBean.signData(mapper.writeValueAsBytes(
                        new TransactionDto(triggeredTransaction)));
                CMSMessage cmsMessageReceipt = dao.persist(new CMSMessage(receipt, TypeVS.FROM_GROUP_TO_ALL_MEMBERS,
                        request.getCmsMessage_DB()));
                triggeredTransaction.setCmsMessage(cmsMessageReceipt);
                dao.persist(triggeredTransaction);
                transactionBean.updateCurrencyAccounts(triggeredTransaction);
                resultList.add(new TransactionDto(triggeredTransaction));
            }
            log.info("transactionType: " + request.getType().toString() + " - num. receptors: " +
                    request.getToUserList().size()  + " - Transaction parent id: " + transactionParent.getId() +
                    " - amount: " + request.getAmount().toString());
            ResultListDto<TransactionDto> resultListDto = new ResultListDto(resultList, request.getOperation());
            if (request.getType() == Transaction.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                resultListDto.setMessage(messages.get("transactionFromGroupToMemberGroupOKMsg", request.getAmount() +
                        " " + request.getCurrencyCode()));
                resultListDto.setStatusCode(ResponseVS.SC_OK);
            }
            return resultListDto;
        }
    }

    public Group validateRequest(TransactionDto dto) throws ValidationException {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Query query = dao.getEM().createNamedQuery("findUserByRepresentativeAndIBAN").setParameter(
                "representative", dto.getSigner()).setParameter("IBAN", dto.getFromUserIBAN());
        Group group = dao.getSingleResult(Group.class, query);
        if(group == null) {
            throw new ValidationException(messages.get(
                    "groupNotFoundByIBANErrorMsg", dto.getFromUserIBAN(), dto.getSigner().getNif()));
        }
        if(dto.getType() != Transaction.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            List<User> toUserList = new ArrayList<>();
            for(String groupUserIBAN : dto.getToUserIBAN()) {
                query = dao.getEM().createQuery("SELECT s FROM Subscription s WHERE s.group =:group AND " +
                        "s.state =:state AND s.user.IBAN =:IBAN").setParameter("group", group)
                        .setParameter("state", Subscription.State.ACTIVE)
                        .setParameter("IBAN", groupUserIBAN);
                Subscription subscription = dao.getSingleResult(Subscription.class, query);
                if(subscription == null) throw new ValidationException(messages.get("groupUserNotFoundByIBANErrorMsg",
                        groupUserIBAN, group.getName()));
                toUserList.add(subscription.getUser());
            }
            if(toUserList.isEmpty()) throw new ValidationException("transaction without valid receptors");
            dto.setToUserList(toUserList);
        } else if (dto.getType() == Transaction.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            query = dao.getEM().createNamedQuery("countSubscriptionByGroupAndState").setParameter("group", group)
                    .setParameter("state", Subscription.State.ACTIVE);
            dto.setNumReceptors(((Long)query.getSingleResult()).intValue());
        }
        return group;
    }

    private ResultListDto<TransactionDto> processTransactionForAllMembers(TransactionDto request,
                                                                          Map<CurrencyAccount, BigDecimal> accountFromMovements, Group group, TagVS tagVS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        BigDecimal numReceptors = new BigDecimal(request.getNumReceptors());
        BigDecimal userPart = request.getAmount().divide(numReceptors, 2, RoundingMode.FLOOR);
        Transaction transactionParent = dao.persist(request.getTransaction(group, null, accountFromMovements, tagVS));
        transactionBean.updateCurrencyAccounts(transactionParent);
        Query query = dao.getEM().createQuery("SELECT s FROM Subscription s WHERE s.group =:group AND s.state =:state")
                .setParameter("group", group).setParameter("state", Subscription.State.ACTIVE);
        List<Subscription> subscriptionList = query.getResultList();
        if(subscriptionList.isEmpty()) throw new ValidationException("the group has no active users");
        List<TransactionDto> resultList = new ArrayList<>();
        ObjectMapper mapper = JSON.getMapper();
        for(Subscription subscription : subscriptionList) {
            String toUserNIF = subscription.getUser().getNif();
            TransactionDto triggeredDto = request.getGroupChild(
                    toUserNIF, userPart, subscriptionList.size(), config.getContextURL());
            CMSSignedMessage receipt = cmsBean.signData(mapper.writeValueAsBytes(triggeredDto));
            dao.persist(new CMSMessage(receipt, TypeVS.FROM_GROUP_TO_ALL_MEMBERS, request.getCmsMessage_DB()));
            Transaction triggeredTransaction = dao.persist(Transaction.generateTriggeredTransaction(transactionParent,
                    userPart, subscription.getUser(), subscription.getUser().getIBAN()));
            resultList.add(new TransactionDto(triggeredTransaction));
            transactionBean.updateCurrencyAccounts(triggeredTransaction);
        }
        log.info("transaction: " + transactionParent.getId() + " - operation: " + request.getOperation().toString());
        ResultListDto<TransactionDto> resultDto = new ResultListDto(resultList);
        CMSMessage requestCMSMessage = request.getCmsMessage_DB();
        CMSSignedMessage parentReceipt = cmsBean.addSignature(requestCMSMessage.getCMS());
        dao.merge(requestCMSMessage.setCMS(parentReceipt));
        resultDto.setMessage(messages.get("transactionFromGroupToAllMembersGroupOKMsg",
                request.getAmount().toString() + " " + request.getCurrencyCode()));
        return resultDto;
    }

}