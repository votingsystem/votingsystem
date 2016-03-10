package org.votingsystem.model.voting;


import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ValidationExceptionVS;

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

    @OneToOne private CertificateVS accessControlCert;
    @OneToOne private CertificateVS controlCenterCert;

    public EventElection() {}

    public EventElection(String subject, String content, Cardinality cardinality, UserVS userVS,
                   ControlCenterVS controlCenterVS, Date dateBegin, Date dateFinish) {
        setSubject(subject);
        setContent(content);
        setCardinality(cardinality);
        setUserVS(userVS);
        setControlCenterVS(controlCenterVS);
        setDateBegin(dateBegin);
        setDateFinish(dateFinish);
    }

    public EventElection(Long accessControlEventId, String subject, String content, String URL,
                   AccessControlVS accessControl, UserVS userVS, Date dateBegin, Date dateFinish) {
        setAccessControlEventId(accessControlEventId);
        setSubject(subject);
        setContent(content);
        setUrl(URL);
        setAccessControlVS(accessControl);
        setUserVS(userVS);
        setDateBegin(dateBegin);
        setDateFinish(dateFinish);
    }

    public FieldEventVS checkOptionId(Long optionId) throws ValidationExceptionVS {
        for(FieldEventVS option: getFieldsEventVS()) {
            if(optionId.longValue() == option.getId().longValue()) return option;
        }
        throw new ValidationExceptionVS("FieldEventVS not found - id: " + optionId);
    }

    public EventElection updateAccessControlIds() {
        setId(null);
        if(getFieldsEventVS() != null) {
            for(FieldEventVS fieldEventVS : getFieldsEventVS()) {
                fieldEventVS.setAccessControlFieldEventId(fieldEventVS.getId());
                fieldEventVS.setId(null);
            }
        }
        return this;
    }

    public CertificateVS getAccessControlCert() {
        return accessControlCert;
    }

    public EventElection setAccessControlCert(CertificateVS accessControlCert) {
        this.accessControlCert = accessControlCert;
        return this;
    }

    public CertificateVS getControlCenterCert() {
        return controlCenterCert;
    }

    public EventElection setControlCenterCert(CertificateVS controlCenterCert) {
        this.controlCenterCert = controlCenterCert;
        return this;
    }
}
