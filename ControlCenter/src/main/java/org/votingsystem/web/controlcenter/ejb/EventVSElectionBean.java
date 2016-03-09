package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.EventVSStatsDto;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.*;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


@Stateless
public class EventVSElectionBean {

    private static final Logger log = Logger.getLogger(EventVSElectionBean.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject TimeStampBean timeStampBean;
    @Inject SubscriptionVSBean subscriptionVSBean;

    public EventVSElection saveEvent(MessageCMS messageCMS) throws Exception {
        CMSSignedMessage cmsReq = messageCMS.getCMS();
        EventVSDto request = cmsReq.getSignedContent(EventVSDto.class);
        request.validate(config.getContextURL());
        AccessControlVS accessControl = checkAccessControl(request.getAccessControlURL());
        X509Certificate certCAVotacion = PEMUtils.fromPEMToX509Cert(request.getCertCAVotacion().getBytes());
        X509Certificate userCert = PEMUtils.fromPEMToX509Cert(request.getUserVS().getBytes());
        UserVS user = subscriptionVSBean.checkUser(UserVS.FROM_X509_CERT(userCert));
        EventVSElection eventVS = request.getEventVSElection();
        eventVS.setAccessControlVS(accessControl);
        eventVS.setUserVS(user);
        eventVS.setPublishRequestCMS(messageCMS);
        setEventDatesState(eventVS);
        eventVS.updateAccessControlIds();
        dao.persist(eventVS);
        X509Certificate controlCenterX509Cert = cmsBean.getServerCert();
        CertificateVS eventVSControlCenterCertificate =  CertificateVS.ACTORVS(null, controlCenterX509Cert);
        dao.persist(eventVSControlCenterCertificate);
        Collection<X509Certificate> accessControlCerts = PEMUtils.fromPEMToX509CertCollection(request.getCertChain().getBytes());
        X509Certificate accessControlX509Cert = accessControlCerts.iterator().next();
        CertificateVS eventVSAccessControlCertificate = CertificateVS.ACTORVS(accessControl, accessControlX509Cert);
        dao.persist(eventVSAccessControlCertificate);
        CertificateVS eventVSCertificate = CertificateVS.ELECTION(certCAVotacion);
        eventVSCertificate.setActorVS(accessControl);
        dao.persist(eventVSCertificate);
        eventVS.setAccessControlCert(eventVSAccessControlCertificate).setControlCenterCert(eventVSControlCenterCertificate)
                .setCertificateVS(eventVSCertificate).setState(EventVS.State.ACTIVE);
        return dao.merge(eventVS);
    }

    public MessageCMS cancelEvent(MessageCMS messageCMS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS signer = messageCMS.getUserVS();
        EventVSDto request = messageCMS.getSignedContent(EventVSDto.class);
        Query query = dao.getEM().createQuery("select e from EventVSElection e where e.accessControlEventVSId =:eventId")
                .setParameter("eventId", request.getEventId());
        EventVSElection eventVS = dao.getSingleResult(EventVSElection.class, query);
        if(eventVS == null) throw new ValidationExceptionVS(
                "ERROR - EventVSElection not found - accessControlEventVSId: " +request.getEventId());
        if(EventVS.State.ACTIVE != eventVS.getState()) throw new ValidationExceptionVS(new MessageDto(
                ResponseVS.SC_ERROR_REQUEST_REPEATED, "ERROR - trying to cancel an EventVS tha isn't active"));
        if(!(eventVS.getUserVS().getNif().equals(signer.getNif()) || cmsBean.isAdmin(signer.getNif())))
            throw new ValidationExceptionVS("userWithoutPrivilege - nif: " + signer.getNif());
        request.validateCancelation(eventVS.getAccessControlVS().getServerURL());
        CMSSignedMessage cmsResp = cmsBean.addSignature(messageCMS.getCMS());
        dao.merge(messageCMS.setCMS(cmsResp));
        eventVS.setState(request.getState()).setDateCanceled(new Date());
        dao.merge(eventVS);
        log.info("cancelEvent - canceled EventVSElection  id:" + eventVS.getId());
        return messageCMS;
    }

    private AccessControlVS checkAccessControl(String serverURL) {
        log.info("checkAccessControlVS - serverURL: " + serverURL);
        serverURL = StringUtils.checkURL(serverURL);
        Query query = dao.getEM().createQuery("select a from AccessControlVS a where a.serverURL =:serverURL")
                .setParameter("serverURL", serverURL);
        AccessControlVS accessControl = dao.getSingleResult(AccessControlVS.class, query);
        if(accessControl != null) return accessControl;
        else {
            ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(serverURL),
                    ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    accessControl = (AccessControlVS) ((ActorVSDto)responseVS.getMessage(ActorVSDto.class)).getActorVS();
                    return dao.persist(accessControl);
                } catch(Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
        return null;
    }
    
    public void checkEventVSDates (EventVS eventVS) throws ValidationExceptionVS {
        if(eventVS.getState() == EventVS.State.CANCELED) return;
        if(eventVS.getDateBegin().after(eventVS.getDateFinish())) throw new ValidationExceptionVS(
                "date begin after date finish - dateBegin: " + eventVS.getDateBegin() + " - dateFinish: " + eventVS.getDateFinish());
        Date currentDate = new Date();
        if (currentDate.after(eventVS.getDateFinish()) && eventVS.getState() != EventVS.State.TERMINATED) {
            eventVS.setState(EventVS.State.TERMINATED);
        } else if(eventVS.getDateBegin().after(currentDate) && eventVS.getState() != EventVS.State.PENDING) {
            eventVS.setState(EventVS.State.PENDING);
        } else if(eventVS.getDateBegin().before(currentDate) && eventVS.getDateFinish().after(currentDate) &&
                eventVS.getState() != EventVS.State.ACTIVE) {
            eventVS.setState(EventVS.State.ACTIVE);
        }
        if(eventVS.getId() != null) dao.merge(eventVS);
        else dao.persist(eventVS);
    }
    

    public void setEventDatesState (EventVS eventVS) throws ValidationExceptionVS {
        if(eventVS.getDateBegin() == null) eventVS.setDateBegin(new Date());
        Date todayDate = new Date(System.currentTimeMillis() + 1);// to avoid race conditions
        if(eventVS.getDateBegin().after(eventVS.getDateFinish())) throw new ValidationExceptionVS(
                "date begin after date finish - dateBegin: " + eventVS.getDateBegin() + " - dateFinish: " + eventVS.getDateFinish());

        if (todayDate.after(eventVS.getDateFinish())) eventVS.setState(EventVS.State.TERMINATED);
        if (todayDate.after(eventVS.getDateBegin()) && todayDate.before(eventVS.getDateFinish()))
            eventVS.setState(EventVS.State.ACTIVE);
        if (todayDate.before(eventVS.getDateBegin())) eventVS.setState(EventVS.State.PENDING);
    }

    public EventVSStatsDto getStats (EventVSElection eventVS) {
        EventVSStatsDto statsDto = new EventVSStatsDto();
        statsDto.setId(eventVS.getId());
        statsDto.setSubject(eventVS.getSubject() + " - 'this is inside simple quotes' - ");
        Query query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS")
                .setParameter("eventVS", eventVS);
        statsDto.setNumVotesVS((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.OK);
        statsDto.setNumVotesVSOK((long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.CANCELED);
        statsDto.setNumVotesVSVotesVSCANCELED((long) query.getSingleResult());
        for(FieldEventVS option : eventVS.getFieldsEventVS()) {
            query = dao.getEM().createQuery("select count(v) from VoteVS v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", VoteVS.State.OK);
            option.setNumVotesVS((long) query.getSingleResult());
        }
        statsDto.setFieldsEventVS(eventVS.getFieldsEventVS());
        return statsDto;
    }

}