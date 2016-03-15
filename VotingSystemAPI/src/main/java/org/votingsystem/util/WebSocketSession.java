package org.votingsystem.util;

import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.SocketMessageDto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSession<T> {

    private TypeVS typeVS;
    private T data;
    private DeviceDto device;
    private String UUID;

    public WebSocketSession(SocketMessageDto socketMsg) {
        this.typeVS = socketMsg.getOperation();
        this.UUID = socketMsg.getUUID();
    }

    public WebSocketSession(DeviceDto device, T data, TypeVS typeVS) {
        this.data = data;
        this.device = device;
        this.typeVS = typeVS;
    }

    public WebSocketSession(DeviceDto device) {
        this.device = device;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public DeviceDto getDevice() {
        return device;
    }

    public void setDevice(DeviceDto device) {
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