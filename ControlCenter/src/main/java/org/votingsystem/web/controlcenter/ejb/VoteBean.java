package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.dto.voting.VoteCancelerDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.FieldEvent;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.model.voting.VoteCanceler;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.DAOUtils;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class VoteBean {

    private static Logger log = Logger.getLogger(VoteBean.class.getName());

    @Inject private ConfigVS config;
    @PersistenceContext private EntityManager em;
    @Inject DAOBean dao;
    @Inject private CMSBean cmsBean;

    @Transactional
    public Vote validateVote(CMSDto CMSDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSMessage cmsMessage = CMSDto.getCmsMessage();
        EventElection eventVS = (EventElection) CMSDto.getEventVS();
        eventVS = em.merge(eventVS);
        Vote vote = cmsMessage.getCMS().getVote();
        vote.setCmsMessage(cmsMessage);
        Query query = em.createQuery("select f from FieldEvent f where f.eventVS =:eventVS and " +
                "f.accessControlFieldEventId =:fieldEventId").setParameter("eventVS", eventVS).setParameter(
                "fieldEventId", vote.getOptionSelected().getId());
        FieldEvent optionSelected = DAOUtils.getSingleResult(FieldEvent.class, query);
        if (optionSelected == null) throw new ValidationExceptionVS("ERROR - FieldEvent not found - fieldEventId: " +
                vote.getOptionSelected().getId());
        CertificateVS certificateVS = CertificateVS.VOTE(vote.getHashCertVSBase64(),
                vote.getCMSMessage().getUser(), vote.getX509Certificate());
        String signedVoteDigest = cmsMessage.getCMS().getContentDigestStr();
        CMSSignedMessage validatedVote = cmsBean.addSignature(cmsMessage.getCMS());
        ResponseVS responseVS = HttpHelper.getInstance().sendData(validatedVote.toPEM(), ContentTypeVS.VOTE,
                eventVS.getAccessControl().getVoteServiceURL());
        if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(messages.get(
                "accessRequestVoteErrorMsg", responseVS.getMessage()));
        CMSSignedMessage cmsMessageResp = CMSSignedMessage.FROM_PEM(responseVS.getMessageBytes());
        if(cmsMessageResp.isValidSignature() == null || !cmsMessageResp.getContentDigestStr().equals(signedVoteDigest)) {
            throw new ValidationExceptionVS("ERROR - expected signedVoteDigest: " + signedVoteDigest + " - found: " +
                    cmsMessageResp.getContentDigestStr());
        }
        cmsBean.validateVoteCerts(cmsMessageResp, eventVS);
        em.merge(cmsMessage.setCMS(cmsMessageResp).setType(TypeVS.ACCESS_CONTROL_VALIDATED_VOTE));
        vote.setState(Vote.State.OK).setOptionSelected(optionSelected);
        em.persist(certificateVS);
        vote.setCertificateVS(certificateVS);
        vote.setEventVS(eventVS);
        em.persist(vote);
        log.log(Level.INFO, "validateVote OK - vote id: " + vote.getId());
        return vote;
    }

    public VoteCanceler processCancel (CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User signer = cmsMessage.getUser();
        VoteCancelerDto request = cmsMessage.getSignedContent(VoteCancelerDto.class);
        request.validate();
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.hashCertVSBase64 =:hashCertVSBase64 and " +
                "c.state =:state").setParameter("hashCertVSBase64", request.getHashCertVSBase64())
                .setParameter("state", CertificateVS.State.OK);
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if (certificateVS == null) throw new ValidationExceptionVS(messages.get("certNotFoundErrorMsg"));
        query = dao.getEM().createQuery("select v from Vote v where v.certificateVS =:certificateVS " +
                "and v.state =:state").setParameter("certificateVS", certificateVS).setParameter("state", Vote.State.OK);
        Vote vote = dao.getSingleResult(Vote.class, query);
        if(vote == null) throw new ValidationExceptionVS("Vote not found");
        Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!vote.getEventVS().isActive(timeStampDate)) throw new ValidationExceptionVS(messages.get(
                "timestampDateErrorMsg", DateUtils.getDateStr(timeStampDate),
                DateUtils.getDateStr(vote.getEventVS().getDateBegin()),
                DateUtils.getDateStr(vote.getEventVS().getDateFinish())));
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        dao.merge(cmsMessage.setCMS(cmsSignedMessage));
        VoteCanceler voteCanceler = new VoteCanceler(cmsMessage, null, VoteCanceler.State.CANCELLATION_OK,
                request.getOriginHashAccessRequest(), request.getHashAccessRequestBase64(),
                request.getOriginHashCertVote(), request.getHashCertVSBase64(), vote);
        dao.persist(voteCanceler);
        dao.merge(vote.setState(Vote.State.CANCELED));
        dao.merge(certificateVS.setState(CertificateVS.State.CANCELED));
        return voteCanceler;
    }

}
