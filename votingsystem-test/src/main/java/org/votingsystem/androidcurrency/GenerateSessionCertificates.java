package org.votingsystem.androidcurrency;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.votingsystem.BaseTest;
import org.votingsystem.Constants;
import org.votingsystem.androidcurrency.util.SessionInfo;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.*;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.pkcs7.SignatureBuilder;
import org.votingsystem.socket.SocketOperation;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class GenerateSessionCertificates extends BaseTest {

    private static final Logger log = Logger.getLogger(GenerateSessionCertificates.class.getName());

    private static final String QR_CODE = "eid=https://voting.ddns.net/currency-server;op=0;uid=2f38674d-ab16-418f-8b0b-39ae84d14412;";

    private static CMSSignedMessage sessionCertification;


    public static void main(String[] args) throws Exception {
        GenerateSessionCertificates genSessionCert = new GenerateSessionCertificates();
        SessionCertificationDto certificationDto = genSessionCert.buildIdentificationRequest();
        genSessionCert.sendMessageToBrowser(certificationDto);
        System.exit(0);
    }

    public GenerateSessionCertificates() { }

    private SessionCertificationDto buildIdentificationRequest() throws Exception {
        SessionCertificationDto result = null;
        try {
            MockDNIe mockDNIe = new MockDNIe("08888888D");
            SignatureBuilder signatureService = new SignatureBuilder(mockDNIe);
            User user = User.FROM_CERT(mockDNIe.getX509Certificate(), User.Type.CURRENCY_SERVER);
            String mobileUUID = UUID.randomUUID().toString();
            CertificationRequest mobileCsrReq = CertificationRequest.getUserRequest(
                    user.getNumId(), user.getEmail(), user.getPhone(), "test-application",
                    mobileUUID, user.getName(), user.getSurname(), Device.Type.MOBILE);

            String browserUUID = UUID.randomUUID().toString();
            CertificationRequest browserCsrReq = CertificationRequest.getUserRequest(
                    user.getNumId(), user.getEmail(), user.getPhone(), "test-application",
                    browserUUID, user.getName(), user.getSurname(), Device.Type.BROWSER);

            SessionCertificationDto sessionCertDto = new SessionCertificationDto(new UserDto(user),
                    new String(mobileCsrReq.getCsrPEM()), mobileUUID,
                    new String(browserCsrReq.getCsrPEM()), browserUUID).setOperation(
                    new OperationTypeDto(CurrencyOperation.SESSION_CERTIFICATION, null));

            byte[] resultBytes = JSON.getMapper().writeValueAsBytes(sessionCertDto);

            CMSSignedMessage cmsSignedMessage = signatureService.signDataWithTimeStamp(resultBytes);
            byte[] signedMessageBytes = cmsSignedMessage.toPEM();
            log.info("cmsSignedMessage: " + new String(signedMessageBytes));

            ResponseDto response = HttpConn.getInstance().doPostRequest(signedMessageBytes, MediaType.PKCS7_SIGNED,
                    CurrencyOperation.SESSION_CERTIFICATION.getUrl(Constants.ID_PROVIDER_ENTITY_ID));
            if(ResponseDto.SC_OK != response.getStatusCode()) {
                log.info("status: " + response.getStatusCode() + " - response message: " + response.getMessage());
                System.exit(0);
            }
            sessionCertification = CMSSignedMessage.FROM_PEM(response.getMessageBytes());
            SessionCertificationDto certificationResponse = JSON.getMapper().readValue(
                    sessionCertification.getSignedContentStr(), SessionCertificationDto.class);

            SessionInfo sessionInfo = new SessionInfo(Constants.ID_PROVIDER_ENTITY_ID, mobileCsrReq, browserCsrReq);
            sessionInfo.loadIssuedCerts(certificationResponse);
            log.info("BrowserCsrSigned: " + certificationResponse.getBrowserCsrSigned());

            result = sessionInfo.buildBrowserCertificationDto();
            log.info("sessionCertificationDto: " + JSON.getMapper().writeValueAsString(result));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private void sendMessageToBrowser(SessionCertificationDto certificationDto) throws Exception {
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("UUID", qrMessageDto.getUUID()));
        urlParameters.add(new BasicNameValuePair("operation", qrMessageDto.getOperation()));
        ResponseDto responseDto = HttpConn.getInstance().doPostForm(
                CurrencyOperation.QR_INFO.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID), urlParameters);
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("status: " + responseDto.getStatusCode() + " - responseDto: " + responseDto.getMessage());
            System.exit(0);
        }
        SessionCertificationDto browserPublicKey = JSON.getMapper().readValue(responseDto.getMessageBytes(),
                SessionCertificationDto.class);
        PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(browserPublicKey.getPublicKeyPEM());

        MessageDto messageDto = new MessageDto();
        messageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE).setDeviceToUUID(qrMessageDto.getUUID());

        MessageDto messageContent = new MessageDto();
        messageContent.setOperation(new OperationTypeDto(CurrencyOperation.SESSION_CERTIFICATION, null)).setMessage("");

        String base64Data = Base64.getEncoder().encodeToString(
                JSON.getMapper().writeValueAsString(certificationDto).getBytes());
        messageContent.setBase64Data(base64Data);

        byte[] encryptedMessage = Encryptor.encryptToCMS(JSON.getMapper().writeValueAsBytes(messageContent), publicKey);
        messageDto.setEncryptedMessage(new String(encryptedMessage));

        urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("browserUUID", qrMessageDto.getUUID()));
        urlParameters.add(new BasicNameValuePair("cmsMessage", new String(sessionCertification.toPEM())));
        urlParameters.add(new BasicNameValuePair("socketMsg", JSON.getMapper().writeValueAsString(messageDto)));
        responseDto = HttpConn.getInstance().doPostForm(
                CurrencyOperation.SESSION_CERTIFICATION_DATA.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID), urlParameters);
        log.info("message: " + responseDto.getMessage());
    }

}