package org.votingsystem.test.pkcs7;

import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.util.JSON;
import org.votingsystem.dto.ResponseDto;
import  org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;

import java.util.UUID;
import java.util.logging.Logger;

public class SignatureTest extends BaseTest {

    private static Logger log =  Logger.getLogger(SignatureTest.class.getName());

    private static String forge_pkcs7PEM = "-----BEGIN PKCS7-----\n" +
            "MIIKIAYJKoZIhvcNAQcCoIIKETCCCg0CAQExDzANBglghkgBZQMEAgEFADBvBgkq\n" +
            "hkiG9w0BBwGgYgRgeyJvcGVyYXRpb24iOnsidHlwZSI6IlNFU1NJT05fQ0VSVElG\n" +
            "SUNBVElPTiJ9LCJ1c2VyVVVJRCI6ImIzODNkZWQ3LTAwNzctNGI1ZS1iNmY2LWRk\n" +
            "OWQ1ZmU2NTQxMSJ9oIIErzCCBKswggOToAMCAQICCBlqwGNgjwR/MA0GCSqGSIb3\n" +
            "DQEBCwUAMCwxGjAYBgNVBAMMEUZBS0UgUk9PVCBETkllIENBMQ4wDAYDVQQLDAVD\n" +
            "ZXJ0czAeFw0xNzAxMjcyMDA4MzRaFw0xNzAxMjcyMzAwMDBaMFMxHDAaBgNVBAsM\n" +
            "E2Jyb3dzZXItY2VydGlmaWNhdGUxEDAOBgNVBAQMB0dhcmPDrWExDTALBgNVBCoM\n" +
            "BEpvc2UxEjAQBgNVBAUTCTA4ODg4ODg4RDCCASIwDQYJKoZIhvcNAQEBBQADggEP\n" +
            "ADCCAQoCggEBAKoOcooYIfn32uKWJK3q6HITErdQAOOA4kXkcCCCBmtAlkVT1Wzs\n" +
            "ZViSKP0mOTgrF1vG+vt/3S/Ke9Fajk17/ZoT1V9+xetP/cSfqtQ7TYToolM/Qm6y\n" +
            "MT/HImxrfY4BWk9OXrnby/xM7or2rkcuGnq5NH2v5YXeyrxVg5uMmgmFg32nIeXz\n" +
            "im8i7ksBvGy9b3p5+tz2iGHSFJPNynhIgHWpzO+ZiyLWnpBg3Qs4pBM2i348wMT8\n" +
            "SuU83V0ffYQ8q61avJxljcrzjvW8bHDwIWuwa27T3tNS5qY2rnxyj4Y2BPBoAug5\n" +
            "fkKyb/NnCE9RHCoGpLFdrFroIG7fIk7UgXsCAwEAAaOCAagwggGkMFsGA1UdIwRU\n" +
            "MFKAFD2Y24ezQwtgwwMwPWJqsmvQvFwwoTCkLjAsMRowGAYDVQQDDBFGQUtFIFJP\n" +
            "T1QgRE5JZSBDQTEOMAwGA1UECwwFQ2VydHOCCG/MMPi8INTqMD8GCCsGAQUFBwEB\n" +
            "BDMwMTAvBggrBgEFBQcwAYYjaHR0cHM6Ly8xOTIuMTY4LjEuNS9pZHByb3ZpZGVy\n" +
            "L29jc3AwHQYDVR0OBBYEFF/y/niDjA0K82z69gsRHYmZDnxxMAwGA1UdEwEB/wQC\n" +
            "MAAwDgYDVR0PAQH/BAQDAgWgMBIGCQAAAAAAAAAABQQFMQMBAf8wgbIGCQAAAAAA\n" +
            "AAAAAwSBpDGBoQyBnnsiZGV2aWNlTmFtZSI6InRlc3AtYXBwbGljYXRpb24iLCJu\n" +
            "aWYiOiIwODg4ODg4OEQiLCJnaXZlbm5hbWUiOiJKb3NlIiwic3VybmFtZSI6Ikdh\n" +
            "cmPDrWEiLCJkZXZpY2VUeXBlIjoiTU9CSUxFIiwidXVpZCI6ImIzODNkZWQ3LTAw\n" +
            "NzctNGI1ZS1iNmY2LWRkOWQ1ZmU2NTQxMSJ9MA0GCSqGSIb3DQEBCwUAA4IBAQCl\n" +
            "qGGCYMHFSQXi+YWXwwhDEjc8+rYLZrWQFJdMEnOZGfqghxJUMDYWbyr0wLdb3fsE\n" +
            "Fo/0N6BAI7wQdRupZE4eBOaC3pZQxKctGTcqi9LXR7bJ83L1XdORshTld63pCa98\n" +
            "nKek3l1u07p0bNOFHP9/BvUinAOdxp8F5MS4oGZlqvxPtVbkO2LGoNDm4XaU9sZq\n" +
            "jVDE9yjh3Hm9N9ccBtrFy7GZk64+uYNPVECGGozJ76D2ycEr8ic72/hAJA6+XIEw\n" +
            "iJonl8aCdu54m2ohnhV0NuIhWiTf9a9envJnl2vpyLec5xsD5YhBhStgIZYf1BLI\n" +
            "/jm+O+yIqbBnWaImE1kNMYIE0TCCBM0CAQEwODAsMRowGAYDVQQDDBFGQUtFIFJP\n" +
            "T1QgRE5JZSBDQTEOMAwGA1UECwwFQ2VydHMCCBlqwGNgjwR/MA0GCWCGSAFlAwQC\n" +
            "AQUAoIIDajAYBgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMC8GCSqGSIb3DQEJBDEi\n" +
            "BCC9Leu2mJgBEeXtf9rqlxzfDwQY22Y2PH1maf9pzECJWzAcBgkqhkiG9w0BCQUx\n" +
            "DxcNMTcwMTI3MjEwODM1WjCCAv0GCyqGSIb3DQEJEAIOMYIC7DCCAugGCSqGSIb3\n" +
            "DQEHAqCCAtkwggLVAgEDMQ8wDQYJYIZIAWUDBAIBBQAwgYEGCyqGSIb3DQEJEAEE\n" +
            "oHIkcARuMGwCAQEGBoIShDeGejAxMA0GCWCGSAFlAwQCAQUABCC9Leu2mJgBEeXt\n" +
            "f9rqlxzfDwQY22Y2PH1maf9pzECJWwIIMN5uDtW43r8YDzIwMTcwMTI3MjAwODM1\n" +
            "WjALAgEBgAIB9IECAfQCBAFMA6wxggI5MIICNQIBATA2MCwxGjAYBgNVBAMMEUZB\n" +
            "S0UgUk9PVCBETkllIENBMQ4wDAYDVQQLDAVDZXJ0cwIGAVjpBaSyMA0GCWCGSAFl\n" +
            "AwQCAQUAoIHTMBoGCSqGSIb3DQEJAzENBgsqhkiG9w0BCRABBDAcBgkqhkiG9w0B\n" +
            "CQUxDxcNMTcwMTI3MjAwODM1WjAtBgkqhkiG9w0BCTQxIDAeMA0GCWCGSAFlAwQC\n" +
            "AQUAoQ0GCSqGSIb3DQEBAQUAMC8GCSqGSIb3DQEJBDEiBCB3temRJCmaEt1CwY7q\n" +
            "Xd37QuNN3LxJNeLTdQxPsFeDHzA3BgsqhkiG9w0BCRACLzEoMCYwJDAiBCBTLYlN\n" +
            "UVSCaOHwYBqptlh/SwiV/j7rJxoSj1OkkyAhBTANBgkqhkiG9w0BAQEFAASCAQB/\n" +
            "0TAMZSIbvkoqrRLDq43x8BYbIgf/mloQyS2hfUDMXrtkeTT+xU1eO6sqrvhTfxEN\n" +
            "ZrMoKLBWEVJ3pvc9T7p85Ob9WobdcHCIm/X/+3geFBKj5kw7RkmQI+HqFZL8LMLt\n" +
            "xCmq9fCI/43uAOBelXqbTII7bIl7MkDSmODKHNKxR5blBt8E3m5y0yDQEqqZVoGL\n" +
            "aJbDCpFqRS4KulnrImz0yNNiA62frh+iheP1HvG08Rg1+OI8TLIVR8jwAFOPRCCk\n" +
            "5/lfJ3vaJKEe2c2X3fWoUoCiHnM+Mn1h2acxQvS17ZSJUkDobE0SEQemGsUK1d5I\n" +
            "vC8X0h5ZaE357KmRvC0EoQAwDQYJKoZIhvcNAQEBBQAEggEAf29ZAeg244mHu5JO\n" +
            "a9Qs7nV3kq/ygxpt9OH/u1z7A8GhYDmvHu/dgtS3I4ZYdxYplUk8igIRHcWz64PV\n" +
            "LBbJyjlsnW2V910qKAPpsRKnsMPD20ZqlYxQtSGifbFFysklJgFnwg6JIhJO18Dl\n" +
            "/og6xwW2QZxVGyo2tVY+K+cWiLF4Al+ptxv+BkPuPz3YgA6IQ8vebUmTf76Q2FQX\n" +
            "CW6VMuAruffPQWX7B+Zn6jQ8A5C5n7qHWJvdX/6JSzIsnR1xriS5bV+td/dY51XE\n" +
            "/ny3razcyxh2YnG6kGS2tnNGf+pTGSMjx7xtVv+QfeVLt+frcB6pDKGf+qIyagQy\n" +
            "v/wZjQ==\n" +
            "-----END PKCS7-----";



    public static void main(String[] args) throws Exception {
        SignatureTest signatureTest = new SignatureTest();
        //CMSSignedMessage cmsSignedMessage = signatureTest.sign();
        //signatureTest.validate(cmsSignedMessage.toPEM());
        //signatureTest.sendToServer(cmsSignedMessage.toPEM(), "https://voting.ddns.net/currency-server/api/test/cms-document");
        //signatureTest.sendToServer(cmsSignedMessage.toPEM(), "https://voting.ddns.net/currency-server/api/device/init-browser-session");
        signatureTest.readPem();
        System.exit(0);
    }

    public SignatureTest() {
        super();
    }

    public void readPem() throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(forge_pkcs7PEM);
        log.info("isValidSignature: " + cmsSignedMessage.isValidSignature());
    }


    public CMSSignedMessage sign() throws Exception {
        CMSSignatureBuilder signatureService = new CMSSignatureBuilder(new MockDNIe("08888888D"));
        OperationTypeDto operationType = new OperationTypeDto(CurrencyOperation.INIT_BROWSER_SESSION, null);
        OperationDto operation = new OperationDto();
        operation.setOperation(operationType);
        operation.setUUID(UUID.randomUUID().toString());
        String operationStr = JSON.getMapper().writeValueAsString(operation);
        log.info("operation: " + operationStr);

        CMSSignedMessage cmsSignedMessage = signatureService.signDataWithTimeStamp(operationStr.getBytes());
        return cmsSignedMessage;
    }

    public void validate(byte[] cmsPEMBytes) throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(cmsPEMBytes);
        log.info("isValidSignature: " + cmsSignedMessage.isValidSignature() + " - contentDigest: " +
                cmsSignedMessage.getContentDigestStr());
    }

    public void sendToServer(byte[] cmsPEMBytes, String serviceURL) throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(cmsPEMBytes);
        log.info("isValidSignature: " + cmsSignedMessage.isValidSignature());
        //doPostRequest(byte[] byteArray, String contentType, String targetURL)
        ResponseDto response = HttpConn.getInstance().doPostRequest(cmsPEMBytes, MediaType.PKCS7_SIGNED, serviceURL);
        log.info("status: " + response.getStatusCode() + " - message: " + response.getMessage());
    }

}






