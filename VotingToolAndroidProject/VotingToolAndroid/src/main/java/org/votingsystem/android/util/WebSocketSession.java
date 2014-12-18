package org.votingsystem.android.util;

import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.AESParams;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSession<T> {

    private TypeVS typeVS;
    private AESParams aesParams;
    private T data;
    private DeviceVS deviceVS;

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

}
