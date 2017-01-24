package org.votingsystem.socket;

import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.OperationTypeDto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSession<T> {

    private OperationTypeDto operation;
    private T data;
    private DeviceDto device;
    private String UUID;

    public WebSocketSession(MessageDto socketMsg) {
        this.operation = socketMsg.getOperation();
        this.UUID = socketMsg.getUUID();
    }

    public WebSocketSession(DeviceDto device, T data, OperationTypeDto operation) {
        this.data = data;
        this.device = device;
        this.operation = operation;
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

    public OperationTypeDto getOperation() {
        return operation;
    }

    public WebSocketSession setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public WebSocketSession setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

}