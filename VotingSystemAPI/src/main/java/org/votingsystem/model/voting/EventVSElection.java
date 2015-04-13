package org.votingsystem.model.voting;


import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ValidationExceptionVS;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
//@Indexed
@Entity @Table(name="EventVSElection") @DiscriminatorValue("EventVSElection")
public class EventVSElection extends EventVS implements Serializable {

    private static final long serialVersionUID = 1L;

    @OneToOne private CertificateVS accessControlCert;
    @OneToOne private CertificateVS controlCenterCert;

    public EventVSElection() {}

    public EventVSElection(String subject, String content, Cardinality cardinality, UserVS userVS,
                   ControlCenterVS controlCenterVS, Date dateBegin, Date dateFinish) {
        setSubject(subject);
        setContent(content);
        setCardinality(cardinality);
        setUserVS(userVS);
        setControlCenterVS(controlCenterVS);
        setDateBegin(dateBegin);
        setDateFinish(dateFinish);
    }

    public EventVSElection(Long accessControlEventVSId, String subject, String content, String URL,
                   AccessControlVS accessControl, UserVS userVS, Date dateBegin, Date dateFinish) {
        setAccessControlEventVSId(accessControlEventVSId);
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

    public EventVSElection updateAccessControlIds() {
        setId(null);
        if(getFieldsEventVS() != null) {
            for(FieldEventVS fieldEventVS : getFieldsEventVS()) {
                fieldEventVS.setAccessControlFieldEventId(fieldEventVS.getId());
                fieldEventVS.setId(null);
            }
        }
        if(getTagVSSet() != null) {
            for(TagVS tagVS : getTagVSSet()) {
                tagVS.setId(null);
            }
        }
        return this;
    }

    public CertificateVS getAccessControlCert() {
        return accessControlCert;
    }

    public EventVSElection setAccessControlCert(CertificateVS accessControlCert) {
        this.accessControlCert = accessControlCert;
        return this;
    }

    public CertificateVS getControlCenterCert() {
        return controlCenterCert;
    }

    public EventVSElection setControlCenterCert(CertificateVS controlCenterCert) {
        this.controlCenterCert = controlCenterCert;
        return this;
    }
}
