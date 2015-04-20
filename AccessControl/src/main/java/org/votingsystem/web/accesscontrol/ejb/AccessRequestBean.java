package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessRequestVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.util.Date;
import java.util.logging.Logger;

@Stateless
public class AccessRequestBean {

    private static Logger log = Logger.getLogger(AccessRequestBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;

    public AccessRequestDto saveRequest(MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        AccessRequestDto request =  messageSMIME.getSignedContent(AccessRequestDto.class);
        validateAccessRequest(request, signer.getTimeStampToken().getTimeStampInfo().getGenTime());
        Query query = dao.getEM().createQuery("select a from AccessRequestVS a where a.userVS =:userVS and " +
                "a.eventVS =:eventVS and a.state =:state").setParameter("userVS", signer)
                .setParameter("eventVS", request.getEventVS()).setParameter("state", AccessRequestVS.State.OK);
        AccessRequestVS accessRequestVS = dao.getSingleResult(AccessRequestVS.class, query);
        if (accessRequestVS != null){
            throw new ExceptionVS(MessageDto.REQUEST_REPEATED(null, config.getRestURL() + "/messageSMIME/id/" +
                    accessRequestVS.getMessageSMIME().getId()));
        } else {
            accessRequestVS = dao.persist(new AccessRequestVS(signer, messageSMIME, AccessRequestVS.State.OK,
                    request.getHashAccessRequestBase64(), request.getEventVS()));
            request.setAccessRequestVS(accessRequestVS);
            return request;
        }
    }

    private void validateAccessRequest(AccessRequestDto accessRequestDto, Date timeStampDate) throws ValidationExceptionVS {
        if(accessRequestDto.getEventId() == null) throw new ValidationExceptionVS("missing param 'eventId'");
        if(accessRequestDto.getEventURL() == null) throw new ValidationExceptionVS("missing param 'eventURL'");
        if(accessRequestDto.getHashAccessRequestBase64() == null) throw new ValidationExceptionVS("missing param 'hashAccessRequestBase64'");
        EventVSElection eventVS = dao.find(EventVSElection.class, accessRequestDto.getEventId());
        if(eventVS == null) throw new ValidationExceptionVS("eventVSNotFound - eventId: " + accessRequestDto.getEventId());
        if(!eventVS.isActive(timeStampDate)) {
            throw new ValidationExceptionVS("timeStampRangeErrorMsg - timeStampDate: " + timeStampDate +
                    " - range: [" + eventVS.getDateBegin() + " - " + eventVS.getDateFinish() + "]");
        }
        Query query = dao.getEM().createQuery("select a from AccessRequestVS a " + "where a.hashAccessRequestBase64 =:hashAccessRequestBase64")
                .setParameter("hashAccessRequestBase64", accessRequestDto.getHashAccessRequestBase64());
        AccessRequestVS accessRequestVS = dao.getSingleResult(AccessRequestVS.class, query);
        if (accessRequestVS != null) {
            throw new ValidationExceptionVS("ERROR - AccessRequest repeated -  hashRepeated:" +
                    accessRequestDto.getHashAccessRequestBase64());
        }
        accessRequestDto.setAccessRequestVS(accessRequestVS);
        accessRequestDto.setEventVS(eventVS);
    }

}
