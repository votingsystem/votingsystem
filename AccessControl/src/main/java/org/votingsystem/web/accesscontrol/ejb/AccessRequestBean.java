package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessRequest;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.crypto.CsrResponse;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.logging.Logger;

@Stateless
public class AccessRequestBean {

    private static Logger log = Logger.getLogger(AccessRequestBean.class.getName());

    @Inject DAOBean dao;
    @Inject CSRBean csrBean;
    @Inject ConfigVS config;

    @Transactional
    public CsrResponse saveRequest(CMSMessage cmsMessage, byte[] csr) throws Exception {
        UserVS signer = cmsMessage.getUserVS();
        AccessRequestDto request =  cmsMessage.getSignedContent(AccessRequestDto.class);
        validateAccessRequest(request, signer.getTimeStampToken().getTimeStampInfo().getGenTime());
        Query query = dao.getEM().createQuery("select a from AccessRequest a where a.userVS =:userVS and " +
                "a.eventVS =:eventVS and a.state =:state").setParameter("userVS", signer)
                .setParameter("eventVS", request.getEventVS()).setParameter("state", AccessRequest.State.OK);
        AccessRequest accessRequest = dao.getSingleResult(AccessRequest.class, query);
        if (accessRequest != null){
            throw new ExceptionVS(MessageDto.REQUEST_REPEATED(null, config.getContextURL() + "/rest/cmsMessage/id/" +
                    accessRequest.getCmsMessage().getId()));
        } else {
            accessRequest = dao.persist(new AccessRequest(signer, cmsMessage, AccessRequest.State.OK,
                    request.getHashAccessRequestBase64(), request.getEventVS()));
            request.setAccessRequest(accessRequest);
            CsrResponse csrResponse = null;
            try {
                if(signer.getType() == UserVS.Type.REPRESENTATIVE) {
                    csrResponse = csrBean.signRepresentativeCertVote(csr, request.getEventVS(), signer);
                } else csrResponse = csrBean.signCertVote(csr, request.getEventVS());
            } catch (Exception ex) {
                if(accessRequest != null && accessRequest.getId() != null && accessRequest.getState()
                        == AccessRequest.State.OK) {
                    accessRequest.setMetaInf(ex.getMessage());
                    dao.merge(accessRequest.setState(AccessRequest.State.CANCELED));
                }
                throw ex;
            }
            return csrResponse;
        }
    }

    private void validateAccessRequest(AccessRequestDto accessRequestDto, Date timeStampDate) throws ValidationExceptionVS {
        if(accessRequestDto.getEventId() == null) throw new ValidationExceptionVS("missing param 'eventId'");
        if(accessRequestDto.getEventURL() == null) throw new ValidationExceptionVS("missing param 'eventURL'");
        if(accessRequestDto.getHashAccessRequestBase64() == null) throw new ValidationExceptionVS("missing param 'hashAccessRequestBase64'");
        EventElection eventVS = dao.find(EventElection.class, accessRequestDto.getEventId());
        if(eventVS == null) throw new ValidationExceptionVS("eventVSNotFound - eventId: " + accessRequestDto.getEventId());
        if(!eventVS.isActive(timeStampDate)) {
            throw new ValidationExceptionVS("timeStampRangeErrorMsg - timeStampDate: " + timeStampDate +
                    " - range: [" + eventVS.getDateBegin() + " - " + eventVS.getDateFinish() + "]");
        }
        Query query = dao.getEM().createQuery("select a from AccessRequest a " + "where a.hashAccessRequestBase64 =:hashAccessRequestBase64")
                .setParameter("hashAccessRequestBase64", accessRequestDto.getHashAccessRequestBase64());
        AccessRequest accessRequest = dao.getSingleResult(AccessRequest.class, query);
        if (accessRequest != null) {
            throw new ValidationExceptionVS("ERROR - AccessRequest repeated -  hashRepeated:" +
                    accessRequestDto.getHashAccessRequestBase64());
        }
        accessRequestDto.setAccessRequest(accessRequest);
        accessRequestDto.setEventVS(eventVS);
    }

}
