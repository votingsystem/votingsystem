package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.TypeVS;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativeDto {

    private TypeVS operation;
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
    private String UUID;
    private String base64Image;
    private UserVS.Type type;

    public RepresentativeDto() {}

    public RepresentativeDto(UserVS userVS, String description) {
        this(userVS);
        this.description = description;
    }

    public RepresentativeDto(UserVS userVS) {
        this.id = userVS.getId();
        this.nif = userVS.getNif();
        this.name = userVS.getName();
        this.description = userVS.getDescription();
        this.firstName = userVS.getFirstName();
        this.lastName = userVS.getLastName();
        this.type = userVS.getType();
    }

    public RepresentativeDto(UserVS userVS, Long smimeActivationId, Long numRepresentations, String contextURL) {
        this(userVS);
        this.numRepresentations = numRepresentations;
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

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
