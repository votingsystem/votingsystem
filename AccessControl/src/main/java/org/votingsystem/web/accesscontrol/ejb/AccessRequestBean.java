package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessRequestVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.RequestRepeatedException;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class AccessRequestBean {

    private static Logger log = Logger.getLogger(AccessRequestBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;

    public AccessRequest saveRequest(MessageSMIME messageSMIME) throws Exception {
        UserVS signer = messageSMIME.getUserVS();
        AccessRequest request = new AccessRequest(messageSMIME.getSignedContentMap(),
                signer.getTimeStampToken().getTimeStampInfo().getGenTime());
        Query query = dao.getEM().createQuery("select a from AccessRequestVS a where a.userVS =:userVS and " +
                "a.eventVS =:eventVS and a.state =:state").setParameter("userVS", signer)
                .setParameter("eventVS", request.eventVS).setParameter("state", AccessRequestVS.State.OK);
        AccessRequestVS accessRequestVS = dao.getSingleResult(AccessRequestVS.class, query);
        if (accessRequestVS != null){
            throw new RequestRepeatedException(config.getRestURL() + "/messageSMIME/id/" +
                    accessRequestVS.getMessageSMIME().getId());
        } else {
            request.accessRequestVS = dao.persist(new AccessRequestVS(signer, messageSMIME, AccessRequestVS.State.OK,
                    request.hashAccessRequestBase64, request.eventVS));
            return request;
        }
    }
    
    public class AccessRequest {
        private String eventURL;
        private AccessRequestVS accessRequestVS;
        private String hashAccessRequestBase64;
        private TypeVS operation;
        private EventVSElection eventVS;
        public AccessRequest(Map messageMap, Date timeStampDate) throws ExceptionVS, IOException {
            if(!messageMap.containsKey("eventId")) throw new ValidationExceptionVS("missing param 'eventId'");
            if(!messageMap.containsKey("eventURL")) throw new ValidationExceptionVS("missing param 'eventURL'");
            eventURL = (String) messageMap.get("eventURL");
            if(!messageMap.containsKey("hashAccessRequestBase64"))
                throw new ValidationExceptionVS("missing param 'hashAccessRequestBase64'");
            hashAccessRequestBase64 = (String) messageMap.get("hashAccessRequestBase64");
            eventVS = dao.find(EventVSElection.class,((Number)messageMap.get("eventId")).longValue());
            if(eventVS == null) throw new ValidationExceptionVS("eventVSNotFound - eventId: " + messageMap.get("eventId"));
            if(!eventVS.isActive(timeStampDate)) {
                throw new ValidationExceptionVS("timeStampRangeErrorMsg - timeStampDate: " + timeStampDate +
                        " - range: [" + eventVS.getDateBegin() + " - " + eventVS.getDateFinish() + "]");
            }
            Query query = dao.getEM().createQuery("select a from AccessRequestVS a " + "where a.hashAccessRequestBase64 =:hashAccessRequestBase64")
                    .setParameter("hashAccessRequestBase64", hashAccessRequestBase64);
            AccessRequestVS accessRequestVS = dao.getSingleResult(AccessRequestVS.class, query);
            if (accessRequestVS != null) {
                log.log(Level.SEVERE, "ERROR - AccessRequest repeated -  hashRepeated:" + hashAccessRequestBase64);
                throw new ValidationExceptionVS("ERROR - AccessRequest repeated -  hashRepeated:" + hashAccessRequestBase64);
            }
        }

        public String getEventURL() {
            return eventURL;
        }

        public String getHashAccessRequestBase64() {
            return hashAccessRequestBase64;
        }

        public TypeVS getOperation() {
            return operation;
        }

        public EventVSElection getEventVS() {
            return eventVS;
        }

        public AccessRequestVS getAccessRequestVS() {
            return accessRequestVS;
        }
    }
}
