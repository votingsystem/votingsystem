package org.votingsystem.qr;

import org.votingsystem.dto.Dto;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.http.HttpResponse;
import org.votingsystem.util.OperationType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRRequestBundle<T extends Dto> {

    private OperationType operationType;
    private IdentityRequestDto identityRequest;
    private T reponseDto;
    private String base64Data;


    public QRRequestBundle() {}

    public QRRequestBundle(OperationType operationType, T reponseDto) {
        this.operationType = operationType;
        this.reponseDto = reponseDto;
        this.identityRequest = identityRequest;
    }

    public IdentityRequestDto getIdentityRequest() {
        return identityRequest;
    }

    public QRRequestBundle setIdentityRequest(IdentityRequestDto identityRequest) {
        this.identityRequest = identityRequest;
        return this;
    }

    public T getResponseDto() {
        return reponseDto;
    }

    public QRRequestBundle setResponseDto(T reponseDto) {
        this.reponseDto = reponseDto;
        return this;
    }

    public byte[] generateResponse(HttpServletRequest req, LocalDateTime localDateTime) throws IOException, ServletException {
        QRResponseDto qrResponseDto = new QRResponseDto(operationType, localDateTime);
        if(reponseDto != null)
            qrResponseDto.setBase64Data(Base64.getEncoder().encodeToString(HttpResponse.getResponseContent(req, reponseDto)));
        else
            qrResponseDto.setBase64Data(base64Data);
        return HttpResponse.getResponseContent(req, qrResponseDto);
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public QRRequestBundle setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }
}