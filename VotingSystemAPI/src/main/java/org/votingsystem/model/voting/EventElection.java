package org.votingsystem.model.voting;


import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.util.Date;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
//@Indexed
@Entity @DiscriminatorValue("EventElection")
public class EventElection extends EventVS implements Serializable {

    private static final long serialVersionUID = 1L;

    @OneToOne private Certificate accessControlCert;
    @OneToOne private Certificate controlCenterCert;

    public EventElection() {}

    public EventElection(String subject, String content, Cardinality cardinality, User user,
                         ControlCenter controlCenter, Date dateBegin, Date dateFinish) {
        setSubject(subject);
        setContent(content);
        setCardinality(cardinality);
        setUser(user);
        setControlCenter(controlCenter);
        setDateBegin(dateBegin);
        setDateFinish(dateFinish);
    }

    public EventElection(Long accessControlEventId, String subject, String content, String URL,
                         AccessControl accessControl, User user, Date dateBegin, Date dateFinish) {
        setAccessControlEventId(accessControlEventId);
        setSubject(subject);
        setContent(content);
        setUrl(URL);
        setAccessControl(accessControl);
        setUser(user);
        setDateBegin(dateBegin);
        setDateFinish(dateFinish);
    }

    public FieldEvent checkOptionId(Long optionId) throws ValidationException {
        for(FieldEvent option: getFieldsEventVS()) {
            if(optionId.longValue() == option.getId().longValue()) return option;
        }
        throw new ValidationException("FieldEvent not found - id: " + optionId);
    }

    public EventElection updateAccessControlIds() {
        setId(null);
        if(getFieldsEventVS() != null) {
            for(FieldEvent fieldEvent : getFieldsEventVS()) {
                fieldEvent.setAccessControlFieldEventId(fieldEvent.getId());
                fieldEvent.setId(null);
            }
        }
        return this;
    }

    public Certificate getAccessControlCert() {
        return accessControlCert;
    }

    public EventElection setAccessControlCert(Certificate accessControlCert) {
        this.accessControlCert = accessControlCert;
        return this;
    }

    public Certificate getControlCenterCert() {
        return controlCenterCert;
    }

    public EventElection setControlCenterCert(Certificate controlCenterCert) {
        this.controlCenterCert = controlCenterCert;
        return this;
    }
}
