package org.votingsystem.operation;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.JKSSignatureToken;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.votingsystem.BaseTest;
import org.votingsystem.Constants;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.AdminRequestDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.KeyDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.model.User;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.xades.XAdESSignature;
import org.votingsystem.xml.XML;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 */
public class CheckUserCertificate extends BaseTest {

    private static final Logger log = Logger.getLogger(CheckUserCertificate.class.getName());

    //private static String TEST_ENTITY = "https://192.168.1.5/voting-service";
    private static String TEST_ENTITY = "https://192.168.1.5/idprovider";

    private static String ADMIN_KEYSTORE = "certs/votingsystem-idprovider.jks";
    //private static String ADMIN_KEYSTORE = "certs/votingsystem-serviceprovider.jks";
    //private static String ADMIN_KEYSTORE = "certs/fake_7553172H.jks";
    private static String ADMIN_KEYSTORE_PASSWORD = org.votingsystem.util.Constants.PASSW_DEMO;
    private static String CERT_TO_CHECK = "certs/fake_7553172H.pem";


    public static void main(String[] args) throws Exception {
        new CheckUserCertificate().checkCert();
        System.exit(0);
    }

    public CheckUserCertificate() {
        super();
    }

    private void checkCert() throws Exception {
        X509Certificate userCert = PEMUtils.fromPEMToX509Cert(FileUtils.getBytesFromStream(
                Thread.currentThread().getContextClassLoader().getResource(CERT_TO_CHECK).openStream()));
        AdminRequestDto adminRequest = new AdminRequestDto(org.votingsystem.util.OperationType.CERT_USER_CHECK).setKey(new KeyDto(userCert, null));
        byte[] xmlToSign = XML.getMapper().writeValueAsBytes(adminRequest);
        log.info("xmlToSign: " + new String(xmlToSign));
        AbstractSignatureTokenConnection signingToken = new JKSSignatureToken(
                Thread.currentThread().getContextClassLoader().getResource(ADMIN_KEYSTORE).openStream(),
                ADMIN_KEYSTORE_PASSWORD);
        byte[] signedDocumentBytes =  XAdESSignature.sign(xmlToSign, signingToken,
                new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("signedXML", new String(signedDocumentBytes)));
        urlParameters.add(new BasicNameValuePair("userType", User.Type.ENTITY.name()));
        ResponseDto responseDto = HttpConn.getInstance().doPostFormRequest(
                OperationType.ADMIN_OPERATION_PROCESS.getUrl(TEST_ENTITY), urlParameters);
        log.info("status: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
    }
}
