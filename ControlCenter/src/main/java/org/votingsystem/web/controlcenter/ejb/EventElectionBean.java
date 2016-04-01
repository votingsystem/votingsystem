package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.EventVSStatsDto;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.*;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
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
public class EventElectionBean {

    private static final Logger log = Logger.getLogger(EventElectionBean.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject TimeStampBean timeStampBean;
    @Inject SubscriptionBean subscriptionBean;

    public EventElection saveEvent(CMSMessage cmsMessage) throws Exception {
        CMSSignedMessage cmsReq = cmsMessage.getCMS();
        EventVSDto request = cmsReq.getSignedContent(EventVSDto.class);
        request.validate(config.getContextURL());
        AccessControl accessControl = checkAccessControl(request.getAccessControlURL());
        X509Certificate certCAVotacion = PEMUtils.fromPEMToX509Cert(request.getCertCAVotacion().getBytes());
        X509Certificate userCert = PEMUtils.fromPEMToX509Cert(request.getUser().getBytes());
        User user = subscriptionBean.checkUser(User.FROM_X509_CERT(userCert));
        EventElection eventVS = request.getEventElection();
        eventVS.setAccessControl(accessControl);
        eventVS.setUser(user);
        eventVS.setCmsMessage(cmsMessage);
        setEventDatesState(eventVS);
        eventVS.updateAccessControlIds();
        dao.persist(eventVS);
        X509Certificate controlCenterX509Cert = cmsBean.getServerCert();
        Certificate eventVSControlCenterCertificate =  Certificate.ACTOR(null, controlCenterX509Cert);
        dao.persist(eventVSControlCenterCertificate);
        Collection<X509Certificate> accessControlCerts = PEMUtils.fromPEMToX509CertCollection(request.getCertChain().getBytes());
        X509Certificate accessControlX509Cert = accessControlCerts.iterator().next();
        Certificate eventVSAccessControlCertificate = Certificate.ACTOR(accessControl, accessControlX509Cert);
        dao.persist(eventVSAccessControlCertificate);
        Certificate eventVSCertificate = Certificate.ELECTION(certCAVotacion);
        eventVSCertificate.setActor(accessControl);
        dao.persist(eventVSCertificate);
        eventVS.setAccessControlCert(eventVSAccessControlCertificate).setControlCenterCert(eventVSControlCenterCertificate)
                .setCertificate(eventVSCertificate).setState(EventVS.State.ACTIVE);
        return dao.merge(eventVS);
    }

    public CMSMessage cancelEvent(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User signer = cmsMessage.getUser();
        EventVSDto request = cmsMessage.getSignedContent(EventVSDto.class);
        Query query = dao.getEM().createQuery("select e from EventElection e where e.accessControlEventId =:eventId")
                .setParameter("eventId", request.getEventId());
        EventElection eventVS = dao.getSingleResult(EventElection.class, query);
        if(eventVS == null) throw new ValidationException(
                "ERROR - EventElection not found - accessControlEventId: " +request.getEventId());
        if(EventVS.State.ACTIVE != eventVS.getState()) throw new ValidationException(new MessageDto(
                ResponseVS.SC_ERROR_REQUEST_REPEATED, "ERROR - trying to cancel an EventVS tha isn't active"));
        if(!(eventVS.getUser().getNif().equals(signer.getNif()) || cmsBean.isAdmin(signer.getNif())))
            throw new ValidationException("userWithoutPrivilege - nif: " + signer.getNif());
        request.validateCancelation(eventVS.getAccessControl().getServerURL());
        CMSSignedMessage cmsResp = cmsBean.addSignature(cmsMessage.getCMS());
        dao.merge(cmsMessage.setCMS(cmsResp));
        eventVS.setState(request.getState()).setDateCanceled(new Date());
        dao.merge(eventVS);
        log.info("cancelEvent - canceled EventElection  id:" + eventVS.getId());
        return cmsMessage;
    }

    private AccessControl checkAccessControl(String serverURL) {
        log.info("checkAccessControl - serverURL: " + serverURL);
        serverURL = StringUtils.checkURL(serverURL);
        Query query = dao.getEM().createQuery("select a from AccessControl a where a.serverURL =:serverURL")
                .setParameter("serverURL", serverURL);
        AccessControl accessControl = dao.getSingleResult(AccessControl.class, query);
        if(accessControl != null) return accessControl;
        else {
            ResponseVS responseVS = HttpHelper.getInstance().getData(Actor.getServerInfoURL(serverURL),
                    ContentType.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    accessControl = (AccessControl) ((ActorDto)responseVS.getMessage(ActorDto.class)).getActor();
                    return dao.persist(accessControl);
                } catch(Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
        return null;
    }
    
    public void checkEventVSDates (EventVS eventVS) throws ValidationException {
        if(eventVS.getState() == EventVS.State.CANCELED) return;
        if(eventVS.getDateBegin().after(eventVS.getDateFinish())) throw new ValidationException(
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
    

    public void setEventDatesState (EventVS eventVS) throws ValidationException {
        if(eventVS.getDateBegin() == null) eventVS.setDateBegin(new Date());
        Date todayDate = new Date(System.currentTimeMillis() + 1);// to avoid race conditions
        if(eventVS.getDateBegin().after(eventVS.getDateFinish())) throw new ValidationException(
                "date begin after date finish - dateBegin: " + eventVS.getDateBegin() + " - dateFinish: " + eventVS.getDateFinish());

        if (todayDate.after(eventVS.getDateFinish())) eventVS.setState(EventVS.State.TERMINATED);
        if (todayDate.after(eventVS.getDateBegin()) && todayDate.before(eventVS.getDateFinish()))
            eventVS.setState(EventVS.State.ACTIVE);
        if (todayDate.before(eventVS.getDateBegin())) eventVS.setState(EventVS.State.PENDING);
    }

    public EventVSStatsDto getStats (EventElection eventVS) {
        EventVSStatsDto statsDto = new EventVSStatsDto(eventVS);
        statsDto.setId(eventVS.getId());
        statsDto.setSubject(eventVS.getSubject() + " - 'this is inside simple quotes' - ");
        Query query = dao.getEM().createQuery("select count(v) from Vote v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", Vote.State.OK);
        statsDto.setNumVotes((long) query.getSingleResult());
        for(FieldEvent option : eventVS.getFieldsEventVS()) {
            query = dao.getEM().createQuery("select count(v) from Vote v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", Vote.State.OK);
            option.setNumVotes((long) query.getSingleResult());
        }
        statsDto.setFieldsEventVS(eventVS.getFieldsEventVS());
        return statsDto;
    }

}