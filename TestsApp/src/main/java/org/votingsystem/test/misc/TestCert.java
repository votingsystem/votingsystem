package org.votingsystem.test.misc;

import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.cert.X509Certificate;
import java.util.logging.Logger;

public class TestCert {

    private static Logger log =  Logger.getLogger(TestCert.class.getName());

    private static String certWithAnonymouesExtensionPEM = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDijCCAvOgAwIBAgIIdgMOaY+UZsIwDQYJKoZIhvcNAQEFBQAwPDESMBAGA1UE\n" +
            "BRMJOTAwMDAwMDBCMSYwJAYDVQQqEx1Wb3RpbmcgU3lzdGVtIEN1cnJlbmN5IFNl\n" +
            "cnZlcjAeFw0xNjAzMDgyMTE2NDdaFw0xNzAzMDgyMTE2NDdaMDsxDzANBgNVBAQM\n" +
            "BkdBUkNJQTEUMBIGA1UEKgwLSk9TRSBKQVZJRVIxEjAQBgNVBAUTCTA3NTUzMTcy\n" +
            "SDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAwlmO/vGEOPe8NIb7uEX3BpZL\n" +
            "Rhaau0aEGEhSZYKDkDDKkGDmDScYf8J0+HKSdrT9Y0iMOy4PHGw527A4sjBgSXX/\n" +
            "IHFUPEIngrQtq9pGcCNFPsWY8kCUBZrYGAn7xFFTblfxjMnDO34Pv6QKcflVMXMk\n" +
            "J4Hx/uS3AqElyPEtr0cCAwEAAaOCAZQwggGQMGsGA1UdIwRkMGKAFLLUIphX+dQV\n" +
            "Jy2ROL7porQA74rNoUKkQDA+MSwwKgYDVQQDDCNWb3RpbmcgU3lzdGVtIENlcnRp\n" +
            "ZmljYXRlIEF1dGhvcml0eTEOMAwGA1UECwwFQ2VydHOCBgFQFM4qGjAdBgNVHQ4E\n" +
            "FgQU2cXncxWcC9KwYwYdr+2L2YcbB1AwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8E\n" +
            "BAMCBaAwEgYJAAAAAAAAAAAFBAUxAwEBADCBzwYJAAAAAAAAAAAEBIHBMYG+DIG7\n" +
            "eyJkZXZpY2VJZCI6IjM1NTEzNjA1Njk5MDE0OSIsImRldmljZU5hbWUiOiJMR0Ug\n" +
            "TmV4dXMgNCIsImRldmljZVR5cGUiOiJNT0JJTEUiLCJlbWFpbCI6Impnem9ybm96\n" +
            "YUBnZy5iIiwiZ2l2ZW5uYW1lIjoiSk9TRSBKQVZJRVIiLCJtb2JpbGVQaG9uZSI6\n" +
            "IjYwOSIsIm5pZiI6IjA3NTUzMTcySCIsInN1cm5hbWUiOiJHQVJDSUEifTANBgkq\n" +
            "hkiG9w0BAQUFAAOBgQBYl10PrpzLDEPPBh79nLZK85jSf0iQbgzhxns/vXrp/TJv\n" +
            "zIqefu6P9X9xiJ+oXHEYerksZVPJ2lsnib7WgnMNG8o0Zd4B8NG3tOMEVNTWnc0U\n" +
            "RZlv9lx1sQFF3PKhpv42ZLzMAkO7FvHUuSWMVkpj6mpF0MwH+ecAB70Or+A4zA==\n" +
            "-----END CERTIFICATE-----";

    private static String anonymousDelegationCertPEM = "-----BEGIN CERTIFICATE-----\n" +
            "MIICzzCCAjigAwIBAgIIWgXhWcbbGCIwDQYJKoZIhvcNAQEFBQAwOzESMBAGA1UE\n" +
            "BRMJNTAwMDAwMDBSMSUwIwYDVQQqExxWb3RpbmcgU3lzdGVtIEFjY2VzcyBDb250\n" +
            "cm9sMB4XDTE2MDMwNjIzMDAwMFoXDTE2MDMwOTE5MTAzN1owaTEqMCgGA1UECwwh\n" +
            "QW5vbnltb3VzUmVwcmVzZW50YXRpdmVEZWxlZ2F0aW9uMTswOQYDVQQDDDJhY2Nl\n" +
            "c3NDb250cm9sVVJMOmh0dHBzOi8vMTkyLjE2OC4xLjUvQWNjZXNzQ29udHJvbDCB\n" +
            "nzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAlZOklzvZTLCks5rCp+A6JXjWim8f\n" +
            "89msp8+rc0xh0esvhbZx9WofU/32FdvfJZrR5/shFzYlCszn+b9K/3ntXvvSSdSz\n" +
            "bmJ2g25RlQf9/nNsRfUxexHGAkEdbNy9+1GKvM142YUNEHAH7m8GhRKVVAsCGpuj\n" +
            "++zAlemcL2KXXeECAwEAAaOBrTCBqjBrBgNVHSMEZDBigBQ2bXwb/p5m+tX09Kw6\n" +
            "YuyEJwN0L6FCpEAwPjEsMCoGA1UEAwwjVm90aW5nIFN5c3RlbSBDZXJ0aWZpY2F0\n" +
            "ZSBBdXRob3JpdHkxDjAMBgNVBAsMBUNlcnRzggYBUBTLpSowHQYDVR0OBBYEFLNS\n" +
            "gUvSXyNOuOKM3JwlyPI+eIasMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgWg\n" +
            "MA0GCSqGSIb3DQEBBQUAA4GBAFw5Cm2kEWinrqZIe9+UjzTgBz3S9ZtCb7HqDt9p\n" +
            "gQiCmYBIQ/6iEP9da+vaa0odCELW3zuUWWRp7daf60akdH/4Bz5hhVFYpKwwpovK\n" +
            "CDi+/WbTgSpnBcA1VL/pvriZhMk5kxeckj32JGbjW32Tbuhh+u5ZZaLC6dA1M+iG\n" +
            "QYb+\n" +
            "-----END CERTIFICATE-----";

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        X509Certificate anonymousDelegationCert = PEMUtils.fromPEMToX509Cert(certWithAnonymouesExtensionPEM.getBytes());
        log.info(anonymousDelegationCert.toString());
        CertExtensionDto dto = CertUtils.getCertExtensionData(CertExtensionDto.class, anonymousDelegationCert, ContextVS.DEVICE_OID);
    }

}
