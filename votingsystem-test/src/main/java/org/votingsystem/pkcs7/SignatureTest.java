package org.votingsystem.pkcs7;

import org.votingsystem.BaseTest;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.OperationCheckerDto;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;
import org.votingsystem.util.SystemOperation;

import java.util.UUID;
import java.util.logging.Logger;

public class SignatureTest extends BaseTest {

    private static Logger log =  Logger.getLogger(SignatureTest.class.getName());

    private static String forge_pkcs7PEM = "-----BEGIN PKCS7-----\n" +
            "MIIIPgYJKoZIhvcNAQcCoIIILzCCCCsCAQExDzANBglghkgBZQMEAgEFADBjBgkq\n" +
            "hkiG9w0BBwGgVgRUeyJvcGVyYXRpb24iOnsidHlwZSI6IkNMT1NFX1NFU1NJT04i\n" +
            "fSwidXVpZCI6ImU3MTRmYWE5LTFhNGYtNGI3OC05MmViLTAzZTE4ZTQ1YTExYiJ9\n" +
            "oIIDWjCCA1YwggI+oAMCAQICCFg6kuEHlP1gMA0GCSqGSIb3DQEBCwUAMCwxGjAY\n" +
            "BgNVBAMMEUZBS0UgUk9PVCBETkllIENBMQ4wDAYDVQQLDAVDZXJ0czAeFw0xNzAx\n" +
            "MTgwODAwMTFaFw0xNzAxMTgyMzAwMDBaME0xHDAaBgNVBAsME2Jyb3dzZXItY2Vy\n" +
            "dGlmaWNhdGUxLTArBgNVBAUTJDIwNWUwZTA3LWYwMjItNDRlOS05YjMyLTFmZTE2\n" +
            "M2M1MTczMjCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAp9uSiKSEI7vXbPKw\n" +
            "AW0HfUfxZXoo2OPegdcC58srpIeI93qAcXEAQPh5nL0Q7xWmBgwnjRdfhydfdJ6r\n" +
            "ydpwqxjU2BilincT/lMJwpA54mbvxkH+IdH8ls66JhPi2VdmIzjZ3Bja7i4C/+R1\n" +
            "U/iz2601cVZPnNjYEaWimDd+FeECAwEAAaOB3jCB2zBbBgNVHSMEVDBSgBQ9mNuH\n" +
            "s0MLYMMDMD1iarJr0LxcMKEwpC4wLDEaMBgGA1UEAwwRRkFLRSBST09UIEROSWUg\n" +
            "Q0ExDjAMBgNVBAsMBUNlcnRzgghvzDD4vCDU6jA/BggrBgEFBQcBAQQzMDEwLwYI\n" +
            "KwYBBQUHMAGGI2h0dHBzOi8vMTkyLjE2OC4xLjUvaWRwcm92aWRlci9vY3NwMB0G\n" +
            "A1UdDgQWBBS40tHq4pdJE9ug64XqTsjPHoahuTAMBgNVHRMBAf8EAjAAMA4GA1Ud\n" +
            "DwEB/wQEAwIFoDANBgkqhkiG9w0BAQsFAAOCAQEAzuDVfyNcOoBnzFypr3OqirTe\n" +
            "4MIDnccJSSztmuwkZfP+NcxuTzA6ub6Lx6h+mPSPTca9B9jIl/OoO7Db37np8Fzj\n" +
            "gwSkneeOCQpWWjBkBEt9OYFSCTtj4R7adMnzWIm53IUB1EqBPVsxvvJLnNWRrQKn\n" +
            "tRPg5ET8l+Dvi3HAvC27602i5rvEeeRiPLSUwmR+MOykWn02EgnHeyCXSiaplrBL\n" +
            "xEQ2Y4QcFgQ45n21S7/IAa1BANoSN9t59x3YLl2DuBbH+mRR5XEy8JS4Pst+CHW2\n" +
            "gQoee6gvvdeCU0wws3VS+Hr18BEePyAu5WKO/vGtV4WGoo4T5EdrY7JDfwoipTGC\n" +
            "BFAwggRMAgEBMDgwLDEaMBgGA1UEAwwRRkFLRSBST09UIEROSWUgQ0ExDjAMBgNV\n" +
            "BAsMBUNlcnRzAghYOpLhB5T9YDANBglghkgBZQMEAgEFAKCCA2owGAYJKoZIhvcN\n" +
            "AQkDMQsGCSqGSIb3DQEHATAvBgkqhkiG9w0BCQQxIgQg8bjYwbsjTWs3KVnuGXRB\n" +
            "ilgPYRJ8URiQQHS9GgOeYDEwHAYJKoZIhvcNAQkFMQ8XDTE3MDExODA5MDAyNFow\n" +
            "ggL9BgsqhkiG9w0BCRACDjGCAuwwggLoBgkqhkiG9w0BBwKgggLZMIIC1QIBAzEP\n" +
            "MA0GCWCGSAFlAwQCAQUAMIGBBgsqhkiG9w0BCRABBKByJHAEbjBsAgEBBgaCEoQ3\n" +
            "hnowMTANBglghkgBZQMEAgEFAAQg8bjYwbsjTWs3KVnuGXRBilgPYRJ8URiQQHS9\n" +
            "GgOeYDECCET3Ecswj6arGA8yMDE3MDExODA4MDAyNFowCwIBAYACAfSBAgH0AgQA\n" +
            "u2hVMYICOTCCAjUCAQEwNjAsMRowGAYDVQQDDBFGQUtFIFJPT1QgRE5JZSBDQTEO\n" +
            "MAwGA1UECwwFQ2VydHMCBgFY6QWksjANBglghkgBZQMEAgEFAKCB0zAaBgkqhkiG\n" +
            "9w0BCQMxDQYLKoZIhvcNAQkQAQQwHAYJKoZIhvcNAQkFMQ8XDTE3MDExODA4MDAy\n" +
            "NFowLQYJKoZIhvcNAQk0MSAwHjANBglghkgBZQMEAgEFAKENBgkqhkiG9w0BAQEF\n" +
            "ADAvBgkqhkiG9w0BCQQxIgQgxpno6Z/yv7Kga3K4varZre7S6CxHzNV+HEEOY9xm\n" +
            "9xgwNwYLKoZIhvcNAQkQAi8xKDAmMCQwIgQgUy2JTVFUgmjh8GAaqbZYf0sIlf4+\n" +
            "6ycaEo9TpJMgIQUwDQYJKoZIhvcNAQEBBQAEggEAAn+D2sGeG2OBh04nhnk9PO44\n" +
            "g0n+4UhlzCvFaPx+uqA0EU996217ImegME4IyTeTEEjfukUY53eo+uZjr6od4o+U\n" +
            "2+xzxUPziQf2JsE7wAROD1LpZ04Ct/w4OSQSxsnVCXzkWVbmkLWpFEs2wSgK0RRM\n" +
            "ZT+ivAZXumFD3+Ov37ZwihXFZVP6pBhWFZ1ymw4FkeT5mMOf41KW9mDemO5/Hfha\n" +
            "70R5ox1dMeRYR82r4ecs43baaUAU9FwkT+pGiqIltGrKnJBMB0Pr2QoulB3PRZie\n" +
            "h2n1Hz/ujIzTwnRuuybk4xSMOxyUbpGsLCkeEmBFl/tKMzX65xV8roKCAcPTtqEA\n" +
            "MA0GCSqGSIb3DQEBAQUABIGASRfBRHR/Z4tH+KMuwsRBaG5PFGCb/wQ0gJc7LrQq\n" +
            "Ds+d/+I3twsUnxabXboOMbOGcuI+YZqZk0NvTtCSpsqRi1JQp+G4xoQ1FDviAZ85\n" +
            "wsjYloYJbCtHby7ZCHovFf1lyK+6rGzphCeg/jFstiqRk/2vGVz/SI7mTfdo5pwR\n" +
            "lHw=\n" +
            "-----END PKCS7-----";



    public static void main(String[] args) throws Exception {
        SignatureTest signatureTest = new SignatureTest();
        CMSSignedMessage cmsSignedMessage = signatureTest.sign();
        //signatureTest.validate(cmsSignedMessage.toPEM());
        signatureTest.sendToServer(cmsSignedMessage.toPEM());
        //signatureTest.readPem();
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
        SignatureBuilder signatureService = new SignatureBuilder(new MockDNIe("08888888D"));
        OperationTypeDto operationType = new OperationTypeDto(CurrencyOperation.CLOSE_SESSION, null);
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

    public void sendToServer(byte[] cmsPEMBytes) throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(cmsPEMBytes);
        log.info("isValidSignature: " + cmsSignedMessage.isValidSignature());
        //doPostRequest(byte[] byteArray, String contentType, String targetURL)
        ResponseDto response = HttpConn.getInstance().doPostRequest(cmsPEMBytes, "application/pkcs7-signature",
                "http://votingsystem.ddns.net/currency-server/api/test-signed/cms");
        log.info("status: " + response.getStatusCode() + " - message: " + response.getMessage());
    }

}






