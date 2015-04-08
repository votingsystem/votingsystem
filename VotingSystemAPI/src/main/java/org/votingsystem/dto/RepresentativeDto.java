package org.votingsystem.dto;

import org.votingsystem.model.UserVS;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeDto {

    private Long id;
    private Long numRepresentations ;
    private String nif;
    private String name;
    private String firstName;
    private String lastName;
    private String description;
    private String URL;
    private String representativeMessageURL;
    private String imageURL;
    private UserVS.Type type;

    public RepresentativeDto() {}

    public RepresentativeDto(UserVS userVS, Long smimeActivationId, Long numRepresentations, String contextURL) {
        this.id = userVS.getId();
        this.numRepresentations = numRepresentations;
        this.nif = userVS.getNif();
        this.name = userVS.getName();
        this.firstName = userVS.getFirstName();
        this.lastName = userVS.getLastName();
        this.description = userVS.getDescription();
        this.type = userVS.getType();
        this.URL = format("{0}/representative/id/{1}", contextURL, userVS.getId());
        this.representativeMessageURL = format("{0}/messageSMIME/id/{1}", contextURL, smimeActivationId);
        this.imageURL = format("{0}/representative/id/{1}/image", contextURL, userVS.getId());
    }

    public Long getId() {
        return id;
    }

    public Long getNumRepresentations() {
        return numRepresentations;
    }

    public String getNif() {
        return nif;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDescription() {
        return description;
    }

    public String getURL() {
        return URL;
    }

    public String getRepresentativeMessageURL() {
        return representativeMessageURL;
    }

    public String getImageURL() {
        return imageURL;
    }

    public UserVS.Type getType() {
        return type;
    }
}
