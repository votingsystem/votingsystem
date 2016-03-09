package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
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

    public MessageCMS cancelEvent(MessageCMS messageCMS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSSignedMessage cmsMessageReq = messageCMS.getCMS();
        UserVS signer = messageCMS.getUserVS();
        EventVSDto request = messageCMS.getSignedContent(EventVSDto.class);
        EventVS eventVS = dao.find(EventVS.class, request.getEventId());
        if (eventVS == null) throw new ValidationExceptionVS("ERROR - EventVS not found - eventId: " + request.getId());
        if(eventVS.getState() != EventVS.State.ACTIVE && eventVS.getState() != EventVS.State.PENDING)
                throw new ValidationExceptionVS("ERROR - EventVS not ACTIVE - eventId: " + request.getEventId());
        request.validateCancelation(config.getContextURL());
        if(!(eventVS.getUserVS().getNif().equals(signer.getNif()) || cmsBean.isAdmin(signer.getNif())))
            throw new ValidationExceptionVS("userWithoutPrivilege - nif: " + signer.getNif());
        CMSSignedMessage cmsMessageResp = null;
        String fromUser = config.getServerName();
        if(eventVS instanceof EventVSElection) {
            cmsMessageResp = cmsBean.addSignature(cmsMessageReq);
            String controlCenterUrl = ((EventVSElection)eventVS).getControlCenterVS().getServerURL();
            ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(cmsMessageResp.toPEM(),
                    ContentTypeVS.JSON_SIGNED, controlCenterUrl + "/rest/eventVSElection/cancel");
            if(ResponseVS.SC_OK != responseVSControlCenter.getStatusCode() &&
                    ResponseVS.SC_ERROR_REQUEST_REPEATED != responseVSControlCenter.getStatusCode()) {
                throw new ValidationExceptionVS(
                        "ERROR - controlCenterCommunicationErrorMsg -  controlCenterUrl: " + controlCenterUrl);
            }
        } else cmsMessageResp = cmsBean.addSignature(cmsMessageReq);
        messageCMS.setCMS(cmsMessageResp);
        eventVS.setState(request.getState());
        eventVS.setDateCanceled(new Date());
        if(eventVS.getKeyStoreVS() != null) {
            eventVS.getKeyStoreVS().setValid(Boolean.FALSE);
            dao.merge(eventVS.getKeyStoreVS());
        }
        dao.merge(eventVS);
        log.info("EventVS with id:" + eventVS.getId() + " changed to state: " + request.getState().toString());
        return messageCMS;
    }

}
