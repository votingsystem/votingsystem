package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.util.Constants;
import org.votingsystem.util.HashUtils;
import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRMessageDto<T> implements Serializable {

    public static final String TAG = QRMessageDto.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public static final String DEVICE_ID_KEY          = "did";
    public static final String ITEM_ID_KEY            = "iid";
    public static final String OPERATION_CODE_KEY     = "op";
    public static final String PUBLIC_KEY_KEY         = "pk";
    public static final String SYSTEM_ENTITY_ID_KEY   = "eid";
    public static final String MSG_KEY                = "msg";
    public static final String URL_KEY                = "url";
    public static final String QR_UUID_KEY            = "uid";


    @JsonIgnore private OperationType operationType;
    @JsonIgnore private T data;
    @JsonIgnore private String originRevocationHash;
    @JsonIgnore private DeviceDto device;
    @JsonIgnore private AESParamsDto aesParams;;

    private String systemEntityID;
    private String operationCode;
    private Long deviceId;
    private String itemId;
    private Date dateCreated;
    private String revocationHashBase64;
    private String publicKeyBase64;
    private String url;
    private String msg;
    private String UUID;

    public QRMessageDto() {}

    public QRMessageDto(DeviceDto deviceDto, OperationType operationType){
        this.operationType = operationType;
        this.deviceId = deviceDto.getId();
        this.dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }

    public static QRMessageDto FROM_QR_CODE(String msg) {
        QRMessageDto qrMessageDto = new QRMessageDto();
        if (msg.contains(DEVICE_ID_KEY + "="))
            qrMessageDto.setDeviceId(Long.valueOf(msg.split(DEVICE_ID_KEY + "=")[1].split(";")[0]));
        if (msg.contains(ITEM_ID_KEY + "="))
            qrMessageDto.setItemId(msg.split(ITEM_ID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(SYSTEM_ENTITY_ID_KEY + "="))
            qrMessageDto.setSystemEntityID(msg.split(SYSTEM_ENTITY_ID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(OPERATION_CODE_KEY + "="))
            qrMessageDto.setOperationCode(msg.split(OPERATION_CODE_KEY + "=")[1].split(";")[0]);
        if (msg.contains(PUBLIC_KEY_KEY + "="))
            qrMessageDto.setPublicKeyBase64(msg.split(PUBLIC_KEY_KEY + "=")[1].split(";")[0]);
        if (msg.contains(MSG_KEY + "="))
            qrMessageDto.setMsg(msg.split(MSG_KEY + "=")[1].split(";")[0]);
        if (msg.contains(QR_UUID_KEY + "="))
            qrMessageDto.setUUID(msg.split(QR_UUID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(URL_KEY + "="))
            qrMessageDto.setUrl(msg.split(URL_KEY + "=")[1].split(";")[0]);
        return qrMessageDto;
    }

    @JsonIgnore
    public PublicKey getRSAPublicKey() throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
        //fix qr codes replacements of '+' with spaces
        publicKeyBase64 = publicKeyBase64.replace(" ", "+");
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(java.util.Base64.getDecoder().decode(publicKeyBase64));
        return factory.generatePublic(pubKeySpec);
    }

    @JsonIgnore
    public DeviceDto getDevice() throws Exception {
        if(device != null) return device;
        DeviceDto dto = new DeviceDto(deviceId);
        if(publicKeyBase64 != null) dto.setPublicKey(getRSAPublicKey());
        return dto;
    }
    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    public static QRMessageDto FROM_URL(String url) throws NoSuchAlgorithmException {
        QRMessageDto result = new QRMessageDto();
        result.setUrl(url);
        result.createRequest();
        return result;
    }

    public QRMessageDto createRequest() throws NoSuchAlgorithmException {
        this.originRevocationHash = java.util.UUID.randomUUID().toString();
        this.revocationHashBase64 = HashUtils.getHashBase64(originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public QRMessageDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String revocationHashBase64) {
        this.revocationHashBase64 = revocationHashBase64;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
        if(this.operationCode != null) this.operationCode = this.operationCode.toUpperCase();
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String key) {
        this.publicKeyBase64 = key;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public void setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSystemEntityID() {
        return systemEntityID;
    }

    public void setSystemEntityID(String systemEntityID) {
        this.systemEntityID = systemEntityID;
    }


}