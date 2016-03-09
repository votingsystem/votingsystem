package org.votingsystem.web.currency.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.util.ConfigVS;
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

    private static Logger log = Logger.getLogger(GroupVSBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject UserVSBean userVSBean;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject CMSBean cmsBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject TransactionVSBean transactionVSBean;


    public GroupVS cancelGroup(GroupVS groupVS, CMSMessage cmsMessage) throws Exception {
        UserVS signer = cmsMessage.getUserVS();
        log.info("signer:" + signer.getNif());
        if(!groupVS.getRepresentative().getNif().equals(signer.getNif()) && !cmsBean.isAdmin(signer.getNif())) {
            throw new ExceptionVS("operation: " +  TypeVS.CURRENCY_GROUP_CANCEL.toString() +
                    " - userWithoutGroupPrivilegesErrorMsg - user: " + signer.getNif() + " - group: " + groupVS.getName());
        }
        GroupVSDto request = cmsMessage.getSignedContent(GroupVSDto.class);
        request.validateCancelRequest();
        dao.merge(groupVS.setState(UserVS.State.CANCELED));
        return groupVS;
    }

    public GroupVS editGroup(GroupVS groupVS, CMSMessage cmsMessage) throws Exception {
        UserVS signer = cmsMessage.getUserVS();
        log.info("signer:" + signer.getNif());
        if(!groupVS.getRepresentative().getNif().equals(cmsMessage.getUserVS().getNif()) &&
                !cmsBean.isAdmin(cmsMessage.getUserVS().getNif())) {
            throw new ExceptionVS("operation: " +  TypeVS.CURRENCY_GROUP_EDIT.toString() +
                    " - userWithoutGroupPrivilegesErrorMsg - user: " + signer.getNif() + " - group: " + groupVS.getName());
        }
        GroupVSDto request = cmsMessage.getSignedContent(GroupVSDto.class);
        request.validateEditRequest();
        if(request.getId().longValue() != groupVS.getId().longValue()) {
            throw new ExceptionVS("group id error - expected: " + groupVS.getId() + " - found: " + request.getId());
        }
        dao.merge(groupVS.setDescription(request.getDescription()));
        return groupVS;
    }

    public GroupVS saveGroup(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS signer = cmsMessage.getUserVS();
        GroupVSDto request = cmsMessage.getSignedContent(GroupVSDto.class);
        if (TypeVS.CURRENCY_GROUP_EDIT == request.getOperation()) {
            GroupVS groupVS = dao.find(GroupVS.class, request.getId());
            if(groupVS == null) throw new ValidationExceptionVS("ERROR - GroupVS not found - id: " + request.getId());
            return editGroup(groupVS, cmsMessage);
        } else if(TypeVS.CURRENCY_GROUP_NEW == request.getOperation()) {
            validateNewGroupRequest(request);
        } else throw new ValidationExceptionVS("ERROR - operation expected CURRENCY_GROUP_EDIT, CURRENCY_GROUP_NEW - " +
                "found: " + request.getOperation());
        Query query = dao.getEM().createQuery("SELECT u FROM UserVS u WHERE u.name =:name")
                .setParameter("name", request.getName().trim());
        GroupVS groupVS = dao.getSingleResult(GroupVS.class, query);
        if(groupVS != null) {
            throw new ExceptionVS(messages.get("nameGroupRepeatedMsg", request.getName()));
        }
        currencyAccountBean.checkUserVSAccount(signer);
        groupVS = dao.persist(new GroupVS(request.getName().trim(), UserVS.State.ACTIVE, signer,
                request.getDescription(), request.getTags()));
        config.createIBAN(groupVS);
        CMSSignedMessage receipt = cmsBean.addSignature(cmsMessage.getCMS());
        cmsMessage.setCMS(receipt);
        log.info("saveGroup - GroupVS id:" + groupVS.getId());
        return groupVS;
    }

    private GroupVSDto validateNewGroupRequest(GroupVSDto groupVSDto) throws ValidationExceptionVS {
        groupVSDto.validateNewGroupRequest();
        if(groupVSDto.getTags() != null) {
            Set<TagVS> resultTagVSSet = new HashSet<>();
            for(TagVS tagVS: groupVSDto.getTags()) {
                resultTagVSSet.add(config.getTag(tagVS.getName()));
            }
            groupVSDto.setTags(resultTagVSSet);
        }
        return groupVSDto;
    }

    public SubscriptionVS subscribe(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        SubscriptionVS subscriptionVS = null;
        UserVS signer = cmsMessage.getUserVS();
        log.info("signer: " + signer.getNif());
        GroupVSDto request = cmsMessage.getSignedContent(GroupVSDto.class);
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
        subscriptionVS = dao.persist(new SubscriptionVS(signer, groupVS, SubscriptionVS.State.PENDING, cmsMessage));
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
