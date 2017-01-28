package org.votingsystem.androidcurrency.util;

import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.util.CurrencyOperation;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionInfo implements Serializable {

    private static final Logger log = Logger.getLogger(SessionInfo.class.getName());

    private DeviceDto sessionDevice;
    private UserDto user;
    private CertificationRequest mobileCsrReq;
    private CertificationRequest browserCsrReq;
    private SessionCertificationDto sessionCertification;
    private String entityId;


    public SessionInfo() {}

    public SessionInfo(String entityId, CertificationRequest mobileCsrReq, CertificationRequest browserCsrReq) {
        this.entityId = entityId;
        this.mobileCsrReq = mobileCsrReq;
        this.browserCsrReq = browserCsrReq;
    }

    public CertificationRequest getMobileCsrReq() {
        return mobileCsrReq;
    }

    public SessionInfo setMobileCsrReq(CertificationRequest mobileCsrReq) {
        this.mobileCsrReq = mobileCsrReq;
        return this;
    }

    public CertificationRequest getBrowserCsrReq() {
        return browserCsrReq;
    }

    public SessionInfo setBrowserCsrReq(CertificationRequest browserCsrReq) {
        this.browserCsrReq = browserCsrReq;
        return this;
    }

    public DeviceDto getSessionDevice() {
        return sessionDevice;
    }

    public SessionInfo setSessionDevice(DeviceDto sessionDevice) {
        this.sessionDevice = sessionDevice;
        return this;
    }

    public UserDto getUser() {
        return user;
    }

    public SessionInfo setUser(UserDto user) {
        this.user = user;
        return this;
    }

    public SessionCertificationDto getSessionCertification() {
        return sessionCertification;
    }

    public String getEntityId() {
        return entityId;
    }

    public SessionInfo setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public void loadIssuedCerts(SessionCertificationDto certificationDto) {
        sessionCertification = certificationDto;
        mobileCsrReq.setSignedCsr(certificationDto.getMobileCsrSigned().getBytes());
        browserCsrReq.setSignedCsr(certificationDto.getBrowserCsrSigned().getBytes());
    }

    public SessionCertificationDto buildBrowserCertificationDto() throws Exception {
        SessionCertificationDto sessionCertificationDto = new SessionCertificationDto();
        sessionCertificationDto.setOperation(new OperationTypeDto(CurrencyOperation.SESSION_CERTIFICATION, entityId))
                .setPrivateKeyPEM(new String(PEMUtils.getPEMEncoded(browserCsrReq.getPrivateKey())))
                .setMobileUUID(sessionCertification.getMobileUUID())
                .setMobileCsrSigned(sessionCertification.getMobileCsrSigned())
                .setBrowserUUID(sessionCertification.getBrowserUUID())
                .setBrowserCsrSigned(sessionCertification.getBrowserCsrSigned())
                .setUser(sessionCertification.getUser());
        return sessionCertificationDto;
    }

}