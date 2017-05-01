package org.currency.test;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.FileUtils;
import org.votingsystem.xml.XML;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;


public class Test extends BaseTest {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    private static final String cert = "-----BEGIN CERTIFICATE-----\n" +
            "MIID0jCCArqgAwIBAgIGAVsa8VoyMA0GCSqGSIb3DQEBCwUAMCwxGjAYBgNVBAMM\n" +
            "EUZBS0UgUk9PVCBETkllIENBMQ4wDAYDVQQLDAVDZXJ0czAeFw0xNzAzMjkxNDQx\n" +
            "MTZaFw0xODAzMjkxNDQxMTZaMCsxKTAnBgNVBCoTIFZvdGluZy1DdXJyZW5jeSBU\n" +
            "aW1lc3RhbXAgU2VydmVyMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
            "gqSf2rhupBgWb/AzvNNtdqipuMSh4ys7sAKDEAlFxNgqMYU5Ql8feOhD9ufE3IC6\n" +
            "ZX5fwq6Qz1VsNbXR2BgnKIqIoqyHvU8uJpTx+/tbI/u4cnhJzAnouj6aUWS7NijV\n" +
            "mWYbBQrpGxDxV1DYkwylTegWsZrVYDevhwikqF1QdT6a5op0vN1mwFuYN4Sz/CHA\n" +
            "yN7IyImi5AZjUs7dM90dQM0KmQX8cel85FKQ95OKOx2iWlYfPUn6LDUQRblE4fWH\n" +
            "ZFypKZtak/9kmnDd7QUyIQICNAOFpfOKF69FJnyxijBG2NTP/7MhAEm93k+IYwZm\n" +
            "ODCVoF8afcsAPUi9L0LAtwIDAQABo4H6MIH3MEMGCCsGAQUFBwEBBDcwNTAzBggr\n" +
            "BgEFBQcwAYYnaHR0cHM6Ly92b3RpbmcuZGRucy5uZXQvaWRwcm92aWRlci9vY3Nw\n" +
            "MFsGA1UdIwRUMFKAFD2Y24ezQwtgwwMwPWJqsmvQvFwwoTCkLjAsMRowGAYDVQQD\n" +
            "DBFGQUtFIFJPT1QgRE5JZSBDQTEOMAwGA1UECwwFQ2VydHOCCG/MMPi8INTqMB0G\n" +
            "A1UdDgQWBBShbf0Xe98ALj1L2npNcO1SCYCsgjAMBgNVHRMBAf8EAjAAMA4GA1Ud\n" +
            "DwEB/wQEAwIFoDAWBgNVHSUBAf8EDDAKBggrBgEFBQcDCDANBgkqhkiG9w0BAQsF\n" +
            "AAOCAQEAncUj9+VvMn2ltZucIvDLsmIIbQ86t+pmj+ziNWSPRaMQQQZ/Ta7F4aYm\n" +
            "2PtIOI5g8UhS+fT3nc14pXk7o2woDnQkG/6fK2IwFQ3KfoK2aDU6zohHGx7BPYPu\n" +
            "szYj8K/pHjjzg5Ms17YfL45DIu/tDgj6O0aD9tjFsAfsL13aFYfByDNLea3PgzK9\n" +
            "nBMxEtMMIxnCmgiSkNGd+KcJxfRqwM9JFF+xUVbaD+7t1ldCQmYP2pytrEfta0Wt\n" +
            "GUPhFUWeWv2WGJ3rltYZSNi1Hq/k6xJi7GPWIxqhV5KAMOBjiydtBpyq6/ydZJb4\n" +
            "ANIhuEjRswAybVljlrQcMBQmjFyZRg==\n" +
            "-----END CERTIFICATE-----\n";

    public static void main(String[] args) throws Exception {
        new Test().test();
        System.exit(0);
    }

    public void test() throws Exception {
        URL res = Thread.currentThread().getContextClassLoader().getResource("timestamp-servers.xml");
        byte[] fileBytes = FileUtils.getBytesFromStream(res.openStream());
        List<String> certificates = XML.getMapper().readValue(fileBytes, new TypeReference<List<String>>() {});
        log.info("certificates.size: " + certificates.size());
        for(String certificate : certificates) {
            log.info(PEMUtils.fromPEMToX509Cert(certificate.trim().getBytes()).toString());
        }

    }



}
