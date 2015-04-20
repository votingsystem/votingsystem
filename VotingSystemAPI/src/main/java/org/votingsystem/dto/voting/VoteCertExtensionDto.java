package org.votingsystem.dto.voting;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteCertExtensionDto {

    private String accessControlURL;
    private String hashCertVS;
    private Long eventId;

    public VoteCertExtensionDto() {}

    public VoteCertExtensionDto(String accessControlURL, String hashCertVS, Long eventId) {
        this.accessControlURL = accessControlURL;
        this.hashCertVS = hashCertVS;
        this.eventId = eventId;
    }


    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
