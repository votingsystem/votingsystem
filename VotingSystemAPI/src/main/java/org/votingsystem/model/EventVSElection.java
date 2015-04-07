package org.votingsystem.model;


import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.StringUtils;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
//@Indexed
@Entity @Table(name="EventVSElection") @DiscriminatorValue("EventVSElection")
public class EventVSElection extends EventVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public FieldEventVS checkOptionId(Long opcionId) {
        if(opcionId == null) return null;
        for(FieldEventVS opcion: getFieldsEventVS()) {
            if(opcionId.longValue() == opcion.getId().longValue()) return opcion;
        }
        return null;
    }

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

    public EventVSElection resetId() {
        setId(null);
        if(getFieldsEventVS() != null) {
            for(FieldEventVS fieldEventVS : getFieldsEventVS()) {
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

}
