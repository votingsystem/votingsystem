package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativeVotingHistoryMetaInf {

    private Long numVotes;
    private Date dateFrom;
    private Date dateTo;
    private String representativeURL;
    private String filePath;
    private String downloadURL;

    public RepresentativeVotingHistoryMetaInf() {}

    public RepresentativeVotingHistoryMetaInf(Long numVotes, Date dateFrom, Date dateTo, String representativeURL,
                  String filePath, String downloadURL) {
        this.numVotes = numVotes;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.representativeURL = representativeURL;
        this.filePath = filePath;
        this.downloadURL = downloadURL;
    }

    public Long getNumVotes() {
        return numVotes;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public String getRepresentativeURL() {
        return representativeURL;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
