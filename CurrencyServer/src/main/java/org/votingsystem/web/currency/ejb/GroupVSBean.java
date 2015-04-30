package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class GroupVSBean {

    private static Logger log = Logger.getLogger(GroupVSBean.class.getSimpleName());

    @Inject DAOBean dao;
    private MessagesVS messages = MessagesVS.getCurrentInstance();
    @Inject ConfigVS config;
    @Inject UserVSBean userVSBean;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject TransactionVSBean transactionVSBean;


    public GroupVS cancelGroup(GroupVS groupVS, MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        log.info("signer:" + signer.getNif());
        if(!groupVS.getRepresentative().getNif().equals(signer.getNif()) && !signatureBean.isAdmin(signer.getNif())) {
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
                !signatureBean.isAdmin(messageSMIME.getUserVS().getNif())) {
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
        GroupVSDto request = validateNewGroupRequest(messageSMIME.getSignedContent(GroupVSDto.class)) ;
        Query query = dao.getEM().createQuery("SELECT u FROM UserVS u WHERE u.name =:name")
                .setParameter("name", request.getName().trim());
        GroupVS groupVS = dao.getSingleResult(GroupVS.class, query);
        if(groupVS != null) {
            throw new ExceptionVS(messages.get("nameGroupRepeatedMsg", request.getName()));
        }
        currencyAccountBean.checkUserVSAccount(signer);
        groupVS = dao.persist(new GroupVS(request.getName().trim(), UserVS.State.ACTIVE, signer,
                request.getInfo(), request.getTags()));
        config.createIBAN(groupVS);
        String fromUser = config.getServerName();
        String toUser = signer.getNif();
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(fromUser, toUser,
                messageSMIME.getSMIME(), messages.get("newGroupVSReceiptSubject"));
        messageSMIME.setSMIME(receipt);
        log.info("saveGroup - GroupVS id:" + groupVS.getId());
        return groupVS;
    }

    private GroupVSDto validateNewGroupRequest(GroupVSDto groupVSDto) throws ValidationExceptionVS {
        groupVSDto.validateNewGroupRequest();
        if(groupVSDto.getTags() != null) {
            Set<TagVS> resultTagVSSet = new HashSet<>();
            for(TagVS tagVS: groupVSDto.getTags()) {
                TagVS tagVSDB = config.getTag(tagVS.getName());
                if(tagVSDB != null) resultTagVSSet.add(tagVSDB);
                else throw new ValidationExceptionVS(tagVS.getName() + " not found");
            }
            groupVSDto.setTags(resultTagVSSet);
        }
        return groupVSDto;
    }

    public SubscriptionVS subscribe(MessageSMIME messageSMIME) throws Exception {
        SubscriptionVS subscriptionVS = null;
        UserVS signer = messageSMIME.getUserVS();
        log.info("signer: " + signer.getNif());
        GroupVSDto request = messageSMIME.getSignedContent(GroupVSDto.class);
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

    @Transactional
    public GroupVSDto getGroupVSDto(GroupVS groupVS) throws Exception {
        GroupVSDto groupVSDto = GroupVSDto.DETAILS(groupVS, userVSBean.getUserVSDto(groupVS.getRepresentative(), false));
        Query query = dao.getEM().createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
                .setParameter("state", SubscriptionVS.State.ACTIVE);
        groupVSDto.setNumActiveUsers((long) query.getSingleResult());
        query = dao.getEM().createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
                .setParameter("state", SubscriptionVS.State.PENDING);
        groupVSDto.setNumPendingUsers((long) query.getSingleResult());
        return groupVSDto;
    }

}
