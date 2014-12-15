package org.votingsystem.signature.util;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionVS<T> {

    private AESParams aesParams;
    private T data;

    public SessionVS(AESParams aesParams) {
        this.aesParams = aesParams;
    }

    public SessionVS(AESParams aesParams, T data) {
        this.aesParams = aesParams;
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public AESParams getAESParams() {
        return aesParams;
    }

    public void setAESParams(AESParams aesParams) {
        this.aesParams = aesParams;
    }
}
