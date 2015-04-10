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
    private String downloadURL;

    public RepresentativeVotingHistoryMetaInf() {}

    public RepresentativeVotingHistoryMetaInf(Long numVotes, Date dateFrom, Date dateTo, String representativeURL,
                  String downloadURL) {
        this.numVotes = numVotes;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.representativeURL = representativeURL;
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
}
