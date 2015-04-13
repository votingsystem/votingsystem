package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativeAccreditationsMetaInf {

    private Long numAccreditations;
    private Date selectedDate;
    private String representativeURL;
    private String downloadURL;
    private String filePath;

    public RepresentativeAccreditationsMetaInf() {}

    public RepresentativeAccreditationsMetaInf(Long numAccreditations, Date selectedDate, String representativeURL,
               String filePath, String downloadURL) {
        this.numAccreditations = numAccreditations;
        this.selectedDate = selectedDate;
        this.representativeURL = representativeURL;
        this.filePath = filePath;
        this.downloadURL = downloadURL;
    }

    public Long getNumAccreditations() {
        return numAccreditations;
    }

    public Date getSelectedDate() {
        return selectedDate;
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
