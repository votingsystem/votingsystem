package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.dto.voting.VoteVSCancelerDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.*;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
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
public class VoteVSBean {

    private static Logger log = Logger.getLogger(VoteVSBean.class.getName());

    private final Queue<CMSMessage> pendingVotes = new ConcurrentLinkedQueue<>();

    @Inject private ConfigVS config;
    @Inject private DAOBean dao;
    @Inject private CMSBean cmsBean;


    public VoteVS validateVote(CMSDto CMSDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSMessage cmsMessage = CMSDto.getCmsMessage();
        EventVSElection eventVS = (EventVSElection) CMSDto.getEventVS();
        eventVS = dao.merge(eventVS);
        VoteVS voteVSRequest = cmsMessage.getCMS().getVoteVS();
        CertificateVS voteVSCertificate = voteVSRequest.getCertificateVS();
        FieldEventVS optionSelected = eventVS.checkOptionId(voteVSRequest.getOptionSelected().getId());
        if (optionSelected == null) throw new ValidationExceptionVS(messages.get("voteOptionNotFoundErrorMsg",
                voteVSRequest.getOptionSelected().getId().toString()));
        CMSSignedMessage cmsMessageResp = cmsBean.addSignature(cmsMessage.getCMS());
        cmsMessage.setType(TypeVS.ACCESS_CONTROL_VALIDATED_VOTE).setCMS(cmsMessageResp);
        dao.merge(voteVSCertificate.setState(CertificateVS.State.USED));
        return dao.persist(new VoteVS(optionSelected, eventVS, VoteVS.State.OK, voteVSCertificate, cmsMessage));
    }

    public VoteVSCanceler processCancel (CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS signer = cmsMessage.getUserVS();
        CMSSignedMessage cmsSignedMessage = cmsMessage.getCMS();
        VoteVSCancelerDto request = cmsMessage.getSignedContent(VoteVSCancelerDto.class);
        request.validate();
        Query query = dao.getEM().createQuery("select a from AccessRequestVS a where a.hashAccessRequestBase64 =:hashAccessRequestBase64 and " +
                "a.state =:state").setParameter("hashAccessRequestBase64", request.getHashAccessRequestBase64())
                .setParameter("state", AccessRequestVS.State.OK);
        AccessRequestVS accessRequestVS = dao.getSingleResult(AccessRequestVS.class, query);
        if (accessRequestVS == null) throw new ValidationExceptionVS(messages.get(
                "voteCancellationAccessRequestNotFoundError"));
        query = dao.getEM().createQuery("select c from CertificateVS c where c.hashCertVSBase64 =:hashCertVSBase64 and " +
                "c.state =:state").setParameter("hashCertVSBase64", request.getHashCertVSBase64())
                .setParameter("state", CertificateVS.State.USED);
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if (certificateVS == null) throw new ValidationExceptionVS(messages.get("certificateVSNotFoundMsg"));
        query = dao.getEM().createQuery("select v from VoteVS v where v.certificateVS =:certificateVS " +
                "and v.state =:state").setParameter("certificateVS", certificateVS).setParameter("state", VoteVS.State.OK);
        VoteVS voteVS = dao.getSingleResult(VoteVS.class, query);
        if(voteVS == null) throw new ValidationExceptionVS("VoteVS not found");
        Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!voteVS.getEventVS().isActive(timeStampDate)) throw new ValidationExceptionVS(messages.get(
                "timestampDateErrorMsg", DateUtils.getDateStr(timeStampDate),
                DateUtils.getDateStr(voteVS.getEventVS().getDateBegin()),
                DateUtils.getDateStr(voteVS.getEventVS().getDateFinish())));
        CMSSignedMessage cmsSignedMessageReq = cmsBean.addSignature(cmsSignedMessage);
        String cancelerServiceURL = voteVS.getEventVS().getControlCenterVS().getVoteVSCancelerURL();
        ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(cmsSignedMessageReq.toPEM(),
                ContentTypeVS.JSON_SIGNED, cancelerServiceURL);
        if (ResponseVS.SC_OK == responseVSControlCenter.getStatusCode()) {
            CMSSignedMessage cmsSignedMessageResp = new CMSSignedMessage(responseVSControlCenter.getMessageBytes());
            if(!cmsSignedMessage.getContentDigestStr().equals(cmsSignedMessageResp.getContentDigestStr()))
                    throw new ValidationExceptionVS("cmsContentMismatchError");
            cmsBean.validateSignersCerts(cmsSignedMessageResp);
            dao.merge(cmsMessage.setType(TypeVS.CANCEL_VOTE).setCMS(cmsSignedMessageResp));
        } else {
            cmsMessage.setType(TypeVS.ERROR).setReason(responseVSControlCenter.getMessage());
            dao.merge(cmsMessage);
            throw new ValidationExceptionVS(responseVSControlCenter.getMessage());
        }
        VoteVSCanceler voteCanceler = new VoteVSCanceler(cmsMessage, accessRequestVS, VoteVSCanceler.State.CANCELLATION_OK,
                request.getOriginHashAccessRequest(), request.getHashAccessRequestBase64(),
                request.getOriginHashCertVote(), request.getHashCertVSBase64(), voteVS);
        dao.persist(voteCanceler);
        dao.merge(voteVS.setState(VoteVS.State.CANCELED));
        dao.merge(accessRequestVS.setState(AccessRequestVS.State.CANCELED));
        dao.merge(certificateVS.setState(CertificateVS.State.CANCELED));
        return voteCanceler;
    }

}