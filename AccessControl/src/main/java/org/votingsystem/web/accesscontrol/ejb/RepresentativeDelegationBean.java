package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.json.RepresentativeDelegationRequest;
import org.votingsystem.json.RepresentativeJSON;
import org.votingsystem.json.RepresentativeRevokeRequest;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.RequestRepeatedException;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    @Inject MessagesBean messages;

    //{"operation":"REPRESENTATIVE_SELECTION","representativeNif":"...","representativeName":"...","UUID":"..."}
    public synchronized RepresentationDocument saveDelegation(MessageSMIME messageSMIME) throws Exception {
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        UserVS userVS = messageSMIME.getUserVS();
        checkUserDelegationStatus(userVS);
        RepresentativeDelegationRequest request = messageSMIME.getSignedContent(RepresentativeDelegationRequest.class);
        request.validate();
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                .setParameter("nif", request.getRepresentativeNif()).setParameter("type", UserVS.Type.REPRESENTATIVE);
        UserVS representative = dao.getSingleResult(UserVS.class, query);
        if(representative == null)  throw new ValidationExceptionVS(
                "ERROR - representativeNifErrorMsg - representativeNif: " + request.getRepresentativeNif());
        cancelPublicDelegation(messageSMIME);
        userVS.setRepresentative(representative);
        RepresentationDocument representationDocument = dao.persist(new RepresentationDocument(messageSMIME,
                userVS, representative, RepresentationDocument.State.OK));
        String toUser = userVS.getNif();
        String subject = messages.get("representativeSelectValidationSubject");
        messageSMIME.setSMIME(signatureBean.getSMIMEMultiSigned(toUser, smimeMessage, subject));
        log.info(format("user id: {0} - representationDocument id: {1}", userVS.getNif(),
                representationDocument.getId()));
        return representationDocument;
        /*String msg = messageSource.getMessage('representativeAssociatedMsg',[messageJSON.representativeName, userVS.nif].toArray(), locale)
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, data:messageSMIME,
                type:TypeVS.REPRESENTATIVE_SELECTION)*/
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
        UserVS userVS = messageSMIME.getUserVS();
        checkUserDelegationStatus(userVS);
        AnonymousDelegationRequest request = messageSMIME.getSignedContent(AnonymousDelegationRequest.class);
        request.validateRequest();
        cancelPublicDelegation(messageSMIME);
        SMIMEMessage smimeMessageResp = signatureBean.getSMIMEMultiSigned(userVS.getNif(), messageSMIME.getSMIME(), null);
        messageSMIME.setType(request.operation).setSMIME(smimeMessageResp);
        X509Certificate anonymousCert = csrBean.signAnonymousDelegationCert(csrRequest);
        userVS.setRepresentative(null);
        dao.persist(new AnonymousDelegation(AnonymousDelegation.Status.OK, messageSMIME,
                userVS, request.dateFrom, request.dateTo));
        return anonymousCert;
    }

    public RepresentationDocument saveAnonymousDelegation(MessageSMIME messageSMIME) throws Exception {
        X509Certificate anonCert =  messageSMIME.getAnonymousSigner().getCertificate();
        Map<String, String> certExtensionData = CertUtils.getCertExtensionData(anonCert,
                ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID);
        Query query = dao.getEM().createQuery("select c from  CertificateVS c where c.serialNumber=:serialNumber " +
                "and c.type =:type and c.state =:state and c.hashCertVSBase64 =:hashCertVS")
                .setParameter("serialNumber", anonCert.getSerialNumber().longValue())
                .setParameter("type", CertificateVS.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION).setParameter("state", CertificateVS.State.OK)
                .setParameter("hashCertVS", certExtensionData.get("hashCertVS"));
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS == null) throw new ValidationExceptionVS(messages.get("certificateVSUnknownErrorMsg"));
        AnonymousDelegationRequest request = messageSMIME.getSignedContent(AnonymousDelegationRequest.class);
        String toUser = certificateVS.getHashCertVSBase64();
        String subject = messages.get("representativeSelectValidationSubject");
        SMIMEMessage smimeMessage = signatureBean.getSMIMEMultiSigned(toUser, messageSMIME.getSMIME(), subject);
        messageSMIME.setSMIME(smimeMessage);
        dao.merge(messageSMIME);
        dao.merge(certificateVS.setState(CertificateVS.State.USED).setMessageSMIME(messageSMIME));
        RepresentationDocument representationDocument = new RepresentationDocument(messageSMIME, null,
                request.representative, RepresentationDocument.State.OK);
        dao.persist(representationDocument);
        return representationDocument;
        /*String msg = messageSource.getMessage('anonymousRepresentativeAssociatedMsg',
        [request.representativeName].toArray(), locale)
        log.debug "$methodName - representationDocument: ${representationDocument.id}"
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, messageSMIME: messageSMIME,
                type:TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION, contentType: ContentTypeVS.JSON_SIGNED)*/
    }


    public MessageSMIME cancelAnonymousDelegation(MessageSMIME messageSMIME) throws Exception {
        UserVS userVS = messageSMIME.getUserVS();
        AnonymousDelegationRequest request = messageSMIME.getSignedContent(AnonymousDelegationRequest.class);
        request.validateAnonymousDelegationCancellation(userVS);
        request.anonymousDelegation.setStatus(AnonymousDelegation.Status.CANCELED).setCancellationSMIME(
                messageSMIME).setDateCancelled(new Date());
        dao.merge(request.anonymousDelegation);
        request.representationDocument.setCancellationSMIME(messageSMIME);
        dao.merge(request.representationDocument);
        SMIMEMessage smimeMessageResp = signatureBean.getSMIMEMultiSigned(userVS.getNif(), messageSMIME.getSMIME(), null);
        messageSMIME.setSMIME(smimeMessageResp);
        dao.merge(messageSMIME);
        return messageSMIME;
    }

    public void cancelRepresentationDocument(MessageSMIME messageSMIME, UserVS userVS) {
        Query query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:userVS " +
                "and r.state =:state").setParameter("userVS", userVS).setParameter("state", RepresentationDocument.State.OK);
        RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
        if(representationDocument != null) {
            log.info("cancelRepresentationDocument - User changing representative");
            representationDocument.setState(RepresentationDocument.State.CANCELED).setCancellationSMIME(messageSMIME)
                    .setDateCanceled(userVS.getTimeStampToken().getTimeStampInfo().getGenTime());
            dao.merge(representationDocument);
            log.info("cancelRepresentationDocument - user: " + userVS.getNif());
        } else log.info("cancelRepresentationDocument - user without representative -  nif: " + userVS.getNif());
    }


    public void cancelRepresentationDocument(MessageSMIME messageSMIME) {
        UserVS userVS = messageSMIME.getUserVS();
        Query query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:userVS " +
                "and r .state =:state").setParameter("userVS", userVS).setParameter("state", RepresentativeDocument.State.OK);
        RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
        if(representationDocument != null) {
            representationDocument.setState(RepresentationDocument.State.CANCELED).setCancellationSMIME(messageSMIME)
                    .setDateCanceled(userVS.getTimeStampToken().getTimeStampInfo().getGenTime());
            dao.merge(representationDocument);
            log.info(format("cancelRepresentationDocument - user '{0}' " +
                    " - representationDocument {1}", userVS.getNif(), representationDocument.getId()));
        } else log.info("cancelRepresentationDocument - user without representative - user id: " + userVS.getId());
    }

    private void checkUserDelegationStatus(UserVS userVS) throws ValidationExceptionVS, RequestRepeatedException {
        String msg = null;
        if(UserVS.Type.REPRESENTATIVE == userVS.getType()) throw new ValidationExceptionVS(
                "ERROR - user is representative: " + userVS.getNif());
        AnonymousDelegation anonymousDelegation = getAnonymousDelegation(userVS);
        if (anonymousDelegation != null) {
            String delegationURL = format("{0}/messageSMIME/id/{1}", config.getRestURL(),
                    anonymousDelegation.getDelegationSMIME().getId());
            throw new RequestRepeatedException(messages.get("userWithPreviousDelegationErrorMsg", userVS.getNif(),
                    DateUtils.getDateStr(anonymousDelegation.getDateTo())), delegationURL);
        }
    }

    public MessageSMIME processRevoke(MessageSMIME messageSMIME) throws Exception {
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        UserVS userVS = messageSMIME.getUserVS();
        Query query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS and " +
                "r.state =:state").setParameter("userVS", userVS).setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        if(representativeDocument == null) throw new ValidationExceptionVS( 
                messages.get("unsubscribeRepresentativeUserErrorMsg", userVS.getNif()));
        log.info("processRevoke - user: " + userVS.getId());
        RepresentativeRevokeRequest request = messageSMIME.getSignedContent(RepresentativeRevokeRequest.class);
        request.validate(userVS);
        query = dao.getEM().createQuery("select u from UserVS u where u.representative =:userVS")
                .setParameter("userVS", userVS);
        List<UserVS> representedUsers = query.getResultList();
        for(UserVS representedUser : representedUsers) {
            query = dao.getEM().createQuery("select r from RepresentationDocument r where r.userVS =:representedUser " +
                    "and r.representative =:representative and r.state =:state").setParameter("representedUser", representedUser)
                    .setParameter("representative", userVS).setParameter("state", RepresentationDocument.State.OK);
            RepresentationDocument representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
            representationDocument.setState(RepresentationDocument.State.CANCELED_BY_REPRESENTATIVE).setCancellationSMIME(
                    messageSMIME).setDateCanceled(userVS.getTimeStampToken().getTimeStampInfo().getGenTime());
            dao.merge(representationDocument);
            dao.merge(representedUser.setRepresentative(null));
        }
        dao.merge(userVS.setType(UserVS.Type.USER));
        String toUser = userVS.getNif();
        String subject = messages.get("unsubscribeRepresentativeValidationSubject");
        SMIMEMessage smimeMessageResp = signatureBean.getSMIMEMultiSigned(toUser, smimeMessage, subject);
        messageSMIME.setSMIME(smimeMessageResp);
        dao.merge(messageSMIME);
        dao.merge(representativeDocument.setCancellationSMIME(messageSMIME).setDateCanceled(new Date()));
        return messageSMIME;
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

    private class AnonymousDelegationRequest {
        String representativeNif, representativeName, accessControlURL, hashCertVSBase64, originHashCertVSBase64;
        Integer weeksOperationActive;
        Date dateFrom, dateTo;
        TypeVS operation;
        UserVS representative;
        AnonymousDelegation anonymousDelegation;
        CertificateVS certificateVS;
        RepresentationDocument representationDocument;

        public AnonymousDelegationRequest(String signedContent) throws ExceptionVS { }

        public void validate() throws ValidationExceptionVS {
            if(weeksOperationActive == null) throw new ValidationExceptionVS("missing param 'weeksOperationActive'");
            if(dateFrom == null) throw new ValidationExceptionVS("missing param 'dateFrom'");
            if(dateTo == null) throw new ValidationExceptionVS("missing param 'dateTo'");
            if(dateFrom.after(dateTo)) throw new ValidationExceptionVS(
                    format("dateFrom '{0}' after '{1}'", dateFrom, dateTo));
            Date dateFromCheck = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
            if(dateFrom.compareTo(dateFromCheck) != 0) throw new ValidationExceptionVS(
                    format("dateFrom expected '{0}' received '{1}'", dateFromCheck, dateFrom));
            Date dateToCheck = DateUtils.addDays(dateFromCheck, weeksOperationActive * 7).getTime();
            if(dateTo.compareTo(dateToCheck) != 0) throw new ValidationExceptionVS(
                    format("dateTo expected '{0}' received '{1}'", dateToCheck, dateTo));
        }

        public AnonymousDelegationRequest validateDelegationData() throws ExceptionVS {
            representativeNif = NifUtils.validate(representativeNif);
            if(representativeName == null) throw new ValidationExceptionVS("missing param 'representativeName'");
            Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                    .setParameter("nif", representativeNif).setParameter("type", UserVS.Type.REPRESENTATIVE);
            representative = dao.getSingleResult(UserVS.class, query);
            if(representative == null) throw new ValidationExceptionVS(
                    "ERROR - representativeNifErrorMsg - nif: " + representativeNif);
            return this;
        }

        public AnonymousDelegationRequest validateRequest() throws ExceptionVS {
            if(TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST != operation) throw new ValidationExceptionVS(
                    "expected operation 'ANONYMOUS_REPRESENTATIVE_REQUEST' but found '" + operation.toString() + "'");
            return this;
        }
        public AnonymousDelegationRequest validateDelegation() throws ExceptionVS {
            if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION != operation) throw new ValidationExceptionVS(
                    "expected operation 'ANONYMOUS_REPRESENTATIVE_SELECTION' but found '" + operation.toString() + "'");
            validateDelegationData();
            return this;
        }

        public AnonymousDelegationRequest validateAnonymousDelegationCancellation(UserVS userVS) throws ExceptionVS, NoSuchAlgorithmException {
            if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELED != operation) throw new ValidationExceptionVS(
                    "expected operation 'ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELED' but found '" + operation.toString() + "'");
            validateDelegationData();
            anonymousDelegation = getAnonymousDelegation(userVS);
            if(anonymousDelegation == null)  throw new ValidationExceptionVS(
                    "ERROR - userWithoutAnonymousDelegationErrorMsg - userVS nif: " +userVS.getNif());
            if(hashCertVSBase64 == null) throw new ValidationExceptionVS("missing param 'hashCertVSBase64'");
            if(originHashCertVSBase64 == null) throw new ValidationExceptionVS("missing param 'originHashCertVSBase64'");
            if(!hashCertVSBase64.equals(CMSUtils.getHashBase64(originHashCertVSBase64, ContextVS.VOTING_DATA_DIGEST)))
                throw new ValidationExceptionVS("provided origin doesn't match hash origin");
            Query query = dao.getEM().createQuery("select c from CertificateVS c where c.state =:state " +
                    "and c.hashCertVSBase64 =:hashCertVS").setParameter("state", CertificateVS.State.USED)
                    .setParameter("hashCertVS", hashCertVSBase64);
            certificateVS = dao.getSingleResult(CertificateVS.class, query);
            if(certificateVS == null) throw new ValidationExceptionVS("data doesn't match param CertificateVS");
            query = dao.getEM().createQuery("select r from RepresentationDocument r where r.activationSMIME =:messageSMIME")
                    .setParameter("messageSMIME", certificateVS.getMessageSMIME());
            representationDocument = dao.getSingleResult(RepresentationDocument.class, query);
            if(representationDocument == null) throw new ValidationExceptionVS(
                    "data doesn't macth param RepresentationDocument");
            return this;
        }
    }


    public RepresentativeJSON getRepresentativeJSON(UserVS representative) {
        Query query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS and " +
                "r.state =:state").setParameter("userVS", representative).setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        String representativeMessageURL = format("{0}/messageSMIME/id/{1}", config.getRestURL(),
                representativeDocument.getActivationSMIME().getId());
        String imageURL = format("{0}/representative/id/{1}/image", config.getRestURL(), representative.getId());
        String URL = format("{0}/representative/id/{1}", config.getRestURL(), representative.getId());
        query = dao.getEM().createQuery("select count(r) from RepresentationDocument r where " +
                "r.representative =:representative and r.state =:state").setParameter("representative", representative)
                .setParameter("state", RepresentationDocument.State.OK);
        long numRepresentations = (long) query.getSingleResult() + 1;//plus the representative itself
        return new RepresentativeJSON(representative,  representativeDocument.getActivationSMIME().getId(),
                numRepresentations, config.getRestURL());
    }

}
