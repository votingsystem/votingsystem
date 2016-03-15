package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
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
    @Inject CMSBean cmsBean;

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

    public CMSMessage cancelEvent(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSSignedMessage cmsMessageReq = cmsMessage.getCMS();
        User signer = cmsMessage.getUser();
        EventVSDto request = cmsMessage.getSignedContent(EventVSDto.class);
        EventVS eventVS = dao.find(EventVS.class, request.getEventId());
        if (eventVS == null) throw new ValidationException("ERROR - EventVS not found - eventId: " + request.getId());
        if(eventVS.getState() != EventVS.State.ACTIVE && eventVS.getState() != EventVS.State.PENDING)
                throw new ValidationException("ERROR - EventVS not ACTIVE - eventId: " + request.getEventId());
        request.validateCancelation(config.getContextURL());
        if(!(eventVS.getUser().getNif().equals(signer.getNif()) || cmsBean.isAdmin(signer.getNif())))
            throw new ValidationException("userWithoutPrivilege - nif: " + signer.getNif());
        CMSSignedMessage cmsMessageResp = null;
        String fromUser = config.getServerName();
        if(eventVS instanceof EventElection) {
            cmsMessageResp = cmsBean.addSignature(cmsMessageReq);
            String controlCenterUrl = ((EventElection)eventVS).getControlCenter().getServerURL();
            ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(cmsMessageResp.toPEM(),
                    ContentType.JSON_SIGNED, controlCenterUrl + "/rest/eventElection/cancel");
            if(ResponseVS.SC_OK != responseVSControlCenter.getStatusCode() &&
                    ResponseVS.SC_ERROR_REQUEST_REPEATED != responseVSControlCenter.getStatusCode()) {
                throw new ValidationException(
                        "ERROR - controlCenterCommunicationErrorMsg -  controlCenterUrl: " + controlCenterUrl);
            }
        } else cmsMessageResp = cmsBean.addSignature(cmsMessageReq);
        cmsMessage.setCMS(cmsMessageResp);
        eventVS.setState(request.getState());
        eventVS.setDateCanceled(new Date());
        if(eventVS.getKeyStore() != null) {
            eventVS.getKeyStore().setValid(Boolean.FALSE);
            dao.merge(eventVS.getKeyStore());
        }
        dao.merge(eventVS);
        log.info("EventVS with id:" + eventVS.getId() + " changed to state: " + request.getState().toString());
        return cmsMessage;
    }

}
