package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.dto.voting.VoteCancelerDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.*;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class VoteBean {

    private static Logger log = Logger.getLogger(VoteBean.class.getName());

    private final Queue<CMSMessage> pendingVotes = new ConcurrentLinkedQueue<>();

    @Inject private ConfigVS config;
    @Inject private DAOBean dao;
    @Inject private CMSBean cmsBean;


    public Vote validateVote(CMSDto CMSDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSMessage cmsMessage = CMSDto.getCmsMessage();
        EventElection eventVS = (EventElection) CMSDto.getEventVS();
        eventVS = dao.merge(eventVS);
        Vote voteRequest = cmsMessage.getCMS().getVote();
        Certificate voteCertificate = voteRequest.getCertificate();
        FieldEvent optionSelected = eventVS.checkOptionId(voteRequest.getOptionSelected().getId());
        if (optionSelected == null) throw new ValidationException(messages.get("voteOptionNotFoundErrorMsg",
                voteRequest.getOptionSelected().getId().toString()));
        CMSSignedMessage cmsMessageResp = cmsBean.addSignature(cmsMessage.getCMS());
        cmsMessage.setType(TypeVS.ACCESS_CONTROL_VALIDATED_VOTE).setCMS(cmsMessageResp);
        dao.merge(voteCertificate.setState(Certificate.State.USED));
        return dao.persist(new Vote(optionSelected, eventVS, Vote.State.OK, voteCertificate, cmsMessage));
    }

    public VoteCanceler processCancel (CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User signer = cmsMessage.getUser();
        CMSSignedMessage cmsSignedMessage = cmsMessage.getCMS();
        VoteCancelerDto request = cmsMessage.getSignedContent(VoteCancelerDto.class);
        request.validate();
        Query query = dao.getEM().createQuery("select a from AccessRequest a where a.hashAccessRequestBase64 =:hashAccessRequestBase64 and " +
                "a.state =:state").setParameter("hashAccessRequestBase64", request.getHashAccessRequestBase64())
                .setParameter("state", AccessRequest.State.OK);
        AccessRequest accessRequest = dao.getSingleResult(AccessRequest.class, query);
        if (accessRequest == null) throw new ValidationException(messages.get(
                "voteCancellationAccessRequestNotFoundError"));
        query = dao.getEM().createQuery("select c from Certificate c where c.hashCertVSBase64 =:hashCertVSBase64 and " +
                "c.state =:state").setParameter("hashCertVSBase64", request.getHashCertVSBase64())
                .setParameter("state", Certificate.State.USED);
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if (certificate == null) throw new ValidationException(messages.get("certificateNotFoundMsg"));
        query = dao.getEM().createQuery("select v from Vote v where v.certificate =:certificate " +
                "and v.state =:state").setParameter("certificate", certificate).setParameter("state", Vote.State.OK);
        Vote vote = dao.getSingleResult(Vote.class, query);
        if(vote == null) throw new ValidationException("Vote not found");
        Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!vote.getEventVS().isActive(timeStampDate)) throw new ValidationException(messages.get(
                "timestampDateErrorMsg", DateUtils.getDateStr(timeStampDate),
                DateUtils.getDateStr(vote.getEventVS().getDateBegin()),
                DateUtils.getDateStr(vote.getEventVS().getDateFinish())));
        CMSSignedMessage cmsSignedMessageReq = cmsBean.addSignature(cmsSignedMessage);
        String cancelerServiceURL = vote.getEventVS().getControlCenter().getVoteCancelerURL();
        ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(cmsSignedMessageReq.toPEM(),
                ContentType.JSON_SIGNED, cancelerServiceURL);
        if (ResponseVS.SC_OK == responseVSControlCenter.getStatusCode()) {
            CMSSignedMessage cmsSignedMessageResp = new CMSSignedMessage(responseVSControlCenter.getMessageBytes());
            if(!cmsSignedMessage.getContentDigestStr().equals(cmsSignedMessageResp.getContentDigestStr()))
                    throw new ValidationException("cmsContentMismatchError");
            cmsBean.validateSignersCerts(cmsSignedMessageResp);
            dao.merge(cmsMessage.setType(TypeVS.CANCEL_VOTE).setCMS(cmsSignedMessageResp));
        } else {
            cmsMessage.setType(TypeVS.ERROR).setReason(responseVSControlCenter.getMessage());
            dao.merge(cmsMessage);
            throw new ValidationException(responseVSControlCenter.getMessage());
        }
        VoteCanceler voteCanceler = new VoteCanceler(cmsMessage, accessRequest, VoteCanceler.State.CANCELLATION_OK,
                request.getOriginHashAccessRequest(), request.getHashAccessRequestBase64(),
                request.getOriginHashCertVote(), request.getHashCertVSBase64(), vote);
        dao.persist(voteCanceler);
        dao.merge(vote.setState(Vote.State.CANCELED));
        dao.merge(accessRequest.setState(AccessRequest.State.CANCELED));
        dao.merge(certificate.setState(Certificate.State.CANCELED));
        return voteCanceler;
    }

}