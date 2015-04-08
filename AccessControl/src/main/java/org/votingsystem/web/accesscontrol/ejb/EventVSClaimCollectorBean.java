package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.dto.EventVSClaimStats;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Stateless
public class EventVSClaimCollectorBean {

    private static final Logger log = Logger.getLogger(EventVSClaimCollectorBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;
    @Inject MessagesBean messages;


    public MessageSMIME save(MessageSMIME messageSMIME) throws Exception {
        UserVS userVS = messageSMIME.getUserVS();
        SignedClaimRequest request = messageSMIME.getSignedContent(SignedClaimRequest.class);
        request.validate(userVS.getTimeStampToken().getTimeStampInfo().getGenTime());
        Query query = dao.getEM().createQuery("select m from MessageSMIME m where m.eventVS =:eventVS and " +
                "m.userVS =:userVS and m.type=:type").setParameter("eventVS", request.eventVS)
                .setParameter("userVS", userVS).setParameter("type", TypeVS.CLAIM_EVENT_SIGN);
        MessageSMIME smime = dao.getSingleResult(MessageSMIME.class, query);
        if(smime != null && EventVS.Cardinality.EXCLUSIVE.equals(request.eventVS.getCardinality())) {
            throw new ValidationExceptionVS(messages.get("claimSignatureRepeated",
                    userVS.getNif(), request.eventVS.getSubject()));
        }
        if(request.fieldsEventVS != null && !request.fieldsEventVS.isEmpty()) {
            for(Map fieldMap : request.fieldsEventVS) {
                FieldEventVS fieldEventVS = dao.find(FieldEventVS.class, ((Number)fieldMap.get("id")).longValue());
                if (fieldEventVS != null) {
                    dao.persist(new FieldValueEventVS(messageSMIME, fieldEventVS, (String) fieldMap.get("value")));
                } else throw new ValidationExceptionVS("ERROR - signature with unknown field: " + fieldMap);
            }
        }
        String fromUser = config.getServerName();
        String toUser = userVS.getNif();
        String subject = messages.get("mime.subject.claimSignatureValidated");
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned (fromUser, toUser,
                messageSMIME.getSMIME(), subject);
        dao.merge(messageSMIME.setSMIME(receipt).setType(TypeVS.CLAIM_EVENT_SIGN).setEventVS(request.eventVS));
        log.info("save - claim signature OK - signer: " + userVS.getId());
        return messageSMIME;
        /*return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIME, eventVS:request.eventVS,
                messageSMIME: messageSMIME, type:TypeVS.CLAIM_EVENT_SIGN, contentType:ContentTypeVS.JSON_SIGNED)*/
    }

    public EventVSClaimStats getStat (EventVSClaim eventVS) {
        Query query = dao.getEM().createQuery("select count(m) from MessageSMIME m where m.eventVS =:eventVS and " +
                "m.type=:type").setParameter("eventVS", eventVS).setParameter("type", TypeVS.CLAIM_EVENT_SIGN);
        return new EventVSClaimStats(eventVS, (long)query.getSingleResult(), config.getRestURL());
    }

    private class SignedClaimRequest {
        List<Map> fieldsEventVS;
        TypeVS operation;
        Long id;
        EventVSClaim eventVS;
        
        public SignedClaimRequest() {}


        public void validate(Date timeStampDate) throws ValidationExceptionVS {
            if(operation == null || TypeVS.SMIME_CLAIM_SIGNATURE != operation) throw new ValidationExceptionVS(
                    "ERROR - expected operation 'SMIME_CLAIM_SIGNATURE' - found: " + operation);
            eventVS = dao.find(EventVSClaim.class, id);
            if (eventVS == null) throw new ValidationExceptionVS("ERROR - eventVSNotFound - EventVSClaim id: " + id);
            if(!eventVS.isActive(timeStampDate)) {
                throw new ValidationExceptionVS(messages.get("timeStampRangeErrorMsg",
                        DateUtils.getDayWeekDateStr(timeStampDate), DateUtils.getDayWeekDateStr(eventVS.getDateBegin()),
                        DateUtils.getDayWeekDateStr(eventVS.getDateFinish())));
            }
        }
    }

}