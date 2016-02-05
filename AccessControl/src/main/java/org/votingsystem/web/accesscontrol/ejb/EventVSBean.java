package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;
import java.util.logging.Logger;

@Stateless
public class EventVSBean {

    private static Logger log = Logger.getLogger(EventVSBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
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
        MessagesVS messages = MessagesVS.getCurrentInstance();
        SMIMEMessage smimeMessageReq = messageSMIME.getSMIME();
        UserVS signer = messageSMIME.getUserVS();
        EventVSDto request = messageSMIME.getSignedContent(EventVSDto.class);
        EventVS eventVS = dao.find(EventVS.class, request.getEventId());
        if (eventVS == null) throw new ValidationExceptionVS("ERROR - EventVS not found - eventId: " + request.getId());
        if(eventVS.getState() != EventVS.State.ACTIVE && eventVS.getState() != EventVS.State.PENDING)
                throw new ValidationExceptionVS("ERROR - EventVS not ACTIVE - eventId: " + request.getEventId());
        request.validateCancelation(config.getContextURL());
        if(!(eventVS.getUserVS().getNif().equals(signer.getNif()) || signatureBean.isAdmin(signer.getNif())))
            throw new ValidationExceptionVS("userWithoutPrivilege - nif: " + signer.getNif());
        SMIMEMessage smimeMessageResp = null;
        String fromUser = config.getServerName();
        String subject = messages.get("eventCancelationMsgSubject");
        if(eventVS instanceof EventVSElection) {
            String toUser = ((EventVSElection)eventVS).getControlCenterVS().getName();
                    smimeMessageResp = signatureBean.getSMIMEMultiSigned(fromUser, toUser, smimeMessageReq, subject);
            String controlCenterUrl = ((EventVSElection)eventVS).getControlCenterVS().getServerURL();
            ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(smimeMessageResp.getBytes(),
                    ContentTypeVS.JSON_SIGNED, controlCenterUrl + "/rest/eventVSElection/cancel");
            if(ResponseVS.SC_OK != responseVSControlCenter.getStatusCode() &&
                    ResponseVS.SC_ERROR_REQUEST_REPEATED != responseVSControlCenter.getStatusCode()) {
                throw new ValidationExceptionVS(
                        "ERROR - controlCenterCommunicationErrorMsg -  controlCenterUrl: " + controlCenterUrl);
            }
        } else smimeMessageResp = signatureBean.getSMIMEMultiSigned(fromUser, signer.getNif(), smimeMessageReq, subject);
        messageSMIME.setSMIME(smimeMessageResp);
        eventVS.setState(request.getState());
        eventVS.setDateCanceled(new Date());
        if(eventVS.getKeyStoreVS() != null) {
            eventVS.getKeyStoreVS().setValid(Boolean.FALSE);
            dao.merge(eventVS.getKeyStoreVS());
        }
        dao.merge(eventVS);
        log.info("EventVS with id:" + eventVS.getId() + " changed to state: " + request.getState().toString());
        return messageSMIME;
    }

}
