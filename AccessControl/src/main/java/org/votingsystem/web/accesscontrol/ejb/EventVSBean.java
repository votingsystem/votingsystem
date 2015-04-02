package org.votingsystem.web.accesscontrol.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Stateless
public class EventVSBean {

    private static Logger log = Logger.getLogger(EventVSBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject MessagesBean messages;
    @Inject SignatureBean signatureBean;

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

    public MessageSMIME cancelEvent(MessageSMIME messageSMIME) throws Exception {
        SMIMEMessage smimeMessageReq = messageSMIME.getSMIME();
        UserVS signer = messageSMIME.getUserVS();
        EventVSCancelRequest request = new EventVSCancelRequest(smimeMessageReq.getSignedContent());
        if(!(request.eventVS.getUserVS().getNif().equals(signer.getNif()) || signatureBean.isUserAdmin(signer.getNif())))
            throw new ValidationExceptionVS("userWithoutPrivilege - nif: " + signer.getNif());
        SMIMEMessage smimeMessageResp = null;
        String fromUser = config.getServerName();
        String subject = messages.get("mime.subject.eventCancellationValidated");
        if(request.eventVS instanceof EventVSElection) {
            String toUser = ((EventVSElection)request.eventVS).getControlCenterVS().getName();
                    smimeMessageResp = signatureBean.getSMIMEMultiSigned(
                    fromUser, toUser, smimeMessageReq, subject);
            String controlCenterUrl = ((EventVSElection)request.eventVS).getControlCenterVS().getServerURL();
            ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(smimeMessageResp.getBytes(),
                    ContentTypeVS.SIGNED, controlCenterUrl + "/eventVSElection/cancelled");
            if(ResponseVS.SC_OK != responseVSControlCenter.getStatusCode() ||
                    ResponseVS.SC_ERROR_REQUEST_REPEATED != responseVSControlCenter.getStatusCode()) {
                throw new ValidationExceptionVS(
                        "ERROR - controlCenterCommunicationErrorMsg -  controlCenterUrl: " + controlCenterUrl);
            }
        } else smimeMessageResp = signatureBean.getSMIMEMultiSigned(fromUser, signer.getNif(), smimeMessageReq, subject);
        messageSMIME.setSMIME(smimeMessageResp);
        request.eventVS.setState(request.state);
        request.eventVS.setDateCanceled(new Date());
        dao.merge(request.eventVS);
        log.info("EventVS with id:" + request.eventVS.getId() + " changed to state: " + request.state.toString());
        return messageSMIME;
    }

    private class EventVSCancelRequest {
        String accessControlURL;
        EventVS.State state;
        TypeVS operation;
        EventVS eventVS;
        public EventVSCancelRequest(String signedContent) throws ExceptionVS, IOException {
            Map messageMap = new ObjectMapper().readValue(signedContent, new TypeReference<HashMap<String, Object>>() {});
            if(!messageMap.containsKey("eventId")) throw new ValidationExceptionVS("missing param 'eventId'");
            eventVS = dao.find(EventVS.class, Long.valueOf((String) messageMap.get("eventId")));
            if(eventVS == null) throw new ValidationExceptionVS("eventVSNotFound - eventId: " + messageMap.get("eventId"));
            if(eventVS.getState() != EventVS.State.ACTIVE) throw new ValidationExceptionVS(
                    "eventNotActiveMsg - eventId: " + messageMap.get("eventId"));
            if(!messageMap.containsKey("state"))
                throw new ValidationExceptionVS("missing param 'state'");
            if(!messageMap.containsKey("accessControlURL"))
                throw new ValidationExceptionVS("missing param 'accessControlURL'");
            if(!messageMap.containsKey("operation"))
                throw new ValidationExceptionVS("missing param 'operation'");
            if(TypeVS.EVENT_CANCELLATION != TypeVS.valueOf((String) messageMap.get("operation"))) throw new ValidationExceptionVS(
                    "ERROR - operation expected: 'EVENT_CANCELLATION' - operation found: " + messageMap.get("operation"));
            state = EventVS.State.valueOf((String) messageMap.get("state"));
            if(!((EventVS.State.CANCELED == state) || (EventVS.State.DELETED_FROM_SYSTEM == state))) {
                throw new ValidationExceptionVS("invalid 'EVENT_CANCELLATION' state:" + state);
            }
            String requestURL = StringUtils.checkURL((String) messageMap.get("accessControlURL"));
            if(!requestURL.equals(config.getContextURL()))  throw new ValidationExceptionVS(
                    "accessControlURLError - expected: " + config.getContextURL() + " - found: " + requestURL);
        }
    }

}
