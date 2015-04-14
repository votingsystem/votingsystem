package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.util.TypeVS;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSMetaInf {

    private Long id;
    private String serverURL;
    private String downloadURL;
    private String subject;
    private Date dateBegin;
    private Date dateFinish;
    private TypeVS type;
    private Map backup;

    public EventVSMetaInf() {}


    public EventVSMetaInf(EventVS eventVS, String contextURL, String downloadURL) {
        this.id = eventVS.getId();
        this.serverURL = contextURL;
        this.downloadURL = downloadURL;
        this.subject = eventVS.getSubject();
        this.dateBegin = eventVS.getDateBegin();
        this.dateFinish = eventVS.getDateFinish();
        this.type = TypeVS.VOTING_EVENT;
    }

    public void setBackupData(Long numVotes, Long numAccessRequest) {
        backup = new HashMap<>();
        backup.put("numVotes", numVotes);
        backup.put("numAccessRequest", numAccessRequest);
    }

    public void setClaimBackupData(Long numVotes) {
        backup = new HashMap<>();
        backup.put("numSignatures", numVotes);
    }

    public Long getId() {
        return id;
    }

    public String getServerURL() {
        return serverURL;
    }

    public String getSubject() {
        return subject;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public TypeVS getType() {
        return type;
    }

    public String getDownloadURL() {
        return downloadURL;
    }
}
