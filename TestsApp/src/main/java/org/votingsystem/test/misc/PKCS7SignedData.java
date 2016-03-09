package org.votingsystem.test.misc;

import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class PKCS7SignedData {

    private static Logger log =  Logger.getLogger(PKCS7SignedData.class.getName());

    private static String pkcs7PEM = "-----BEGIN PKCS7-----\n" +
            "MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0B\n" +
            "BwGggCSABAVIZWxsbwAAAAAAAKCAMIICGDCCAYGgAwIBAgIIICb038u4xG0wDQYJ\n" +
            "KoZIhvcNAQEFBQAwLjEVMBMGA1UECxMMcGFzc3dvcmRIYWNrMRUwEwYDVQQDEwx2\n" +
            "b3RpbmdzeXN0ZW0wHhcNMTYwMzA3MDgyODAxWhcNMTYwMzA3MDgyODAxWjAuMRUw\n" +
            "EwYDVQQLDAxwYXNzd29yZEhhY2sxFTATBgNVBAMMDHZvdGluZ3N5c3RlbTCBnzAN\n" +
            "BgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA1uC2aPP6eE4LYa184M5gfi3DtKdEopvV\n" +
            "giVICLfs2WXxpB82A/W7Ru937AeMoY85jsBBYyPE6ycxInxLo6R8YEpu8GHPYl3i\n" +
            "1gUIdbQ3EQ1ZqvKBsH+PljXDFqDpNa55lT81SHxeemOFNcDllbiw8dNWySX1erOe\n" +
            "MPGrS68WjyMCAwEAAaM/MD0wHQYDVR0OBBYEFNnfKQcDRVorTWXYJDYsH87ZX0cq\n" +
            "MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgWgMA0GCSqGSIb3DQEBBQUAA4GB\n" +
            "AAnv+ZgghRL39Y1ihkONsUtrecNLBmNKL0afrxvpVKNggs+L8Iw3zV1yjOgeFP5X\n" +
            "KXyqqmXkYx04uMJ6FR/g9YObamA9/tkh+XYMTFUpf3s3cMdFOpj8AeRtnkQr6DvG\n" +
            "nQ22fi2Tu2TFj3eWtKPg4AdzYZcLsMQ3kkE58LpbiwsYAAAxggOhMIIDnQIBATA6\n" +
            "MC4xFTATBgNVBAsTDHBhc3N3b3JkSGFjazEVMBMGA1UEAxMMdm90aW5nc3lzdGVt\n" +
            "AgggJvTfy7jEbTANBglghkgBZQMEAgEFAKBpMBgGCSqGSIb3DQEJAzELBgkqhkiG\n" +
            "9w0BBwEwHAYJKoZIhvcNAQkFMQ8XDTE2MDMwNzA4MjgwMVowLwYJKoZIhvcNAQkE\n" +
            "MSIEIBhfjbMicf4l9WGm/JOLLiZDBuwwTtpRgAfRdkgmOBlpMA0GCSqGSIb3DQEB\n" +
            "AQUABIGAigc7wumSPBBGolQ3SvRuOvZ/tdS9JUqpsg1VmNQrED/eYGCzx50QJM1q\n" +
            "aM4Rn2/HCrEoROf9KwQ7yTmXRezI7m3HWNGwgalQsMozayUwwCxfnQ9bcqwQVQP+\n" +
            "fWQS0/29R6VrkSKf1ryAjN5/EXuA3RCfTeWgNv5Av6rFpssVEDyhggJOMIICSgYL\n" +
            "KoZIhvcNAQkQAg4xggI5MIICNQYJKoZIhvcNAQcCoIICJjCCAiICAQMxDzANBglg\n" +
            "hkgBZQMEAgEFADB7BgsqhkiG9w0BCRABBKBsBGowaAIBAQYCKgMwMTANBglghkgB\n" +
            "ZQMEAgEFAAQgGF+NsyJx/iX1Yab8k4suJkMG7DBO2lGAB9F2SCY4GWkCCDHpwESy\n" +
            "x3skGA8yMDE2MDMwNzA4MjgwMlowCwIBAYACAfSBAgH0AgT2o2JdMYIBjTCCAYkC\n" +
            "AQEwSDA+MSwwKgYDVQQDDCNWb3RpbmcgU3lzdGVtIENlcnRpZmljYXRlIEF1dGhv\n" +
            "cml0eTEOMAwGA1UECwwFQ2VydHMCBgFQE+9HUDANBglghkgBZQMEAgEFAKCBmDAa\n" +
            "BgkqhkiG9w0BCQMxDQYLKoZIhvcNAQkQAQQwHAYJKoZIhvcNAQkFMQ8XDTE2MDMw\n" +
            "NzA4MjgwMlowKwYLKoZIhvcNAQkQAgwxHDAaMBgwFgQU+6QhNnQVSZ7saJJmM8QG\n" +
            "0XnMIYowLwYJKoZIhvcNAQkEMSIEIN1i5G/cbpGXuHtDW6i8zizh9GR+NTZfaM9P\n" +
            "TdoejlIfMA0GCSqGSIb3DQEBAQUABIGAf3ur+aonoFyH2eY/cK6Df8qD5agvSeG6\n" +
            "X5KnZwM+S+ph5Mcxk6VXFYfvyFAYS/6CMrF6hlIHSOLUJkRvk7crdGRQUH/JG8XQ\n" +
            "LMwr8PD01ZhfkblbRBZTIDNM63Sl9bWMb+S2VcKiI02bBSm/eGevpmncl66Jg6Wc\n" +
            "M1fdsn5DN5cAAAAAAAA=\n" +
            "-----END PKCS7-----";

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SignatureService signatureService = SignatureService.genUserVSSignatureService("08888888D");
        CMSSignedMessage cmsMsg = signatureService.signData("Hello");
        log.info("CMSSignedData: " + cmsMsg.getSignedContentStr());
        cmsMsg = CMSSignedMessage.addTimeStamp(cmsMsg, "https://192.168.1.5/TimeStampServer/timestamp");
        byte[] pemBytes = cmsMsg.toPEM();
        log.info("CMSSignedData: " + new String(pemBytes));
        cmsMsg = CMSSignedMessage.FROM_PEM(pemBytes);
        log.info("CMSSignedData: " + cmsMsg.getSignedContent());
        cmsMsg = CMSSignedMessage.FROM_PEM(pkcs7PEM);
        cmsMsg.isValidSignature();
        log.info("CMSSignedData: " + cmsMsg.getSignedContentStr());
    }

}