package org.votingsystem.test.android;

import org.apache.commons.collections.map.HashedMap;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.KeyStoreUtils;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.VoteContainer;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.test.android.xml.XmlReader;
import org.votingsystem.test.android.xml.XmlWriter;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.testlib.util.XMLUtils;
import org.votingsystem.testlib.xml.SignatureAlgorithm;
import org.votingsystem.testlib.xml.XAdESUtils;
import org.votingsystem.testlib.xml.XMLSignatureBuilder;
import org.votingsystem.util.Constants;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.logging.Logger;

import static org.votingsystem.util.Constants.ANON_CERTIFICATE_REQUEST_FILE_NAME;
import static org.votingsystem.util.Constants.CSR_FILE_NAME;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteTest extends BaseTest {

    private static final Logger log = Logger.getLogger(VoteTest.class.getName());

    private static final String QR_CODE = "eid=https://voting.ddns.net/idprovider;uid=232585a0-426c-4193-aab4-1b24021e26f8;";
    private static final String KEYSTORE_PATH = "./vote.jks";

    public static void main(String[] args) throws Exception {
        new VoteTest().vote();
        System.exit(0);
    }

    public VoteTest() {
        super();
    }

    public void vote() throws Exception {
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        String serviceURL = OperationType.GET_QR_INFO.getUrl(qrMessageDto.getSystemEntityID());
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(qrMessageDto.getUUID().getBytes(), null, serviceURL);

        log.info("electionXML: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }
        QRResponseDto qrResponseDto = XML.getMapper().readValue(responseDto.getMessageBytes(), QRResponseDto.class);
        log.info("data: " + new String(qrResponseDto.getData()));
        ElectionDto electionDto = XmlReader.readElection(qrResponseDto.getData());
        ElectionOptionDto electionOption = electionDto.getElectionOptions().iterator().next();
        log.info("selected option: " + electionOption.getContent() + " - election UUID: " + electionDto.getUUID());
        VoteContainer voteContainer = VoteContainer.generate(electionDto, electionOption, qrMessageDto.getSystemEntityID());
        CertificationRequest certificationRequest = voteContainer.getCertificationRequest();

        //log.info("Csr PEM: " + new String(voteContainer.getCertificationRequest().getCsrPEM()));

        IdentityRequestDto identityRequest = new IdentityRequestDto();

        SystemEntityDto identityEntity = new SystemEntityDto(
                org.votingsystem.test.Constants.ID_PROVIDER_ENTITY_ID, SystemEntityType.ID_PROVIDER);
        SystemEntityDto callbackEntity = new SystemEntityDto(
                org.votingsystem.test.Constants.VOTING_SERVICE_ENTITY_ID, SystemEntityType.VOTING_SERVICE_PROVIDER);
        identityRequest.setIndentityServiceEntity(identityEntity).setCallbackServiceEntityId(callbackEntity);
        identityRequest.setUUID(electionDto.getUUID()).setType(OperationType.ANON_VOTE_CERT_REQUEST);
        identityRequest.setRevocationHashBase64(voteContainer.getRevocationHashBase64());

        byte[] xmlRequest = XmlWriter.write(identityRequest);
        String textToSign = XMLUtils.prepareRequestToSign(xmlRequest);
        log.info("identityRequest: " + textToSign);

        byte[] signatureBytes = new XMLSignatureBuilder(textToSign.getBytes(),
                XAdESUtils.XML_MIME_TYPE, SignatureAlgorithm.RSA_SHA_256.getName(), new MockDNIe("08888888D"),
                org.votingsystem.test.Constants.TIMESTAMP_SERVICE_URL).build();

        Map<String, byte[]> fileMap = new HashedMap();
        fileMap.put(CSR_FILE_NAME, voteContainer.getCertificationRequest().getCsrPEM());
        fileMap.put(ANON_CERTIFICATE_REQUEST_FILE_NAME, signatureBytes);

        serviceURL = OperationType.ANON_VOTE_CERT_REQUEST.getUrl(qrMessageDto.getSystemEntityID());
        responseDto = HttpConn.getInstance().doPostMultipartRequest(fileMap, serviceURL);
        log.info("status: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
            certificationRequest.setSignedCsr(responseDto.getMessageBytes());
            byte[] voteBytes = XmlWriter.write(voteContainer.getVote());
            String voteStr = XMLUtils.prepareRequestToSign(voteBytes);
            log.info("vote: " + new String(voteBytes));
            Certificate[] certificateChain = new Certificate[]{certificationRequest.getCertificate()};
            //we use discrete timestamps to avoid associate by time proximity signed request with votes in the audits
            MockDNIe mockDNIe = new MockDNIe(certificationRequest.getPrivateKey(),
                    certificationRequest.getCertificate()).setCertificateChain(certificateChain);
            saveKeyStore(mockDNIe, KEYSTORE_PATH);
            byte[] signedVote = new XMLSignatureBuilder(voteStr.getBytes(), XAdESUtils.XML_MIME_TYPE,
                    SignatureAlgorithm.RSA_SHA_256.getName(), mockDNIe,
                    org.votingsystem.test.Constants.TIMESTAMP_SERVICE_DISCRETE_URL).build();
            log.info("signedVote: " + new String(signedVote));
            responseDto = HttpConn.getInstance().doPostRequest(signedVote, MediaType.XML,
                    OperationType.SEND_VOTE.getUrl(electionDto.getEntityId()));
            log.info("status: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
        }
    }

    public void voteFromKeyStore() throws Exception {
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        String serviceURL = OperationType.GET_QR_INFO.getUrl(qrMessageDto.getSystemEntityID());
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(qrMessageDto.getUUID().getBytes(), null, serviceURL);

        log.info("electionXML: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }
        QRResponseDto qrResponseDto = XML.getMapper().readValue(responseDto.getMessageBytes(), QRResponseDto.class);
        log.info("data: " + new String(qrResponseDto.getData()));
        ElectionDto electionDto = XmlReader.readElection(qrResponseDto.getData());
        ElectionOptionDto electionOption = electionDto.getElectionOptions().iterator().next();
        log.info("selected option: " + electionOption.getContent() + " - election UUID: " + electionDto.getUUID());
        VoteContainer voteContainer = VoteContainer.generate(electionDto, electionOption, qrMessageDto.getSystemEntityID());
        CertificationRequest certificationRequest = voteContainer.getCertificationRequest();

        //we use discrete timestamps to avoid associate by time proximity signed request with votes in the audits
        KeyStore keyStore = KeyStoreUtils.getKeyStore(FileUtils.getBytesFromFile(new File(KEYSTORE_PATH)),
                Constants.PASSW_DEMO.toCharArray());
        MockDNIe mockDNIe = new MockDNIe(keyStore, Constants.PASSW_DEMO.toCharArray(), Constants.USER_CERT_ALIAS);

        byte[] voteBytes = XmlWriter.write(voteContainer.getVote());
        String voteStr = XMLUtils.prepareRequestToSign(voteBytes);

        byte[] signedVote = new XMLSignatureBuilder(voteStr.getBytes(), XAdESUtils.XML_MIME_TYPE,
                SignatureAlgorithm.RSA_SHA_256.getName(), mockDNIe,
                org.votingsystem.test.Constants.TIMESTAMP_SERVICE_DISCRETE_URL).build();
        log.info("signedVote: " + new String(signedVote));
        responseDto = HttpConn.getInstance().doPostRequest(signedVote, MediaType.XML,
                OperationType.SEND_VOTE.getUrl(electionDto.getEntityId()));
        log.info("status: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
    }

    public static void saveKeyStore(MockDNIe mockDNIe, String keyStorePath) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        store.setKeyEntry("userkey", mockDNIe.getPrivateKey(), Constants.PASSW_DEMO.toCharArray(),
                mockDNIe.getCertificateChain());
        byte[] keyStoreBytes = KeyStoreUtils.toByteArray(store, Constants.PASSW_DEMO.toCharArray());
        File keyStoreDestFile = new File(keyStorePath);
        FileUtils.copyBytesToFile(keyStoreBytes, keyStoreDestFile);
        log.info("keyStore stored at: " + keyStoreDestFile.getAbsolutePath());
    }

}