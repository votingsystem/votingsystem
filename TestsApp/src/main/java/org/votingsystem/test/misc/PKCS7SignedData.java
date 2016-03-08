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

    private static String pkcs7SignedData = "-----BEGIN PKCS7-----\n"+
            "MIIE5QYJKoZIhvcNAQcCoIIE1jCCBNICAQExDzANBglghkgBZQMEAgEFADApBgkq\n"+
            "hkiG9w0BBwGgHAQaU29tZSBjb250ZW50IHRvIGJlIHNpZ25lZC6gggMIMIIDBDCC\n"+
            "Am2gAwIBAgIBATANBgkqhkiG9w0BAQUFADBpMRQwEgYDVQQDEwtleGFtcGxlLm9y\n"+
            "ZzELMAkGA1UEBhMCVVMxETAPBgNVBAgTCFZpcmdpbmlhMRMwEQYDVQQHEwpCbGFj\n"+
            "a3NidXJnMQ0wCwYDVQQKEwRUZXN0MQ0wCwYDVQQLEwRUZXN0MB4XDTE2MDMwNjA5\n"+
            "NDMyNVoXDTE3MDMwNjA5NDMyNVowaTEUMBIGA1UEAxMLZXhhbXBsZS5vcmcxCzAJ\n"+
            "BgNVBAYTAlVTMREwDwYDVQQIEwhWaXJnaW5pYTETMBEGA1UEBxMKQmxhY2tzYnVy\n"+
            "ZzENMAsGA1UEChMEVGVzdDENMAsGA1UECxMEVGVzdDCBnzANBgkqhkiG9w0BAQEF\n"+
            "AAOBjQAwgYkCgYEAzMQnIjVEP6OQHmPo6yxzYCZ2vSVXcKRk/i2vCHU68WlGiwi8\n"+
            "RDqhzFGYkJ/FZ8YvJWR0a2yS6sipgfQg4l7Rj6gd3mBt/YQi/3pn31diJr5IhiDd\n"+
            "L/H3WzwDv/g+pUxnEFQii0hvV/KHsYzZ6eVUZY5L8MWb2rLcKoKG1waF458CAwEA\n"+
            "AaOBuzCBuDAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIC9DA7BgNVHSUENDAyBggr\n"+
            "BgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMDBggrBgEFBQcDBAYIKwYBBQUHAwgw\n"+
            "EQYJYIZIAYb4QgEBBAQDAgD3MCwGA1UdEQQlMCOGG2h0dHA6Ly9leGFtcGxlLm9y\n"+
            "Zy93ZWJpZCNtZYcEfwAAATAdBgNVHQ4EFgQURZpCXIFRekHbQwi7IT7NnbAK7cMw\n"+
            "DQYJKoZIhvcNAQEFBQADgYEALM17mPNUKU+M+k1amSRZOkMY/4iU5rgaXAhaHug+\n"+
            "EkXcHkMtOl8pagQYtaY8rVrjZxtLHAU0tIEHPHfuaCbYGVOQStjMh10OlYbwXUU6\n"+
            "aEw9iLsMihPM2AoN+Z4qSOPudAp1XSf07IDKpY8cgNovNTd5asm5ncct6DwH50Lv\n"+
            "TN4xggGDMIIBfwIBATBuMGkxFDASBgNVBAMTC2V4YW1wbGUub3JnMQswCQYDVQQG\n"+
            "EwJVUzERMA8GA1UECBMIVmlyZ2luaWExEzARBgNVBAcTCkJsYWNrc2J1cmcxDTAL\n"+
            "BgNVBAoTBFRlc3QxDTALBgNVBAsTBFRlc3QCAQEwDQYJYIZIAWUDBAIBBQCgaTAY\n"+
            "BgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMC8GCSqGSIb3DQEJBDEiBCBiJ2Anl246\n"+
            "eFG9wI+2nC7dn9xWaYlWZN+J3emy+hECpTAcBgkqhkiG9w0BCQUxDxcNMTYwMzA2\n"+
            "MDk0MzI1WjANBgkqhkiG9w0BAQEFAASBgG92IgVUew1TwTMYnFrV6AWnhvJgDi2I\n"+
            "hFg2cadLHC3ZrTP7XJ7lTGkDhiEJIO0Y45pPSBPa2p8868nFN7eZkn9U19Ut96LL\n"+
            "7mTWl8KTbkSkZ6WxTPIKhz97o5GXhuAP5Bw/qN/ABydx17/y83w9JJBl9EOHZGU6\n"+
            "4zIyNNyNVp66\n"+
            "-----END PKCS7-----\n";

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

        CMSSignedMessage cmsMsg = signData("Hello");


        //CMSSignedMessage newCMSMsg = new CMSSignedMessage(cmsMsg.toASN1Structure().getEncoded());
        //CMSSignedData cmsMultiSignedData = addSignature(cmsSignedData);
        //String pemEncoded = new String(PEMUtils.getPEMEncoded(cmsMultiSignedData.getContentInfo()));
        //log.info("CMSSignedData: " + pemEncoded);
        //validatePKCS7SignedData(pemEncoded);
        log.info(" ------ CMSSignedData: " + cmsMsg.getSignedContentStr());

        cmsMsg = addTimeStamp(cmsMsg);
        byte[] pemBytes = cmsMsg.toPEM();
        log.info("CMSSignedData: " + new String(pemBytes));
        cmsMsg = CMSSignedMessage.FROM_PEM(pemBytes);
        log.info(" ------ CMSSignedData: " + cmsMsg.getSignedContent());

        cmsMsg = CMSSignedMessage.FROM_PEM(pkcs7PEM);
        log.info(" +++------ CMSSignedData: " + cmsMsg.getSignedContentStr());
    }



    private static void validatePKCS7SignedData (String pkcs7PEMData) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pkcs7PEMData.getBytes())));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        if (!contentInfo.getContentType().equals(CMSObjectIdentifiers.envelopedData)) {
            log.info("CMSObjectIdentifiers - envelopedData");
        }
        CMSSignedData signedData = new CMSSignedData(contentInfo.getEncoded());
        Store signedDataCertStore = signedData.getCertificates();
        SignerInformationStore signers = signedData.getSignerInfos();
        Iterator it = signers.getSigners().iterator();
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation)it.next();
            Collection certCollection = signedDataCertStore.getMatches(signer.getSID());
            X509CertificateHolder certificateHolder = (X509CertificateHolder)certCollection.iterator().next();
            X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate( certificateHolder );
            log.info(x509Certificate.toString());
            //assertEquals(true, signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert)));
        }
        log.info("CMSSignedData: " + signedData);
        //CMSSignedDataParser sp = new CMSSignedDataParser(new JcaDigestCalculatorProviderBuilder().setProvider("BC").build(), sig);
    }

    public static CMSSignedMessage signData(String signatureContent) throws Exception {
        SignatureService signatureService = SignatureService.genUserVSSignatureService("7553172H");
        List certList = new ArrayList();
        CMSTypedData msg = new CMSProcessableByteArray(signatureContent.getBytes());
        certList.add(signatureService.getCertSigner());
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(signatureService.getPrivateKey());
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider("BC").build()).build(signer, signatureService.getCertSigner()));
        gen.addCertificates(certs);
        CMSSignedData signedData = gen.generate(msg, true);
        //validatePKCS7SignedData(pemEncoded);
        return  new CMSSignedMessage(signedData);
    }

    public static CMSSignedData addSignature(CMSSignedData signedData) throws Exception {
        SignatureService signatureService = SignatureService.genUserVSSignatureService("00000123P");
        Store signedDataCertStore = signedData.getCertificates();
        SignerInformationStore signers = signedData.getSignerInfos();
        //You'll need to copy the other signers certificates across as well if you want them included.
        List certList = new ArrayList();
        Iterator it = signers.getSigners().iterator();
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation)it.next();
            Collection certCollection = signedDataCertStore.getMatches(signer.getSID());
            X509CertificateHolder certificateHolder = (X509CertificateHolder)certCollection.iterator().next();
            X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate( certificateHolder );
            certList.add(x509Certificate);
            //assertEquals(true, signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert)));
        }
        certList.add(signatureService.getCertSigner());
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(signatureService.getPrivateKey());
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider("BC").build()).build(signer, signatureService.getCertSigner()));
        gen.addCertificates(certs);
        gen.addSigners(signers);
        //CMSProcessable content = new CMSProcessableByteArray(buffer);
        CMSSignedData multiSignedData = gen.generate(signedData.getSignedContent(), true);
        return multiSignedData;
    }

    public static CMSSignedMessage addTimeStamp(CMSSignedMessage signedData) throws Exception {
        TimeStampRequest tspRequest = signedData.getTimeStampRequest();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(tspRequest.getEncoded(), ContentTypeVS.TIMESTAMP_QUERY,
                "https://192.168.1.5/TimeStampServer/timestamp");
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(responseVS.getMessageBytes()));
            signedData = signedData.addTimeStamp(signedData, timeStampToken);
        } else throw new ExceptionVS(responseVS.getMessage());
        return signedData;
    }

}