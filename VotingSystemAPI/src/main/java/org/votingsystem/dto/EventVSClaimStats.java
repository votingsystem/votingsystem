package org.votingsystem.dto;

import org.votingsystem.model.EventVS;
import org.votingsystem.model.EventVSClaim;
import org.votingsystem.model.FieldEventVS;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventVSClaimStats {

    private Long id;
    private Long numSignatures;
    private List<String> fieldsEventVS;
    private String subject;
    private String URL;
    private String publishRequestURL;
    private Date dateBegin;
    private Date dateFinish;
    private EventVS.State state;

    public EventVSClaimStats () {}

    public EventVSClaimStats (EventVSClaim eventVS, Long numSignatures, String contextURL) {
        this.id = eventVS.getId();
        this.numSignatures = numSignatures;
        this.subject = eventVS.getSubject();
        this.state = eventVS.getState();
        this.URL = contextURL + "/eventVSClaim/id/" + this.id;
        this.publishRequestURL = contextURL + "/eventVSClaim/id/" + this.id + "/publishRequest";
        this.dateBegin = eventVS.getDateBegin();
        this.dateFinish = eventVS.getDateFinish();
        if(eventVS.getFieldsEventVS() != null && !eventVS.getFieldsEventVS().isEmpty()) {
            fieldsEventVS = new ArrayList<>();
            for(FieldEventVS fieldEventVS : eventVS.getFieldsEventVS()) {
                fieldsEventVS.add(fieldEventVS.getContent());
            }
        }
    }

    public Long getId() {
        return id;
    }

    public Long getNumSignatures() {
        return numSignatures;
    }

    public List<String> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public String getSubject() {
        return subject;
    }

    public String getURL() {
        return URL;
    }

    public String getPublishRequestURL() {
        return publishRequestURL;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public EventVS.State getState() {
        return state;
    }

}
