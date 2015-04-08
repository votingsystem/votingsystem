package org.votingsystem.web.controlcenter.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.EventVSElectionDto;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.ejb.TimeStampBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


@Stateless
public class EventVSElectionBean {

    private static final Logger log = Logger.getLogger(EventVSElectionBean.class.getSimpleName());

    @Inject EventVSBean eventVSBean;
    @Inject MessagesBean messages;
    @Inject ConfigVS config;
    @Inject TagVSBean tagVSBean;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject TimeStampBean timeStampBean;
    @Inject SubscriptionVSBean subscriptionVSBean;

    public EventVSElection saveEvent(MessageSMIME messageSMIME) throws Exception {
        SMIMEMessage smimeReq = messageSMIME.getSMIME();
        String serverURL = smimeReq.getHeader("serverURL")[0];
        AccessControlVS accessControl = checkAccessControl(serverURL);
        EventVSElectionDto request = messageSMIME.getSignedContent(EventVSElectionDto.class);
        request.validate(config.getContextURL());
        X509Certificate certCAVotacion = CertUtils.fromPEMToX509Cert(request.getCertCAVotacion().getBytes());
        X509Certificate userCert = CertUtils.fromPEMToX509Cert(request.getUserVS().getBytes());
        UserVS user = subscriptionVSBean.checkUser(UserVS.getUserVS(userCert));
        EventVSElection eventVS = request.getEventVSElection();
        eventVS.setAccessControlVS(accessControl);
        eventVS.setUserVS(user);
        setEventDatesState(eventVS);
        eventVS.resetId(); //this is to avoid collisions with access control ids
        if(request.getTags() != null) eventVS.setTagVSSet(tagVSBean.save(request.getTags()));
        dao.persist(eventVS);
        X509Certificate controlCenterX509Cert = signatureBean.getServerCert();
        CertificateVS eventVSControlCenterCertificate =  new CertificateVS(controlCenterX509Cert, eventVS,
                CertificateVS.Type.ACTOR_VS, CertificateVS.State.OK);
        dao.persist(eventVSControlCenterCertificate);
        Collection<X509Certificate> accessControlCerts = CertUtils.fromPEMToX509CertCollection(request.getCertChain().getBytes());
        X509Certificate accessControlX509Cert = accessControlCerts.iterator().next();
        CertificateVS eventVSAccessControlCertificate = new CertificateVS(accessControlX509Cert, eventVS,
                CertificateVS.Type.ACTOR_VS, CertificateVS.State.OK);
        dao.persist(eventVSAccessControlCertificate);
        CertificateVS eventVSRootCertificate = new CertificateVS(certCAVotacion, eventVS,
                CertificateVS.Type.VOTEVS_ROOT, CertificateVS.State.OK);
        eventVSRootCertificate.setActorVS(accessControl);
        dao.persist(eventVSRootCertificate);
        eventVS.setState(EventVS.State.ACTIVE);
        return dao.merge(eventVS);
    }

    public MessageSMIME cancelEvent(MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        EventVSElectionDto request = messageSMIME.getSignedContent(EventVSElectionDto.class);
        request.validateCancelation();
        Query query = dao.getEM().createQuery("select e from EventVSElection e where e.accessControlEventVSId =:eventId")
                .setParameter("eventId", request.getEventId());
        EventVSElection eventVS = dao.getSingleResult(EventVSElection.class, query);
        if(eventVS == null) throw new ValidationExceptionVS(
                "ERROR - EventVSElection not found - accessControlEventVSId: " +request.getEventId());
        if(EventVS.State.ACTIVE != eventVS.getState()) throw new ValidationExceptionVS(
                "ERROR - trying to cancel an EventVS tha isn't active");
        if(!(eventVS.getUserVS().getNif().equals(signer.getNif()) || signatureBean.isUserAdmin(signer.getNif())))
            throw new ValidationExceptionVS("userWithoutPrivilege - nif: " + signer.getNif());
        query = dao.getEM().createQuery("select c from CertificateVS c where c.eventVS =:eventVS and c.type =:type " +
                "and c.actorVS =:actorVS").setParameter("eventVS", eventVS).setParameter("type", CertificateVS.Type.ACTOR_VS)
                .setParameter("actorVS", eventVS.getAccessControlVS());
        CertificateVS accessControlCert = dao.getSingleResult(CertificateVS.class, query);
        if(accessControlCert == null) throw new ExceptionVS("ERROR - missing Access Control Cert");
        String fromUser = config.getServerName();
        String toUser = eventVS.getAccessControlVS().getName();
        String subject = messages.get("mime.subject.eventCancellationValidated");
        SMIMEMessage smimeResp = signatureBean.getSMIMEMultiSigned(fromUser, toUser, messageSMIME.getSMIME(), subject);
        dao.merge(messageSMIME.setSMIME(smimeResp));
        eventVS.setState(request.getState()).setDateCanceled(new Date());
        dao.merge(eventVS);
        log.info("cancelEvent - canceled EventVSElection  id:" + eventVS.getId());
        return messageSMIME;
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
                    Map responseMap = new ObjectMapper().readValue(responseVS.getMessage(),
                            new TypeReference<HashMap<String, Object>>() {});
                    accessControl = new AccessControlVS(ActorVS.parse(responseMap));
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

    public Map getStatsMap (EventVSElection eventVS) {
        Map result = new HashMap();
        result.put("id", eventVS.getId());
        result.put("subject", eventVS.getSubject());
        Query query = dao.getEM().createQuery("select count (a) from AccessRequestVS a where a.eventVS =:eventVS")
                .setParameter("eventVS", eventVS);
        result.put("numAccessRequests", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(a) from AccessRequestVS a where a.eventVS =:eventVS " +
                "and a.state =:state").setParameter("eventVS", eventVS).setParameter("state", AccessRequestVS.State.OK);
        result.put("numAccessRequestsOK", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(a) from AccessRequestVS a where a.eventVS =:eventVS " +
                "and a.state =:state").setParameter("eventVS", eventVS).setParameter("state", AccessRequestVS.State.CANCELED);
        result.put("numAccessRequestsCancelled", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS").setParameter("eventVS", eventVS);
        result.put("numVotesVS", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.OK);
        result.put("numVotesVSOK", (long) query.getSingleResult());
        query = dao.getEM().createQuery("select count(v) from VoteVS v where v.eventVS =:eventVS and v.state =:state")
                .setParameter("eventVS", eventVS).setParameter("state", VoteVS.State.CANCELED);
        result.put("numVotesVSVotesVSCANCELED", (long) query.getSingleResult());
        for(FieldEventVS option : eventVS.getFieldsEventVS()) {
            query = dao.getEM().createQuery("select count(v) from VoteVS v where v.optionSelected =:option " +
                    "and v.state =:state").setParameter("option", option).setParameter("state", VoteVS.State.OK);
            option.setNumVotesVS((long) query.getSingleResult());
        }
        result.put("fieldsEventVS", eventVS.getFieldsEventVS());
        return result;
    }

}