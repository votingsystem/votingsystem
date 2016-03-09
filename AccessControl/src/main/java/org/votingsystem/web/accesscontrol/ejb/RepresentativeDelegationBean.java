package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.AnonymousDelegationCertExtensionDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AnonymousDelegation;
import org.votingsystem.model.voting.RepresentationDocument;
import org.votingsystem.model.voting.RepresentativeDocument;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
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

    private static final Logger log = Logger.getLogger(RepresentativeDelegationBean.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject CSRBean csrBean;

    public X509Certificate validateAnonymousRequest(CMSMessage cmsMessage, byte[] csrRequest) throws Exception {
        RepresentativeDelegationDto request = cmsMessage.getSignedContent(RepresentativeDelegationDto.class);
        if(TypeVS.ANONYMOUS_SELECTION_CERT_REQUEST != request.getOperation()) throw new ValidationExceptionVS(
                "expected operation 'ANONYMOUS_SELECTION_CERT_REQUEST' but found '" + request.getOperation() + "'");
        UserVS userVS = cmsMessage.getUserVS();
        checkUserDelegationStatus(userVS);
        CMSSignedMessage cmsMessageResp = cmsBean.addSignature(cmsMessage.getCMS());
        cmsMessage.setType(request.getOperation()).setCMS(cmsMessageResp);
        X509Certificate anonymousCert = csrBean.signAnonymousDelegationCert(csrRequest);
        dao.merge(userVS.setRepresentative(null));
        AnonymousDelegation anonymousDelegation = new AnonymousDelegation(AnonymousDelegation.Status.OK, cmsMessage,
                userVS, request.getDateFrom(), request.getDateTo());
        anonymousDelegation.setHashAnonymousDelegation(request.getHashAnonymousDelegation());
        dao.persist(anonymousDelegation);
        return anonymousCert;
    }

    public RepresentationDocument saveAnonymousDelegation(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        X509Certificate anonymousX509Cert =  cmsMessage.getAnonymousSigner().getCertificate();
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
        RepresentativeDelegationDto request = cmsMessage.getSignedContent(RepresentativeDelegationDto.class);
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
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        cmsMessage.setCMS(cmsSignedMessage);
        dao.merge(cmsMessage);
        dao.merge(certificateVS.setState(CertificateVS.State.USED).setCmsMessage(cmsMessage));
        RepresentationDocument representationDocument = new RepresentationDocument(cmsMessage, null,
                representative, RepresentationDocument.State.OK);
        dao.persist(representationDocument);
        return representationDocument;
    }

    @Transactional
    public AnonymousDelegation cancelAnonymousDelegation(
            CMSMessage cmsMessage, CMSMessage anonymousCMSMessage) throws Exception {
        cancelAnonymousRepresentationDocument(anonymousCMSMessage);
        UserVS userVS = cmsMessage.getUserVS();
        RepresentativeDelegationDto request = cmsMessage.getSignedContent(RepresentativeDelegationDto.class);
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
        String hashAnonymousDelegation = StringUtils.getHashBase64(request.getOriginHashAnonymousDelegation(),
                ContextVS.DATA_DIGEST_ALGORITHM);
        if(!hashAnonymousDelegation.equals(anonymousDelegation.getHashAnonymousDelegation()))
            throw new ValidationExceptionVS("ERROR - AnonymousDelegation hash error - calculated hash doesn't match active one");
        CMSSignedMessage cmsMessageResp = cmsBean.addSignature(cmsMessage.getCMS());
        dao.merge(cmsMessage.setCMS(cmsMessageResp));
        anonymousDelegation.setOriginHashAnonymousDelegation(request.getOriginHashAnonymousDelegation()).setDateCancelled(new Date());
        dao.merge(anonymousDelegation.setStatus(AnonymousDelegation.Status.CANCELED).setCancellationCMS(cmsMessage));
        log.info("cancelAnonymousDelegation -  AnonymousDelegation id: " + anonymousDelegation.getId());
        return anonymousDelegation;
    }

    private RepresentationDocument cancelAnonymousRepresentationDocument(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        RepresentativeDelegationDto request = cmsMessage.getSignedContent(RepresentativeDelegationDto.class);
        if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION != request.getOperation()) throw new ValidationExceptionVS(
                "expected operation 'ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION' found '" + request.getOperation() + "'");
        if(request.getHashCertVSBase64() == null) throw new ValidationExceptionVS("missing param 'hashCertVSBase64'");
        if(request.getOriginHashCertVS() == null) throw new ValidationExceptionVS("missing param 'originHashCertVSBase64'");
        String hashCertVSBase64 = StringUtils.getHashBase64(request.getOriginHashCertVS(), ContextVS.DATA_DIGEST_ALGORITHM);
        if(!request.getHashCertVSBase64().equals(hashCertVSBase64)) {
            throw new ValidationExceptionVS("calculated 'hashCertVSBase64' doesn't match request one");
        }
        X509Certificate anonymousX509Cert =  cmsMessage.getAnonymousSigner().getCertificate();
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
        query = dao.getEM().createQuery("select r from RepresentationDocument r where r.activationCMS =:cmsMessage")
                .setParameter("cmsMessage", certificateVS.getCmsMessage());
        RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
        if(representationDocument == null) throw new ValidationExceptionVS(
                "ERROR - RepresentationDocument for request not found");
        CMSSignedMessage cmsMessageResp = cmsBean.addSignature(cmsMessage.getCMS());
        dao.merge(cmsMessage.setCMS(cmsMessageResp));
        dao.merge(representationDocument.setState(RepresentationDocument.State.CANCELED).setCancellationCMS(cmsMessage));
        return representationDocument;
    }

    public void cancelRepresentationDocument(CMSMessage cmsMessage) {
        UserVS userVS = cmsMessage.getUserVS();
        Query query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:userVS " +
                "and r .state =:state").setParameter("userVS", userVS).setParameter("state", RepresentationDocument.State.OK);
        RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
        if(representationDocument != null) {
            representationDocument.setState(RepresentationDocument.State.CANCELED).setCancellationCMS(cmsMessage)
                    .setDateCanceled(userVS.getTimeStampToken().getTimeStampInfo().getGenTime());
            dao.merge(representationDocument);
            log.info(format("cancelRepresentationDocument - user '{0}' " +
                    " - representationDocument {1}", userVS.getNif(), representationDocument.getId()));
        } else log.info("cancelRepresentationDocument - user without representative - user id: " + userVS.getId());
    }

    private void checkUserDelegationStatus(UserVS userVS) throws ExceptionVS {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        if(UserVS.Type.REPRESENTATIVE == userVS.getType()) throw new ValidationExceptionVS(
                messages.get("userIsRepresentativeErrorMsg", userVS.getNif()));
        AnonymousDelegation anonymousDelegation = getAnonymousDelegation(userVS);
        if (anonymousDelegation != null) {
            String delegationURL = format("{0}/rest/cmsMessage/id/{1}", config.getContextURL(),
                    anonymousDelegation.getDelegationCMS().getId());
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
        return UserVSDto.REPRESENTATIVE(representative, representativeDocument.getActivationCMS().getId(),
                numRepresentations, config.getContextURL());
    }

}