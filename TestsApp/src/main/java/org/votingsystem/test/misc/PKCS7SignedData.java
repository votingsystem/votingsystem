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
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class PKCS7SignedData {

    private static Logger log =  Logger.getLogger(PKCS7SignedData.class.getName());

    private static String forge_pkcs7PEM = "-----BEGIN PKCS7-----\n" +
            "MIIE5QYJKoZIhvcNAQcCoIIE1jCCBNICAQExDzANBglghkgBZQMEAgEFADApBgkq\n" +
            "hkiG9w0BBwGgHAQaU29tZSBjb250ZW50IHRvIGJlIHNpZ25lZC6gggMIMIIDBDCC\n" +
            "Am2gAwIBAgIBATANBgkqhkiG9w0BAQUFADBpMRQwEgYDVQQDEwtleGFtcGxlLm9y\n" +
            "ZzELMAkGA1UEBhMCVVMxETAPBgNVBAgTCFZpcmdpbmlhMRMwEQYDVQQHEwpCbGFj\n" +
            "a3NidXJnMQ0wCwYDVQQKEwRUZXN0MQ0wCwYDVQQLEwRUZXN0MB4XDTE2MDMxMDE0\n" +
            "NTY1OFoXDTE3MDMxMDE0NTY1OFowaTEUMBIGA1UEAxMLZXhhbXBsZS5vcmcxCzAJ\n" +
            "BgNVBAYTAlVTMREwDwYDVQQIEwhWaXJnaW5pYTETMBEGA1UEBxMKQmxhY2tzYnVy\n" +
            "ZzENMAsGA1UEChMEVGVzdDENMAsGA1UECxMEVGVzdDCBnzANBgkqhkiG9w0BAQEF\n" +
            "AAOBjQAwgYkCgYEAjq+S7jBL1fkHHk6098SVS0k6+ZkiXVamB558CKB+KPmNJ3Og\n" +
            "akO1w+Ye/IMpnnYiN/UmSVFP7jCczpJjMZgRCNzuh59KviyQZSAUyNtDTWm7wVyS\n" +
            "OLN52SsOFVjbbTnRQP9Sm+BRD+Rm6B8JT57qvGaoPAbzxhKROT0UWrhUG2sCAwEA\n" +
            "AaOBuzCBuDAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIC9DA7BgNVHSUENDAyBggr\n" +
            "BgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMDBggrBgEFBQcDBAYIKwYBBQUHAwgw\n" +
            "EQYJYIZIAYb4QgEBBAQDAgD3MCwGA1UdEQQlMCOGG2h0dHA6Ly9leGFtcGxlLm9y\n" +
            "Zy93ZWJpZCNtZYcEfwAAATAdBgNVHQ4EFgQUDjWdY/Uo3oo4AG1YODjLxM/u5zAw\n" +
            "DQYJKoZIhvcNAQEFBQADgYEAY/2xdLFN26u+vKx6jry4ktYIUF/1jRAUeU2L5oJg\n" +
            "SoesuVNXmI6i5GDxklEmbB4sKbEYrJ+iIkV+jeHbl6OKZG+OrlkGyGkXoaTQJ0GR\n" +
            "bu2hyNX4Q4eH6snoqFl4roQnUFDYB4/SFMbwJdvcKjLAG2uVzzHxGscPV8mLrwjQ\n" +
            "OSkxggGDMIIBfwIBATBuMGkxFDASBgNVBAMTC2V4YW1wbGUub3JnMQswCQYDVQQG\n" +
            "EwJVUzERMA8GA1UECBMIVmlyZ2luaWExEzARBgNVBAcTCkJsYWNrc2J1cmcxDTAL\n" +
            "BgNVBAoTBFRlc3QxDTALBgNVBAsTBFRlc3QCAQEwDQYJYIZIAWUDBAIBBQCgaTAY\n" +
            "BgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMC8GCSqGSIb3DQEJBDEiBCBiJ2Anl246\n" +
            "eFG9wI+2nC7dn9xWaYlWZN+J3emy+hECpTAcBgkqhkiG9w0BCQUxDxcNMTYwMzEw\n" +
            "MTQ1NjU4WjANBgkqhkiG9w0BAQEFAASBgHsApf1OABoNNok+7QVcKffh0nklvBjA\n" +
            "WB/SD29OXby9Fd3UhdkkSLaCueazH1QiwMCH5ssZ3p06L/unvTIwAcIiSbKimqLn\n" +
            "z/ekTYw6HuEvdiEvITLU2IJubvFQDOY8yhNPwQuRHuR8eb3OQ59CgD599BNn4/H/\n" +
            "G6foAFuXbMsu\n" +
            "-----END PKCS7-----\n";

    private static String pkcs7PEM = "-----BEGIN PKCS7-----\n" +
            "MIAGCSqGSIb3DQEHAqCAMIIE0gIBATEPMA0GCWCGSAFlAwQCAQUAMCkGCSqGSIb3\n" +
            "DQEHAaAcBBpTb21lIGNvbnRlbnQgdG8gYmUgc2lnbmVkLqCCAwgwggMEMIICbaAD\n" +
            "AgECAgEBMA0GCSqGSIb3DQEBBQUAMGkxFDASBgNVBAMTC2V4YW1wbGUub3JnMQsw\n" +
            "CQYDVQQGEwJVUzERMA8GA1UECBMIVmlyZ2luaWExEzARBgNVBAcTCkJsYWNrc2J1\n" +
            "cmcxDTALBgNVBAoTBFRlc3QxDTALBgNVBAsTBFRlc3QwHhcNMTYwMzEwMTMwNTM3\n" +
            "WhcNMTcwMzEwMTMwNTM3WjBpMRQwEgYDVQQDEwtleGFtcGxlLm9yZzELMAkGA1UE\n" +
            "BhMCVVMxETAPBgNVBAgTCFZpcmdpbmlhMRMwEQYDVQQHEwpCbGFja3NidXJnMQ0w\n" +
            "CwYDVQQKEwRUZXN0MQ0wCwYDVQQLEwRUZXN0MIGfMA0GCSqGSIb3DQEBAQUAA4GN\n" +
            "ADCBiQKBgQCImhN2ONcWCcnLT0GscoJQpIfaBx6NN6tu0vAOcOF/H+zYdP0xTEHR\n" +
            "pukAEuIj8YuLYKlIaVurfiRlE6XApdRgb1JnjzD3FaCVhInUia8rtHSlO1P2ItzZ\n" +
            "cmMSU4KJlbQ+/HaVzsP42P8eXISp4NzPNP5G5QMjF1zw+jH2q7mMgwIDAQABo4G7\n" +
            "MIG4MAwGA1UdEwQFMAMBAf8wCwYDVR0PBAQDAgL0MDsGA1UdJQQ0MDIGCCsGAQUF\n" +
            "BwMBBggrBgEFBQcDAgYIKwYBBQUHAwMGCCsGAQUFBwMEBggrBgEFBQcDCDARBglg\n" +
            "hkgBhvhCAQEEBAMCAPcwLAYDVR0RBCUwI4YbaHR0cDovL2V4YW1wbGUub3JnL3dl\n" +
            "YmlkI21lhwR/AAABMB0GA1UdDgQWBBRU2Ig4z557q50g3T2Sq0ZotuIusDANBgkq\n" +
            "hkiG9w0BAQUFAAOBgQBUlZAXjZdgsTrkOshnMpkd6NpJsU53E7dBrGfjkIVF31J9\n" +
            "7OjdZoon13W/ZB+guykkJxUupPIM04V7d9FEmr3xmnLhaaKUbH9ssPgBzWd+0eNY\n" +
            "VIrQG0pfHmgYJ9Z2uJIvOVBqHJRVOfdqE97brwNylORjDd3cP4nASuqj3eZxHTGC\n" +
            "AYMwggF/AgEBMG4waTEUMBIGA1UEAxMLZXhhbXBsZS5vcmcxCzAJBgNVBAYTAlVT\n" +
            "MREwDwYDVQQIEwhWaXJnaW5pYTETMBEGA1UEBxMKQmxhY2tzYnVyZzENMAsGA1UE\n" +
            "ChMEVGVzdDENMAsGA1UECxMEVGVzdAIBATANBglghkgBZQMEAgEFAKBpMBgGCSqG\n" +
            "SIb3DQEJAzELBgkqhkiG9w0BBwEwLwYJKoZIhvcNAQkEMSIEIGInYCeXbjp4Ub3A\n" +
            "j7acLt2f3FZpiVZk34nd6bL6EQKlMBwGCSqGSIb3DQEJBTEPFw0xNjAzMTAxMzA1\n" +
            "MzdaMA0GCSqGSIb3DQEBAQUABIGASosObYm4Qz0ExfFKk0C6gM4WDfsvfP5ccz/Q\n" +
            "DTUr9uBMY0Ll6dxEpJo9iSA+3I8cqwhoBZA5vtQLKe54iliDbf50OAv6K2MLqfq5\n" +
            "znBHbv8kzS4wdxBjoSiHy0calQK60SMSgNPTQnNgC2AkHi6F6gIkYNtcmo4V2jSx\n" +
            "1CwBtWwAAAAA\n" +
            "-----END PKCS7-----";

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");

        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(forge_pkcs7PEM);
        cmsSignedMessage.isValidSignature();
        log.info("CMSSignedData: " + cmsSignedMessage.toPEMStr());
        cmsSignedMessage = CMSSignedMessage.FROM_PEM(cmsSignedMessage.toPEMStr());
        cmsSignedMessage.isValidSignature();
        /*CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(createPKCS7SignedData());
        cmsSignedMessage.isValidSignature();
        log.info("CMSSignedData: " + cmsSignedMessage.toPEMStr());
        log.info(cmsSignedMessage.toASN1Structure().toASN1Primitive().toString());*/

    }

    private static byte[] createPKCS7SignedData() throws Exception {
        SignatureService signatureService = SignatureService.genUserVSSignatureService("08888888D");
        CMSSignedMessage cmsSignedMessage = signatureService.signData("Hello");
        //cmsSignedMessage = CMSSignedMessage.addTimeStamp(cmsSignedMessage, "https://192.168.1.5/TimeStampServer/timestamp");
        byte[] pemBytes = cmsSignedMessage.toPEM();
        return pemBytes;
    }

}