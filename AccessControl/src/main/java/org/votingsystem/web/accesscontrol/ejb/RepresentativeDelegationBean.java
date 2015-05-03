package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.AnonymousDelegationCertExtensionDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AnonymousDelegation;
import org.votingsystem.model.voting.RepresentationDocument;
import org.votingsystem.model.voting.RepresentativeDocument;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class RepresentativeDelegationBean {

    private static final Logger log = Logger.getLogger(RepresentativeDelegationBean.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject CSRBean csrBean;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    public RepresentationDocument saveDelegation(MessageSMIME messageSMIME) throws Exception {
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        UserVS userVS = messageSMIME.getUserVS();
        checkUserDelegationStatus(userVS);
        RepresentativeDelegationDto request = messageSMIME.getSignedContent(RepresentativeDelegationDto.class);
        if(TypeVS.REPRESENTATIVE_SELECTION != request.getOperation()) throw new ValidationExceptionVS(
                format("ERROR - operation missmatch - expected: {0} - found: {1}",
                        TypeVS.REPRESENTATIVE_SELECTION, request.getOperation()));
        request.getRepresentative().setNIF(NifUtils.validate(request.getRepresentative().getNIF()));
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                .setParameter("nif", request.getRepresentative().getNIF()).setParameter("type", UserVS.Type.REPRESENTATIVE);
        UserVS representative = dao.getSingleResult(UserVS.class, query);
        if(representative == null)  throw new ValidationExceptionVS(
                "ERROR - representativeNifErrorMsg - representativeNif: " + request.getRepresentative().getNIF());
        cancelPublicDelegation(messageSMIME);
        userVS.setRepresentative(representative);
        RepresentationDocument representationDocument = dao.persist(new RepresentationDocument(messageSMIME,
                userVS, representative, RepresentationDocument.State.OK));
        dao.merge(userVS);
        String toUser = userVS.getNif();
        String subject = messages.get("representativeSelectValidationSubject");
        messageSMIME.setSMIME(signatureBean.getSMIMEMultiSigned(toUser, smimeMessage, subject));
        dao.merge(messageSMIME);
        log.info(format("user id: {0} - representationDocument id: {1}", userVS.getNif(), representationDocument.getId()));
        return representationDocument;
    }


    private void cancelPublicDelegation(MessageSMIME messageSMIME) {
        Query query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:userVS and " +
                "r.state =:state").setParameter("userVS", messageSMIME.getUserVS())
                .setParameter("state", RepresentationDocument.State.OK);
        RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
        if(representationDocument != null) {
            representationDocument.setDateCanceled(messageSMIME.getUserVS().getTimeStampToken().getTimeStampInfo().getGenTime());
            representationDocument.setState(RepresentationDocument.State.CANCELED).setCancellationSMIME(
                    messageSMIME);
            dao.merge(representationDocument);
            log.info("cancelPublicDelegation - cancelled representationDocument from user id: " + messageSMIME.getUserVS().getId());
        }
    }

    public X509Certificate validateAnonymousRequest(MessageSMIME messageSMIME, byte[] csrRequest) throws Exception {
        RepresentativeDelegationDto request = messageSMIME.getSignedContent(RepresentativeDelegationDto.class);
        if(TypeVS.ANONYMOUS_SELECTION_CERT_REQUEST != request.getOperation()) throw new ValidationExceptionVS(
                "expected operation 'ANONYMOUS_SELECTION_CERT_REQUEST' but found '" + request.getOperation() + "'");
        UserVS userVS = messageSMIME.getUserVS();
        checkUserDelegationStatus(userVS);
        cancelPublicDelegation(messageSMIME);
        SMIMEMessage smimeMessageResp = signatureBean.getSMIMEMultiSigned(userVS.getNif(), messageSMIME.getSMIME(), null);
        messageSMIME.setType(request.getOperation()).setSMIME(smimeMessageResp);
        X509Certificate anonymousCert = csrBean.signAnonymousDelegationCert(csrRequest);
        dao.merge(userVS.setRepresentative(null));
        AnonymousDelegation anonymousDelegation = new AnonymousDelegation(AnonymousDelegation.Status.OK, messageSMIME,
                userVS, request.getDateFrom(), request.getDateTo());
        anonymousDelegation.setHashAnonymousDelegation(request.getHashAnonymousDelegation());
        dao.persist(anonymousDelegation);
        return anonymousCert;
    }

    public RepresentationDocument saveAnonymousDelegation(MessageSMIME messageSMIME) throws Exception {
        X509Certificate anonymousX509Cert =  messageSMIME.getAnonymousSigner().getCertificate();
        AnonymousDelegationCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                AnonymousDelegationCertExtensionDto.class, anonymousX509Cert,
                ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID);
        Query query = dao.getEM().createQuery("select c from  CertificateVS c where c.serialNumber=:serialNumber " +
                "and c.type =:type and c.state =:state and c.hashCertVSBase64 =:hashCertVS")
                .setParameter("serialNumber", anonymousX509Cert.getSerialNumber().longValue())
                .setParameter("type", CertificateVS.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION)
                .setParameter("state", CertificateVS.State.OK)
                .setParameter("hashCertVS", certExtensionDto.getHashCertVS());
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS == null) throw new ValidationExceptionVS(messages.get("certificateVSUnknownErrorMsg"));
        RepresentativeDelegationDto request = messageSMIME.getSignedContent(RepresentativeDelegationDto.class);
        if(request.getRepresentative() == null) throw new ValidationExceptionVS("missing param 'representative'");
        request.getRepresentative().setNIF(NifUtils.validate(request.getRepresentative().getNIF()));
        query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                .setParameter("nif", request.getRepresentative().getNIF()).setParameter("type", UserVS.Type.REPRESENTATIVE);
        UserVS representative = dao.getSingleResult(UserVS.class, query);
        if(representative == null) throw new ValidationExceptionVS(
                "ERROR - representativeNifErrorMsg - nif: " + request.getRepresentative().getNIF());
        if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION != request.getOperation()) throw new ValidationExceptionVS(
                "expected operation 'ANONYMOUS_REPRESENTATIVE_SELECTION' but found '" + request.getOperation() + "'");

        if(request.getWeeksOperationActive() == null) throw new ValidationExceptionVS("missing param 'weeksOperationActive'");
        if(request.getDateFrom() == null) throw new ValidationExceptionVS("missing param 'dateFrom'");
        if(request.getDateTo() == null) throw new ValidationExceptionVS("missing param 'dateTo'");
        if(request.getDateFrom().after(request.getDateTo())) throw new ValidationExceptionVS(
                format("dateFrom '{0}' after '{1}'", request.getDateFrom(), request.getDateTo()));
        Date dateFromCheck = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
        if(request.getDateFrom().compareTo(dateFromCheck) != 0) throw new ValidationExceptionVS(
                format("dateFrom expected '{0}' received '{1}'", dateFromCheck, request.getDateFrom()));
        Date dateToCheck = DateUtils.addDays(dateFromCheck, request.getWeeksOperationActive() * 7).getTime();
        if(request.getDateTo().compareTo(dateToCheck) != 0) throw new ValidationExceptionVS(
                format("dateTo expected '{0}' received '{1}'", dateToCheck, request.getDateTo()));

        String toUser = certificateVS.getHashCertVSBase64();
        String subject = messages.get("representativeSelectValidationSubject");
        SMIMEMessage smimeMessage = signatureBean.getSMIMEMultiSigned(toUser, messageSMIME.getSMIME(), subject);
        messageSMIME.setSMIME(smimeMessage);
        dao.merge(messageSMIME);
        dao.merge(certificateVS.setState(CertificateVS.State.USED).setMessageSMIME(messageSMIME));
        RepresentationDocument representationDocument = new RepresentationDocument(messageSMIME, null,
                representative, RepresentationDocument.State.OK);
        dao.persist(representationDocument);
        return representationDocument;
    }

    @Transactional
    public AnonymousDelegation cancelAnonymousDelegation(
            MessageSMIME messageSMIME, MessageSMIME anonymousMessageSMIME) throws Exception {
        cancelAnonymousRepresentationDocument(anonymousMessageSMIME);
        UserVS userVS = messageSMIME.getUserVS();
        RepresentativeDelegationDto request = messageSMIME.getSignedContent(RepresentativeDelegationDto.class);
        if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION != request.getOperation()) throw new ValidationExceptionVS(
                "expected operation 'ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION' found '" + request.getOperation() + "'");
        if(request.getHashAnonymousDelegation() == null)
            throw new ValidationExceptionVS("missing param 'hashAnonymousDelegation'");
        if(request.getOriginHashAnonymousDelegation() == null)
            throw new ValidationExceptionVS("missing param 'originHashAnonymousDelegation'");
        AnonymousDelegation anonymousDelegation = getAnonymousDelegation(userVS);
        if(anonymousDelegation == null)  throw new ValidationExceptionVS(
                "ERROR - userWithoutAnonymousDelegationErrorMsg - userVS nif: " + userVS.getNif());
        if(!anonymousDelegation.getHashAnonymousDelegation().equals(request.getHashAnonymousDelegation()))
            throw new ValidationExceptionVS("ERROR - cancelation hash doesn't match active one");
        String hashAnonymousDelegation = CMSUtils.getHashBase64(request.getOriginHashAnonymousDelegation(),
                ContextVS.VOTING_DATA_DIGEST);
        if(!hashAnonymousDelegation.equals(anonymousDelegation.getHashAnonymousDelegation()))
            throw new ValidationExceptionVS("ERROR - AnonymousDelegation hash error - calculated hash doesn't match active one");
        SMIMEMessage smimeMessageResp = signatureBean.getSMIMEMultiSigned(userVS.getNif(), messageSMIME.getSMIME(), null);
        dao.merge(messageSMIME.setSMIME(smimeMessageResp));
        anonymousDelegation.setOriginHashAnonymousDelegation(request.getOriginHashAnonymousDelegation()).setDateCancelled(new Date());
        dao.merge(anonymousDelegation.setStatus(AnonymousDelegation.Status.CANCELED).setCancellationSMIME(messageSMIME));
        log.info("cancelAnonymousDelegation -  AnonymousDelegation id: " + anonymousDelegation.getId());
        return anonymousDelegation;
    }

    private RepresentationDocument cancelAnonymousRepresentationDocument(MessageSMIME messageSMIME) throws Exception {
        RepresentativeDelegationDto request = messageSMIME.getSignedContent(RepresentativeDelegationDto.class);
        if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION != request.getOperation()) throw new ValidationExceptionVS(
                "expected operation 'ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION' found '" + request.getOperation() + "'");
        if(request.getHashCertVSBase64() == null) throw new ValidationExceptionVS("missing param 'hashCertVSBase64'");
        if(request.getOriginHashCertVS() == null) throw new ValidationExceptionVS("missing param 'originHashCertVSBase64'");
        String hashCertVSBase64 = CMSUtils.getHashBase64(request.getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST);
        if(!request.getHashCertVSBase64().equals(hashCertVSBase64)) {
            throw new ValidationExceptionVS("calculated 'hashCertVSBase64' doesn't match request one");
        }
        X509Certificate anonymousX509Cert =  messageSMIME.getAnonymousSigner().getCertificate();
        AnonymousDelegationCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                AnonymousDelegationCertExtensionDto.class, anonymousX509Cert, ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID);
        Query query = dao.getEM().createQuery("select c from  CertificateVS c where c.serialNumber=:serialNumber " +
                "and c.type =:type and c.state =:state and c.hashCertVSBase64 =:hashCertVS")
                .setParameter("serialNumber", anonymousX509Cert.getSerialNumber().longValue())
                .setParameter("type", CertificateVS.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION)
                .setParameter("state", CertificateVS.State.USED)
                .setParameter("hashCertVS", certExtensionDto.getHashCertVS());
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS == null) throw new ValidationExceptionVS(messages.get("certificateVSUnknownErrorMsg"));
        query = dao.getEM().createQuery("select r from RepresentationDocument r where r.activationSMIME =:messageSMIME")
                .setParameter("messageSMIME", certificateVS.getMessageSMIME());
        RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
        if(representationDocument == null) throw new ValidationExceptionVS(
                "ERROR - RepresentationDocument for request not found");
        SMIMEMessage smimeMessageResp = signatureBean.getSMIMEMultiSigned(certificateVS.getHashCertVSBase64(),
                messageSMIME.getSMIME(), null);
        dao.merge(messageSMIME.setSMIME(smimeMessageResp));
        dao.merge(representationDocument.setState(RepresentationDocument.State.CANCELED).setCancellationSMIME(messageSMIME));
        return representationDocument;
    }

    public void cancelRepresentationDocument(MessageSMIME messageSMIME) {
        UserVS userVS = messageSMIME.getUserVS();
        Query query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:userVS " +
                "and r .state =:state").setParameter("userVS", userVS).setParameter("state", RepresentationDocument.State.OK);
        RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
        if(representationDocument != null) {
            representationDocument.setState(RepresentationDocument.State.CANCELED).setCancellationSMIME(messageSMIME)
                    .setDateCanceled(userVS.getTimeStampToken().getTimeStampInfo().getGenTime());
            dao.merge(representationDocument);
            log.info(format("cancelRepresentationDocument - user '{0}' " +
                    " - representationDocument {1}", userVS.getNif(), representationDocument.getId()));
        } else log.info("cancelRepresentationDocument - user without representative - user id: " + userVS.getId());
    }

    private void checkUserDelegationStatus(UserVS userVS) throws ValidationExceptionVS, ExceptionVS {
        if(UserVS.Type.REPRESENTATIVE == userVS.getType()) throw new ValidationExceptionVS(
                "ERROR - user is representative: " + userVS.getNif());
        AnonymousDelegation anonymousDelegation = getAnonymousDelegation(userVS);
        if (anonymousDelegation != null) {
            String delegationURL = format("{0}/messageSMIME/id/{1}", config.getRestURL(),
                    anonymousDelegation.getDelegationSMIME().getId());
            throw new ExceptionVS(MessageDto.REQUEST_REPEATED(messages.get("userWithPreviousDelegationErrorMsg", userVS.getNif(),
                    DateUtils.getDateStr(anonymousDelegation.getDateTo())), delegationURL));
        }
    }

    public AnonymousDelegation getAnonymousDelegation(UserVS userVS) {
        Query query = dao.getEM().createQuery("select a from AnonymousDelegation a where a.userVS =:userVS and " +
                "a.status =:status").setParameter("userVS", userVS).setParameter("status", AnonymousDelegation.Status.OK);
        AnonymousDelegation anonymousDelegation = dao.getSingleResult(AnonymousDelegation.class, query);
        if(anonymousDelegation != null && new Date().after(anonymousDelegation.getDateTo())) {
            dao.merge(anonymousDelegation.setStatus(AnonymousDelegation.Status.FINISHED));
            return null;
        } else return anonymousDelegation;
    }

    public UserVSDto getRepresentativeDto(UserVS representative) {
        Query query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS and " +
                "r.state =:state").setParameter("userVS", representative).setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        query = dao.getEM().createQuery("select count(r) from RepresentationDocument r where " +
                "r.representative =:representative and r.state =:state").setParameter("representative", representative)
                .setParameter("state", RepresentationDocument.State.OK);
        long numRepresentations = (long) query.getSingleResult() + 1;//plus the representative itself
        return UserVSDto.REPRESENTATIVE(representative, representativeDocument.getActivationSMIME().getId(),
                numRepresentations, config.getRestURL());
    }

}