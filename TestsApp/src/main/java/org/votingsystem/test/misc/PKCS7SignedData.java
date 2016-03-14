package org.votingsystem.test.misc;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

public class PKCS7SignedData {

    private static Logger log =  Logger.getLogger(PKCS7SignedData.class.getName());

    private static String forge_pkcs7PEM = "-----BEGIN PKCS7-----\n" +
            "MIIHeQYJKoZIhvcNAQcCoIIHajCCB2YCAQExDzANBglghkgBZQMEAgEFADAzBgkq\n" +
            "hkiG9w0BBwGgJgQkSGVsbG8gc2lnbmVkIGFuZCBUaW1lU3RhbXBlZCBtZXNzYWdl\n" +
            "oIIDCDCCAwQwggJtoAMCAQICAQEwDQYJKoZIhvcNAQEFBQAwaTEUMBIGA1UEAxML\n" +
            "ZXhhbXBsZS5vcmcxCzAJBgNVBAYTAlVTMREwDwYDVQQIEwhWaXJnaW5pYTETMBEG\n" +
            "A1UEBxMKQmxhY2tzYnVyZzENMAsGA1UEChMEVGVzdDENMAsGA1UECxMEVGVzdDAe\n" +
            "Fw0xNjAzMTMyMTUzNDRaFw0xNzAzMTMyMTUzNDRaMGkxFDASBgNVBAMTC2V4YW1w\n" +
            "bGUub3JnMQswCQYDVQQGEwJVUzERMA8GA1UECBMIVmlyZ2luaWExEzARBgNVBAcT\n" +
            "CkJsYWNrc2J1cmcxDTALBgNVBAoTBFRlc3QxDTALBgNVBAsTBFRlc3QwgZ8wDQYJ\n" +
            "KoZIhvcNAQEBBQADgY0AMIGJAoGBANtju0rOVtKeDFgUa6DflNB4noqAv490fcnc\n" +
            "myQOT8IufnCjpUo+ljiFe07iPeIN6DCv3SJMo8f3aNVZa1AryQ64j/hzkrYVlXNp\n" +
            "fq5rUqf43aNednF+/huEEtFRO0NVEQotn19LAgF0zcSNgajz83a6yqzgbh9Dvf8a\n" +
            "Nig1bdeBAgMBAAGjgbswgbgwDAYDVR0TBAUwAwEB/zALBgNVHQ8EBAMCAvQwOwYD\n" +
            "VR0lBDQwMgYIKwYBBQUHAwEGCCsGAQUFBwMCBggrBgEFBQcDAwYIKwYBBQUHAwQG\n" +
            "CCsGAQUFBwMIMBEGCWCGSAGG+EIBAQQEAwIA9zAsBgNVHREEJTAjhhtodHRwOi8v\n" +
            "ZXhhbXBsZS5vcmcvd2ViaWQjbWWHBH8AAAEwHQYDVR0OBBYEFP/ieNNvhReXUyXv\n" +
            "fxoaObT1uSgeMA0GCSqGSIb3DQEBBQUAA4GBAH3AG1iO7FNAUdLiiP9ywacTiF3e\n" +
            "zC5lADs4SAqNQCtbkL6KLY8hWWzJKg5NWMmlog1QixS/0Fl+tanxphjipPd9IwGl\n" +
            "3C3cjmEnNpuAnpVKvPg1Fiww/Ufo7Xd0Z2QF3x3ZUKRgA1+//Oc9XDBXzK+SiMwq\n" +
            "JPxsz/fqSPYxcRVrMYIEDTCCBAkCAQEwbjBpMRQwEgYDVQQDEwtleGFtcGxlLm9y\n" +
            "ZzELMAkGA1UEBhMCVVMxETAPBgNVBAgTCFZpcmdpbmlhMRMwEQYDVQQHEwpCbGFj\n" +
            "a3NidXJnMQ0wCwYDVQQKEwRUZXN0MQ0wCwYDVQQLEwRUZXN0AgEBMA0GCWCGSAFl\n" +
            "AwQCAQUAoIIC8TAYBgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMC8GCSqGSIb3DQEJ\n" +
            "BDEiBCBBs48lHhfA0D0aaQ73ds9vrTe489CJbnWTYiAFT/iyADAcBgkqhkiG9w0B\n" +
            "CQUxDxcNMTYwMzEzMjE1MzQ1WjCCAoQGCyqGSIb3DQEJEAIOMYICczCCAm8GCSqG\n" +
            "SIb3DQEHAqCCAmAwggJcAgEDMQ8wDQYJYIZIAWUDBAIBBQAwegYLKoZIhvcNAQkQ\n" +
            "AQSgayRpBGcwZQIBAQYCKgMwMTANBglghkgBZQMEAgEFAAQgQbOPJR4XwNA9GmkO\n" +
            "93bPb603uPPQiW51k2IgBU/4sgACCC7sbremI3TRGA8yMDE2MDMxMzIxNTM0NVow\n" +
            "CwIBAYACAfSBAgH0AgEBMYIByDCCAcQCAQEwSDA+MSwwKgYDVQQDDCNWb3Rpbmcg\n" +
            "U3lzdGVtIENlcnRpZmljYXRlIEF1dGhvcml0eTEOMAwGA1UECwwFQ2VydHMCBgFQ\n" +
            "E+9HUDANBglghkgBZQMEAgEFAKCB0zAaBgkqhkiG9w0BCQMxDQYLKoZIhvcNAQkQ\n" +
            "AQQwHAYJKoZIhvcNAQkFMQ8XDTE2MDMxMzIxNTM0NVowLQYJKoZIhvcNAQk0MSAw\n" +
            "HjANBglghkgBZQMEAgEFAKENBgkqhkiG9w0BAQEFADAvBgkqhkiG9w0BCQQxIgQg\n" +
            "HIm6T+gKEN6+K8JAES5T+wzb62HbB08BVBKHwRochTcwNwYLKoZIhvcNAQkQAi8x\n" +
            "KDAmMCQwIgQgbEM6dKdJT+JW3VKXkg/OVMkftphgd2vCQ2Pe3G735fgwDQYJKoZI\n" +
            "hvcNAQEBBQAEgYAc3HzbIuIgYV/oES0OxI59wc6nCSD3t66/9lBEBY89guw5sihT\n" +
            "rQe7EkAlyeZNk4BQE1GGNMqBaGbbLZQsi/WXWS92J/iRdDWkjG/MTAMSNSsJfcuo\n" +
            "4p7wG6ERr6xSNp/fOJZKOzGldUhNQpoDaiZGiy0aaPED8mWbEh9rn7udezANBgkq\n" +
            "hkiG9w0BAQEFAASBgFSm+Wha+/OUh31i8yvZTisUQfb6BOUGM3S7EQxi5u0ItfnK\n" +
            "omloFjybCfmT86KrCUG3MzP1Oi4V8WuEHWipcrxDztJwP4/+ju67NncEa/RUsZsW\n" +
            "S8VhxN4EuJIKwSwL3uw6iWzcc/hXt/b0LA8QipC8bAUMWfHytnq48ZpLSFY1\n" +
            "-----END PKCS7-----\n";

    private static String pkcs7PEM = "-----BEGIN PKCS7-----\n" +
            "MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0B\n" +
            "BwGggCSABBVIZWxsbyBUaW1lU3RhbXBTaWduZWQAAAAAAACggDCCA1YwggK/oAMC\n" +
            "AQICCH3kQ3xv8F+ZMA0GCSqGSIb3DQEBCwUAMFkxEjAQBgNVBAUTCTA4ODg4ODg4\n" +
            "RDEiMCAGA1UEBBMZVGVzdHMtVm90aW5nU3lzdGVtU3VybmFtZTEfMB0GA1UEKhMW\n" +
            "VGVzdHMtVm90aW5nU3lzdGVtTmFtZTAeFw0xNjAzMTIxNTM3MjZaFw0xNzAzMTIx\n" +
            "NTM3MjZaME8xEjAQBgNVBAUTCTA4ODg4ODg4RDEbMBkGA1UEBAwSbGFzdE5hbWVf\n" +
            "MDg4ODg4ODhEMRwwGgYDVQQqDBNGaXJzdE5hbWVfMDg4ODg4ODhEMIIBIjANBgkq\n" +
            "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1dNzF9fVJHfe7I+Zo4TT1tkJ/MPlKcti\n" +
            "kUmL/IQeosCmNliopP2XjwIqQ3iugwnEfLzZPX4+JQ/cwfcIBUXWZfQi/566C7O8\n" +
            "Y6mSNf59EBVqk6piqLTXhq0Z7GkVm9pD3WAewJzR1Vlk/kgy/8mv2cItWiZhIG6H\n" +
            "UaSExZ/uPKpg+aYBcp3Uh39j5Wmi2v5DbQf3rYVQzIZWt+GmQZIS4dqI+o0Ieqwi\n" +
            "L+wkcQ7319iy8tMMwn4Ubq0VajDF0S6+hJQxTqkZ5ZdlkLf1Ndxk3UOoq4JNB0ay\n" +
            "HY1uy6hGmrqkYjkrVD3fcxamLOJJ6yGKtPEFQd2Pwdfnz17MB3/atQIDAQABo4Gs\n" +
            "MIGpMGoGA1UdIwRjMGGAFAM8opx4gmlE7xIlHNxRlr4msrD5oT+kPTA7MRIwEAYD\n" +
            "VQQFEwk1MDAwMDAwMFIxJTAjBgNVBAMTHFZvdGluZyBTeXN0ZW0gQWNjZXNzIENv\n" +
            "bnRyb2yCCFb7x+YQceZWMB0GA1UdDgQWBBTaIGLwsm//gUzEqMAzfz8KK1U6UTAM\n" +
            "BgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQEAwIFoDANBgkqhkiG9w0BAQsFAAOBgQAR\n" +
            "LWyo5EbY51QntP1tq10FM6auARVr0JZXP4xzSQVzHvHNg8pMiUmmzSxTVKN+U5Ju\n" +
            "C2IB5SrzGtGWNI9HZNiADDaxOUnuafbQ/pGSGHt4dIPM/GLYtl53mO8enMNTyS0E\n" +
            "AOUPP0DG8UZCBRhreRO8g86wQ2lopyFf9+M2Q+cszjCCAsEwggIqoAMCAQICCFb7\n" +
            "x+YQceZWMA0GCSqGSIb3DQEBBQUAMDsxEjAQBgNVBAUTCTUwMDAwMDAwUjElMCMG\n" +
            "A1UEAxMcVm90aW5nIFN5c3RlbSBBY2Nlc3MgQ29udHJvbDAeFw0xNDEyMDExMjIw\n" +
            "NTdaFw0xNTExMzAyMzAwMDBaMFkxEjAQBgNVBAUTCTA4ODg4ODg4RDEiMCAGA1UE\n" +
            "BBMZVGVzdHMtVm90aW5nU3lzdGVtU3VybmFtZTEfMB0GA1UEKhMWVGVzdHMtVm90\n" +
            "aW5nU3lzdGVtTmFtZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAgimbCUSy\n" +
            "KHJh1eOqgpx8Mz2UZsxtntin2Pkxo/nEiaviyXduU+dOS5i7LodHmEkxD1KO0Cc2\n" +
            "Q0rRlFSxBJaasDwgjTD64bOAt925slNvS1Hcu4IJ+WQr7S7XRl5GlQTjcBtBSLzQ\n" +
            "hiZePjt9M4kW0hnPgt0HXN77vLL+XVcVx9cCAwEAAaOBrzCBrDBtBgNVHSMEZjBk\n" +
            "gBR5ZuroHheWMR8LG5r7uBquVdW+WaFCpEAwPjEsMCoGA1UEAwwjVm90aW5nIFN5\n" +
            "c3RlbSBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkxDjAMBgNVBAsMBUNlcnRzgggkPrWq\n" +
            "aEVFoTAdBgNVHQ4EFgQUAzyinHiCaUTvEiUc3FGWviaysPkwDAYDVR0TAQH/BAIw\n" +
            "ADAOBgNVHQ8BAf8EBAMCBaAwDQYJKoZIhvcNAQEFBQADgYEACV/OVgQkaGuh0fNl\n" +
            "3kDh/lxHKcZUm1CBBv4UH5409FotfrFxBC8qDn3cH+2faxAClIS2Qel7H2wDL4Db\n" +
            "at06WNrhwFNMMpJBudKLvgXPniHvPT2GpSbJthZVYmU11ueQeSWMzQoeQFmyQXzt\n" +
            "dQb0JTvBVr+3Z8LRw4A6+wyz8ugAADGCBK8wggSrAgEBMGUwWTESMBAGA1UEBRMJ\n" +
            "MDg4ODg4ODhEMSIwIAYDVQQEExlUZXN0cy1Wb3RpbmdTeXN0ZW1TdXJuYW1lMR8w\n" +
            "HQYDVQQqExZUZXN0cy1Wb3RpbmdTeXN0ZW1OYW1lAgh95EN8b/BfmTANBglghkgB\n" +
            "ZQMEAgEFAKCCAxswGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAcBgkqhkiG9w0B\n" +
            "CQUxDxcNMTYwMzEyMTUzNzI4WjAtBgkqhkiG9w0BCTQxIDAeMA0GCWCGSAFlAwQC\n" +
            "AQUAoQ0GCSqGSIb3DQEBAQUAMC8GCSqGSIb3DQEJBDEiBCAz53wtDpEXUUmcdxFg\n" +
            "YkedRqt4FT/FXJiWYEhQf7PdTDCCAn8GCyqGSIb3DQEJEAIOMYICbjCCAmoGCSqG\n" +
            "SIb3DQEHAqCCAlswggJXAgEDMQ8wDQYJYIZIAWUDBAIBBQAwdQYLKoZIhvcNAQkQ\n" +
            "AQSgZgRkMGICAQEGAioDMDEwDQYJYIZIAWUDBAIBBQAEIHrBVBAV+v6Fa84+bI8H\n" +
            "i146KLqgT/PopAKV3+ZUduoHAghqv1osZ5kSKhgPMjAxNjAzMTIxNTM3MjhaMAsC\n" +
            "AQGAAgH0gQIB9DGCAcgwggHEAgEBMEgwPjEsMCoGA1UEAwwjVm90aW5nIFN5c3Rl\n" +
            "bSBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkxDjAMBgNVBAsMBUNlcnRzAgYBUBPvR1Aw\n" +
            "DQYJYIZIAWUDBAIBBQCggdMwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwG\n" +
            "CSqGSIb3DQEJBTEPFw0xNjAzMTIxNTM3MjhaMC0GCSqGSIb3DQEJNDEgMB4wDQYJ\n" +
            "YIZIAWUDBAIBBQChDQYJKoZIhvcNAQEBBQAwLwYJKoZIhvcNAQkEMSIEIMvKpu0s\n" +
            "NwHb2EMXaqKc0TsBXMsSnbf3UFdjKNYROQFoMDcGCyqGSIb3DQEJEAIvMSgwJjAk\n" +
            "MCIEIGxDOnSnSU/iVt1Sl5IPzlTJH7aYYHdrwkNj3txu9+X4MA0GCSqGSIb3DQEB\n" +
            "AQUABIGAfA+ugb81W1rr7WtDxAEu9G0g3XWeCDSuHSk8xVtVENlQzEl/JRJPeJH/\n" +
            "iumJtWDd0ycveYe1BZ5xxa+wJ203I0J0qamIdYCvyncm8QMsTu/f6G3hFsfmhNzb\n" +
            "vO3210tSVzVKgrB/h6QmMeUqFI7/vRDHMn4DoB+QO4MaSN5Y+IcwDQYJKoZIhvcN\n" +
            "AQEBBQAEggEAndRoy/Df4mg0dIxcwQPVzmykzzqeg4QrrPDawFtInxoPcqsGuHZD\n" +
            "3lAJifVLTOMYKGWCv4lRFg9HBjslGJT4p3c6DsB/shInEbzbBdauFMxIHGYHZNdY\n" +
            "FP7UIlV+X9xnjf64Ri17VCTpdwPwdFBg+EM6TpST3JEFVTIW7x71JTJZ4CBnL785\n" +
            "eYgdgxTMWfggIsUn+dPkQFGgkp4hz/7bBQwFF0gmjBeV+z1Dwav3COMDor2b9fc7\n" +
            "aZjGszaRfE0veZSY7F6VgD4wELaSLFuFkK0tlyHDczLlID2uLiJZvHVZLsBXJ45b\n" +
            "kCxYk9KKPJh+cTBOaNBev5OhAPsHzFmK/wAAAAAAAA==\n" +
            "-----END PKCS7-----";

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");

        /*CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(forge_pkcs7PEM);
        cmsSignedMessage.isValidSignature();
        log.info("timstamp: " + cmsSignedMessage.getTimeStampToken().getTimeStampInfo().getGenTime());
        byte[] pemBytes = createCMSSignedDataWithTimeStampSigned();
        log.info(new String(pemBytes));
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(pemBytes);
        cmsSignedMessage.isValidSignature();
        TimeStampToken timeStampToken = cmsSignedMessage.getSigner().getTimeStampToken();*/

        //log.info("TimeStamp - MessageImprintDigest: " + new String(timeStampToken.getTimeStampInfo().getMessageImprintDigest()));
        //log.info(new String(pemBytes));
    }


    private static byte[] createCMSSignedDataWithTimeStampSigned() throws Exception {
        SignatureService signatureService = SignatureService.genUserVSSignatureService("08888888D");
        CMSSignedMessage cmsSignedMessage = signatureService.signDataWithTimeStamp("Hello TimeStampSigned".getBytes());
        byte[] pemBytes = cmsSignedMessage.toPEM();
        return pemBytes;
    }
}