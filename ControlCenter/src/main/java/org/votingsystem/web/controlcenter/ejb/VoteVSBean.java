package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.dto.voting.VoteVSCancelerDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.model.voting.VoteVSCanceler;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
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
public class VoteVSBean {

    private static Logger log = Logger.getLogger(VoteVSBean.class.getName());

    @Inject private ConfigVS config;
    @PersistenceContext private EntityManager em;
    @Inject DAOBean dao;
    @Inject private CMSBean cmsBean;

    @Transactional
    public VoteVS validateVote(CMSDto CMSDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSMessage cmsMessage = CMSDto.getCmsMessage();
        EventVSElection eventVS = (EventVSElection) CMSDto.getEventVS();
        eventVS = em.merge(eventVS);
        VoteVS voteVS = cmsMessage.getCMS().getVoteVS();
        voteVS.setCmsMessage(cmsMessage);
        Query query = em.createQuery("select f from FieldEventVS f where f.eventVS =:eventVS and " +
                "f.accessControlFieldEventId =:fieldEventId").setParameter("eventVS", eventVS).setParameter(
                "fieldEventId", voteVS.getOptionSelected().getId());
        FieldEventVS optionSelected = DAOUtils.getSingleResult(FieldEventVS.class, query);
        if (optionSelected == null) throw new ValidationExceptionVS("ERROR - FieldEventVS not found - fieldEventId: " +
                voteVS.getOptionSelected().getId());
        CertificateVS certificateVS = CertificateVS.VOTE(voteVS.getHashCertVSBase64(),
                voteVS.getCMSMessage().getUserVS(), voteVS.getX509Certificate());
        String signedVoteDigest = cmsMessage.getCMS().getContentDigestStr();
        CMSSignedMessage validatedVote = cmsBean.addSignature(cmsMessage.getCMS());
        ResponseVS responseVS = HttpHelper.getInstance().sendData(validatedVote.toPEM(), ContentTypeVS.VOTE,
                eventVS.getAccessControlVS().getVoteServiceURL());
        if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(messages.get(
                "accessRequestVoteErrorMsg", responseVS.getMessage()));
        CMSSignedMessage cmsMessageResp = CMSSignedMessage.FROM_PEM(responseVS.getMessageBytes());
        if(!cmsMessageResp.isValidSignature() || !cmsMessageResp.getContentDigestStr().equals(signedVoteDigest)) {
            throw new ValidationExceptionVS("ERROR - expected signedVoteDigest: " + signedVoteDigest + " - found: " +
                    cmsMessageResp.getContentDigestStr());
        }
        cmsBean.validateVoteCerts(cmsMessageResp, eventVS);
        em.merge(cmsMessage.setCMS(cmsMessageResp).setType(TypeVS.ACCESS_CONTROL_VALIDATED_VOTE));
        voteVS.setState(VoteVS.State.OK).setOptionSelected(optionSelected);
        em.persist(certificateVS);
        voteVS.setCertificateVS(certificateVS);
        voteVS.setEventVS(eventVS);
        em.persist(voteVS);
        log.log(Level.INFO, "validateVote OK - voteVS id: " + voteVS.getId());
        return voteVS;
    }

    public VoteVSCanceler processCancel (CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS signer = cmsMessage.getUserVS();
        VoteVSCancelerDto request = cmsMessage.getSignedContent(VoteVSCancelerDto.class);
        request.validate();
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.hashCertVSBase64 =:hashCertVSBase64 and " +
                "c.state =:state").setParameter("hashCertVSBase64", request.getHashCertVSBase64())
                .setParameter("state", CertificateVS.State.OK);
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if (certificateVS == null) throw new ValidationExceptionVS(messages.get("certNotFoundErrorMsg"));
        query = dao.getEM().createQuery("select v from VoteVS v where v.certificateVS =:certificateVS " +
                "and v.state =:state").setParameter("certificateVS", certificateVS).setParameter("state", VoteVS.State.OK);
        VoteVS voteVS = dao.getSingleResult(VoteVS.class, query);
        if(voteVS == null) throw new ValidationExceptionVS("VoteVS not found");
        Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!voteVS.getEventVS().isActive(timeStampDate)) throw new ValidationExceptionVS(messages.get(
                "timestampDateErrorMsg", DateUtils.getDateStr(timeStampDate),
                DateUtils.getDateStr(voteVS.getEventVS().getDateBegin()),
                DateUtils.getDateStr(voteVS.getEventVS().getDateFinish())));
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        dao.merge(cmsMessage.setCMS(cmsSignedMessage));
        VoteVSCanceler voteCanceler = new VoteVSCanceler(cmsMessage, null, VoteVSCanceler.State.CANCELLATION_OK,
                request.getOriginHashAccessRequest(), request.getHashAccessRequestBase64(),
                request.getOriginHashCertVote(), request.getHashCertVSBase64(), voteVS);
        dao.persist(voteCanceler);
        dao.merge(voteVS.setState(VoteVS.State.CANCELED));
        dao.merge(certificateVS.setState(CertificateVS.State.CANCELED));
        return voteCanceler;
    }

}
