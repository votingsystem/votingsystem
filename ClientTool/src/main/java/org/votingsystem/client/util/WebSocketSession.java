package org.votingsystem.client.util;

import org.votingsystem.model.DeviceVS;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.util.TypeVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSession<T> {

    private TypeVS typeVS;
    private AESParams aesParams;
    private T data;
    private DeviceVS deviceVS;
    private String UUID;

    public WebSocketSession(AESParams aesParams, DeviceVS deviceVS, T data, TypeVS typeVS) {
        this.aesParams = aesParams;
        this.data = data;
        this.deviceVS = deviceVS;
        this.typeVS = typeVS;
    }

    public AESParams getAESParams() {
        return aesParams;
    }

    public void setAESParams(AESParams aesParams) {
        this.aesParams = aesParams;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public DeviceVS getDeviceVS() {
        return deviceVS;
    }

    public void setDeviceVS(DeviceVS deviceVS) {
        this.deviceVS = deviceVS;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public String getUUID() {
        return UUID;
    }

    public WebSocketSession setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

}