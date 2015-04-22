package org.votingsystem.dto;


public class AESParamsDto {

    private String key;
    private String iv;

    public AESParamsDto(String key, String iv) {
        this.key = key;
        this.iv = iv;
    }

    public AESParamsDto() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }
}
