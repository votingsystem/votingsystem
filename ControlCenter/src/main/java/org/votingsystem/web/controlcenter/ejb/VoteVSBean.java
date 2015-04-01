package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.text.MessageFormat.*;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class VoteVSBean {

    private static Logger log = Logger.getLogger(VoteVSBean.class.getSimpleName());

    @Inject private ConfigVS config;
    @Inject private DAOBean dao;
    @Inject private SignatureBean signatureBean;
    @Inject MessagesBean messages;

    public VoteVS validateVote(MessageSMIME messageSMIME) throws Exception {
        EventVSElection eventVS = (EventVSElection) messageSMIME.getEventVS();
        VoteVS voteVS = messageSMIME.getSMIME().getVoteVS();
        Query query = dao.getEM().createQuery("select f from FieldEventVS f where f.eventVS =:eventVS and " +
                "f.accessControlFieldEventId =:fieldEventId").setParameter("eventVS", eventVS).setParameter(
                "fieldEventId", voteVS.getOptionSelected().getId());
        FieldEventVS optionSelected = dao.getSingleResult(FieldEventVS.class, query);
        if (optionSelected == null) throw new ValidationExceptionVS("ERROR - FieldEventVS not found - fieldEventId: " +
                voteVS.getOptionSelected().getId());
        CertificateVS certificateVS = CertificateVS.VOTE(voteVS);
        String signedVoteDigest = messageSMIME.getSMIME().getContentDigestStr();
        String fromUser = config.getServerName();
        String toUser = eventVS.getAccessControlVS().getName();
        String subject = messages.get("voteValidatedByAccessControlMsg");
        SMIMEMessage smimeVoteValidation = signatureBean.getSMIMEMultiSigned(
                fromUser, toUser, messageSMIME.getSMIME(), subject);
        ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeVoteValidation.getBytes(), ContentTypeVS.VOTE,
                eventVS.getAccessControlVS().getVoteServiceURL());
        if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(messages.get(
                "accessRequestVoteErrorMsg", responseVS.getMessage()));
        SMIMEMessage smimeMessageResp = new SMIMEMessage(new ByteArrayInputStream(responseVS.getMessageBytes()));
        if(!smimeMessageResp.isValidSignature() || !smimeMessageResp.getContentDigestStr().equals(signedVoteDigest)) {
            throw new ValidationExceptionVS("ERROR - expected signedVoteDigest: " + signedVoteDigest + " - found: " +
                    smimeMessageResp.getContentDigestStr());
        }
        signatureBean.validateVoteCerts(smimeMessageResp, eventVS);
        smimeMessageResp.setMessageID(format("{0}/messageSMIME/id/{1}", config.getRestURL(), messageSMIME.getId()));
        dao.merge(messageSMIME.setSMIME(smimeMessageResp).setType(TypeVS.ACCESS_CONTROL_VALIDATED_VOTE));
        voteVS = dao.persist(new VoteVS(optionSelected, eventVS, VoteVS.State.OK, certificateVS, messageSMIME));
        log.log(Level.FINE, "validateVote OK - voteVS id: " + voteVS.getId());
        return voteVS;
    }

    public synchronized VoteVSCanceler processCancel (MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        VoteVSRequest request = messageSMIME.getSignedContent(VoteVSRequest.class);
        request.validateCancelationRequest();
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.hashCertVSBase64 =:hashCertVSBase64 and " +
                "c.state =:state").setParameter("hashCertVSBase64", request.hashCertVSBase64)
                .setParameter("state", CertificateVS.State.OK);
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if (certificateVS == null) throw new ValidationExceptionVS(messages.get("certNotFoundErrorMsg"));
        query = dao.getEM().createQuery("select v from VoteVS v where v.certificateVS =:certificateVS " +
                "and v.state =:state").setParameter("certificateVS", certificateVS).setParameter("state", VoteVS.State.OK);
        VoteVS voteVS = dao.getSingleResult(VoteVS.class, query);
        if(voteVS == null) throw new ValidationExceptionVS("VoteVS not found");
        Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!certificateVS.getEventVS().isActive(timeStampDate)) throw new ValidationExceptionVS(config.get(
                "timestampDateErrorMsg", DateUtils.getDateStr(timeStampDate),
                DateUtils.getDateStr(certificateVS.getEventVS().getDateBegin()),
                DateUtils.getDateStr(certificateVS.getEventVS().getDateFinish())));
        String fromUser = config.getServerName();
        String toUser = messageSMIME.getUserVS().getNif();
        String subject = messages.get("voteCancelationSubject");
        SMIMEMessage smimeMessage = signatureBean.getSMIMEMultiSigned(fromUser, toUser, messageSMIME.getSMIME(), subject);
        messageSMIME.setSMIME(smimeMessage);
        dao.merge(messageSMIME);
        VoteVSCanceler voteCanceler = dao.persist(new VoteVSCanceler(messageSMIME, null,
                VoteVSCanceler.State.CANCELLATION_OK, request.originHashAccessRequest, request.hashCertVSBase64,
                request.originHashCertVote, request.hashCertVSBase64, voteVS));
        dao.merge(voteVS.setState(VoteVS.State.CANCELED));
        dao.merge(certificateVS.setState(CertificateVS.State.CANCELED));
        return voteCanceler;
    }

    private class VoteVSRequest {
        String originHashCertVote, hashCertVSBase64, originHashAccessRequest, hashAccessRequestBase64;
        TypeVS operation;

        public VoteVSRequest(String signedContent) {}

        public void validateCancelationRequest() throws ValidationExceptionVS, NoSuchAlgorithmException {
            if(operation == null || TypeVS.CANCEL_VOTE != operation) throw new ValidationExceptionVS(
                    "ERROR - expected operation 'CANCEL_VOTE' - found: " + operation);
            if(originHashCertVote == null) throw new ValidationExceptionVS("ERROR - missing param 'originHashCertVote'");
            if(hashCertVSBase64 == null) throw new ValidationExceptionVS("ERROR - missing param 'hashCertVSBase64'");
            if(hashAccessRequestBase64 == null) throw new ValidationExceptionVS("ERROR - missing param 'hashAccessRequestBase64'");
            if(originHashAccessRequest == null) throw new ValidationExceptionVS("ERROR - missing param 'originHashAccessRequest'");
            if(originHashAccessRequest == null) throw new ValidationExceptionVS("ERROR - missing param 'originHashAccessRequest'");
            if(!hashAccessRequestBase64.equals(CMSUtils.getHashBase64(originHashAccessRequest,
                    ContextVS.VOTING_DATA_DIGEST))) throw new ValidationExceptionVS(messages.get(
                    "voteCancellationAccessRequestHashError"));
            if(!hashCertVSBase64.equals(CMSUtils.getHashBase64(originHashCertVote,
                    ContextVS.VOTING_DATA_DIGEST))) throw new ValidationExceptionVS(
                    messages.get("voteCancellationHashCertificateError"));
        }
    }
}
