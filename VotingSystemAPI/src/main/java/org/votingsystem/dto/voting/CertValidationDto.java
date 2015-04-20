package org.votingsystem.dto.voting;


import org.votingsystem.util.TypeVS;

public class CertValidationDto {

    private TypeVS operation = TypeVS.CERT_USER_NEW;
    private String deviceId;
    private String nif;
    private String UUID;

    public CertValidationDto() {}


    public static CertValidationDto validationRequest(String nif, String deviceId) {
        CertValidationDto certValidationDto = new CertValidationDto();
        certValidationDto.setNif("7553172H");
        certValidationDto.setDeviceId("aee09e79-e44e-4a86-9a5d-0fd1ee445038");
        certValidationDto.setUUID(java.util.UUID.randomUUID().toString());
        return certValidationDto;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
