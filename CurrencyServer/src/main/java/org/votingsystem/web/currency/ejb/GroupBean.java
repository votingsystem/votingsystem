package org.votingsystem.web.currency.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.GroupDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Group;
import org.votingsystem.model.currency.Subscription;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
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
public class GroupBean {

    private static Logger log = Logger.getLogger(GroupBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject
    UserBean userBean;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject CMSBean cmsBean;
    @Inject
    SubscriptionBean subscriptionBean;
    @Inject
    TransactionBean transactionBean;


    public Group cancelGroup(Group group, CMSMessage cmsMessage) throws Exception {
        User signer = cmsMessage.getUser();
        log.info("signer:" + signer.getNif());
        if(!group.getRepresentative().getNif().equals(signer.getNif()) && !cmsBean.isAdmin(signer.getNif())) {
            throw new ExceptionVS("operation: " +  TypeVS.CURRENCY_GROUP_CANCEL.toString() +
                    " - userWithoutGroupPrivilegesErrorMsg - user: " + signer.getNif() + " - group: " + group.getName());
        }
        GroupDto request = cmsMessage.getSignedContent(GroupDto.class);
        request.validateCancelRequest();
        dao.merge(group.setState(User.State.CANCELED));
        return group;
    }

    public Group editGroup(Group group, CMSMessage cmsMessage) throws Exception {
        User signer = cmsMessage.getUser();
        log.info("signer:" + signer.getNif());
        if(!group.getRepresentative().getNif().equals(cmsMessage.getUser().getNif()) &&
                !cmsBean.isAdmin(cmsMessage.getUser().getNif())) {
            throw new ExceptionVS("operation: " +  TypeVS.CURRENCY_GROUP_EDIT.toString() +
                    " - userWithoutGroupPrivilegesErrorMsg - user: " + signer.getNif() + " - group: " + group.getName());
        }
        GroupDto request = cmsMessage.getSignedContent(GroupDto.class);
        request.validateEditRequest();
        if(request.getId().longValue() != group.getId().longValue()) {
            throw new ExceptionVS("group id error - expected: " + group.getId() + " - found: " + request.getId());
        }
        dao.merge(group.setDescription(request.getDescription()));
        return group;
    }

    public Group saveGroup(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User signer = cmsMessage.getUser();
        GroupDto request = cmsMessage.getSignedContent(GroupDto.class);
        if (TypeVS.CURRENCY_GROUP_EDIT == request.getOperation()) {
            Group group = dao.find(Group.class, request.getId());
            if(group == null) throw new ValidationException("ERROR - Group not found - id: " + request.getId());
            return editGroup(group, cmsMessage);
        } else if(TypeVS.CURRENCY_GROUP_NEW == request.getOperation()) {
            validateNewGroupRequest(request);
        } else throw new ValidationException("ERROR - operation expected CURRENCY_GROUP_EDIT, CURRENCY_GROUP_NEW - " +
                "found: " + request.getOperation());
        Query query = dao.getEM().createQuery("SELECT u FROM User u WHERE u.name =:name")
                .setParameter("name", request.getName().trim());
        Group group = dao.getSingleResult(Group.class, query);
        if(group != null) {
            throw new ExceptionVS(messages.get("nameGroupRepeatedMsg", request.getName()));
        }
        currencyAccountBean.checkUserAccount(signer);
        group = dao.persist(new Group(request.getName().trim(), User.State.ACTIVE, signer,
                request.getDescription(), request.getTags()));
        config.createIBAN(group);
        CMSSignedMessage receipt = cmsBean.addSignature(cmsMessage.getCMS());
        cmsMessage.setCMS(receipt);
        log.info("saveGroup - Group id:" + group.getId());
        return group;
    }

    private GroupDto validateNewGroupRequest(GroupDto groupDto) throws ValidationException {
        groupDto.validateNewGroupRequest();
        if(groupDto.getTags() != null) {
            Set<TagVS> resultTagVSSet = new HashSet<>();
            for(TagVS tagVS: groupDto.getTags()) {
                resultTagVSSet.add(config.getTag(tagVS.getName()));
            }
            groupDto.setTags(resultTagVSSet);
        }
        return groupDto;
    }

    public Subscription subscribe(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Subscription subscription = null;
        User signer = cmsMessage.getUser();
        log.info("signer: " + signer.getNif());
        GroupDto request = cmsMessage.getSignedContent(GroupDto.class);
        request.validateSubscriptionRequest();
        Group group = dao.find(Group.class, request.getId());
        if(group.getRepresentative().getNif().equals(signer.getNif())) {
            throw new ExceptionVS(messages.get("representativeSubscribedErrorMsg", group.getRepresentative().getNif(),
                    group.getName()));
        }
        Query query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndUser").setParameter("group", group)
                .setParameter("user", signer);
        subscription = dao.getSingleResult(Subscription.class, query);
        if(subscription != null) {
            throw new ExceptionVS(messages.get("userAlreadySubscribedErrorMsg", signer.getNif(), group.getName()));
        }
        subscription = dao.persist(new Subscription(signer, group, Subscription.State.PENDING, cmsMessage));
        return subscription;
    }

    @Transactional
    public GroupDto getGroupDto(Group group) throws Exception {
        GroupDto groupDto = GroupDto.DETAILS(group, userBean.getUserDto(group.getRepresentative(), false));
        Query query = dao.getEM().createNamedQuery("countSubscriptionByGroupAndState").setParameter("group", group)
                .setParameter("state", Subscription.State.ACTIVE);
        groupDto.setNumActiveUsers((long) query.getSingleResult());
        query = dao.getEM().createNamedQuery("countSubscriptionByGroupAndState").setParameter("group", group)
                .setParameter("state", Subscription.State.PENDING);
        groupDto.setNumPendingUsers((long) query.getSingleResult());
        return groupDto;
    }

}
