package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.Election;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElectionMetaInf {

    private Long id;
    private String serverURL;
    private String downloadURL;
    private String subject;
    private LocalDateTime dateBegin;
    private LocalDateTime dateFinish;
    private Map backup;

    public ElectionMetaInf() {}


    public ElectionMetaInf(Election eventVS, String contextURL, String downloadURL) {
        this.id = eventVS.getId();
        this.serverURL = contextURL;
        this.downloadURL = downloadURL;
        this.subject = eventVS.getSubject();
        this.dateBegin = eventVS.getDateBegin();
        this.dateFinish = eventVS.getDateFinish();
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

    public LocalDateTime getDateBegin() {
        return dateBegin;
    }

    public LocalDateTime getDateFinish() {
        return dateFinish;
    }

    public String getDownloadURL() {
        return downloadURL;
    }
}
