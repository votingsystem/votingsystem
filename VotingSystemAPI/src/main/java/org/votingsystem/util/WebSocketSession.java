package org.votingsystem.util;

import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.Device;
import org.votingsystem.util.crypto.AESParams;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSession<T> {

    private TypeVS typeVS;
    private AESParams aesParams;
    private T data;
    private Device device;
    private String UUID;

    public WebSocketSession(SocketMessageDto socketMsg) {
        this.aesParams = socketMsg.getAesEncryptParams();
        this.typeVS = socketMsg.getOperation();
        this.UUID = socketMsg.getUUID();
    }

    public WebSocketSession(AESParams aesParams, Device device, T data, TypeVS typeVS) {
        this.aesParams = aesParams;
        this.data = data;
        this.device = device;
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

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
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