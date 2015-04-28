package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.dto.SMIMEDto;
import org.votingsystem.dto.voting.VoteVSCancelerDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.model.voting.VoteVSCanceler;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.MessagesBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.DAOUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class VoteVSBean {

    private static Logger log = Logger.getLogger(VoteVSBean.class.getSimpleName());

    @Inject private ConfigVS config;
    @PersistenceContext private EntityManager em;
    @Inject DAOBean dao;
    @Inject private SignatureBean signatureBean;
    @Inject MessagesBean messages;

    @Transactional
    public VoteVS validateVote(SMIMEDto smimeDto) throws Exception {
        MessageSMIME messageSMIME = smimeDto.getMessageSMIME();
        EventVSElection eventVS = (EventVSElection) smimeDto.getEventVS();
        eventVS = em.merge(eventVS);
        VoteVS voteVS = messageSMIME.getSMIME().getVoteVS();
        voteVS.setMessageSMIME(messageSMIME);
        Query query = em.createQuery("select f from FieldEventVS f where f.eventVS =:eventVS and " +
                "f.accessControlFieldEventId =:fieldEventId").setParameter("eventVS", eventVS).setParameter(
                "fieldEventId", voteVS.getOptionSelected().getId());
        FieldEventVS optionSelected = DAOUtils.getSingleResult(FieldEventVS.class, query);
        if (optionSelected == null) throw new ValidationExceptionVS("ERROR - FieldEventVS not found - fieldEventId: " +
                voteVS.getOptionSelected().getId());
        CertificateVS certificateVS = CertificateVS.VOTE(voteVS.getHashCertVSBase64(),
                voteVS.getMessageSMIME().getUserVS(), voteVS.getX509Certificate());
        String signedVoteDigest = messageSMIME.getSMIME().getContentDigestStr();
        String fromUser = config.getServerName();
        String toUser = eventVS.getAccessControlVS().getName();
        String subject = messages.get("voteValidatedByAccessControlMsg");
        SMIMEMessage validatedVote = signatureBean.getSMIMEMultiSigned(
                fromUser, toUser, messageSMIME.getSMIME(), subject);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(validatedVote.getBytes(), ContentTypeVS.VOTE,
                eventVS.getAccessControlVS().getVoteServiceURL());
        if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(messages.get(
                "accessRequestVoteErrorMsg", responseVS.getMessage()));
        SMIMEMessage smimeMessageResp = new SMIMEMessage(responseVS.getMessageBytes());
        if(!smimeMessageResp.isValidSignature() || !smimeMessageResp.getContentDigestStr().equals(signedVoteDigest)) {
            throw new ValidationExceptionVS("ERROR - expected signedVoteDigest: " + signedVoteDigest + " - found: " +
                    smimeMessageResp.getContentDigestStr());
        }
        signatureBean.validateVoteCerts(smimeMessageResp, eventVS);
        smimeMessageResp.setMessageID(format("{0}/messageSMIME/id/{1}", config.getRestURL(), messageSMIME.getId()));
        em.merge(messageSMIME.setSMIME(smimeMessageResp).setType(TypeVS.ACCESS_CONTROL_VALIDATED_VOTE));
        voteVS.setState(VoteVS.State.OK).setOptionSelected(optionSelected);
        em.persist(certificateVS);
        voteVS.setCertificateVS(certificateVS);
        voteVS.setEventVS(eventVS);
        em.persist(voteVS);
        log.log(Level.INFO, "validateVote OK - voteVS id: " + voteVS.getId());
        return voteVS;
    }

    public VoteVSCanceler processCancel (MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        VoteVSCancelerDto request = messageSMIME.getSignedContent(VoteVSCancelerDto.class);
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
        String fromUser = config.getServerName();
        String toUser = signer.getNif();
        String subject = messages.get("voteCancelationSubject");
        SMIMEMessage smimeMessage = signatureBean.getSMIMEMultiSigned(fromUser, toUser, messageSMIME.getSMIME(), subject);
        dao.merge(messageSMIME.setSMIME(smimeMessage));
        VoteVSCanceler voteCanceler = new VoteVSCanceler(messageSMIME, null, VoteVSCanceler.State.CANCELLATION_OK,
                request.getOriginHashAccessRequest(), request.getHashAccessRequestBase64(),
                request.getOriginHashCertVote(), request.getHashCertVSBase64(), voteVS);
        dao.persist(voteCanceler);
        dao.merge(voteVS.setState(VoteVS.State.CANCELED));
        dao.merge(certificateVS.setState(CertificateVS.State.CANCELED));
        return voteCanceler;
    }

}
