package org.votingsystem.json;

import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeAccreditationsMetaInf {

    private Long numAccreditations;
    private Date selectedDate;
    private String representativeURL;
    private String downloadURL;

    public RepresentativeAccreditationsMetaInf() {}

    public RepresentativeAccreditationsMetaInf(Long numAccreditations, Date selectedDate, String representativeURL,
                                               String downloadURL) {
        this.numAccreditations = numAccreditations;
        this.selectedDate = selectedDate;
        this.representativeURL = representativeURL;
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
}
