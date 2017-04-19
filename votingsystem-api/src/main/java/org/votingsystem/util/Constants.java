package org.votingsystem.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.MediaType;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Constants {

    private static Logger log = Logger.getLogger(Constants.class.getName());

    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";

    public static final String CURRENCY_SOCKET_SERVICE = "wss://voting.ddns.net/currency-server/websocket/service";

    public static final String VOTING_SYSTEM_BASE_OID                  = "0.0.0.0.0.0.0.0.0.";
    public static final String VOTE_OID                                = VOTING_SYSTEM_BASE_OID + 0;
    public static final String REPRESENTATIVE_VOTE_OID                 = VOTING_SYSTEM_BASE_OID + 1;
    public static final String ANON_REPRESENTATIVE_DELEGATION_OID      = VOTING_SYSTEM_BASE_OID + 2;
    public static final String CURRENCY_OID                            = VOTING_SYSTEM_BASE_OID + 3;
    public static final String DEVICE_OID                              = VOTING_SYSTEM_BASE_OID + 4;
    public static final String ANON_CERT_OID                           = VOTING_SYSTEM_BASE_OID + 5;

    public static final int KEY_SIZE                         = 2048;
    public static final String SIG_NAME                      = "RSA";
    public static final String ALGORITHM_RNG                 = "SHA1PRNG";
    public static final String CERT_GENERATION_SIG_ALGORITHM = "SHA256WithRSAEncryption";
    public static final String SIGNATURE_ALGORITHM           = "SHA256withRSA";
    public static final String DATA_DIGEST_ALGORITHM         = "SHA256";

    public static final String QR_OPERATIONS       = "QR_OPERATIONS_KEY";
    public static final String USER_UUID           = "USER_UUID_KEY";
    public static final String CSR                 = "CSR_KEY";
    public static final String USER_KEY            = "USER_KEY";
    public static final String BROWSER_PLUBLIC_KEY = "BROWSER_PLUBLIC_KEY";

    //For tests environments
    public static final String USER_CERT_ALIAS = "userKey";
    public static final String ROOT_CERT_ALIAS = "rootKey";
    public static final String PASSW_DEMO      = "local-demo";

    public static String SETTINGS_FILE_NAME                       = "settings.properties";
    public static String WALLET_FILE_NAME                         = "wallet";
    public static String WALLET_FILE_EXTENSION                    = ".wvs";
    public static String SERIALIZED_OBJECT_EXTENSION              = ".servs";
    public static String RECEIPT_FILE_NAME                        = "receipt";
    public static String CANCEL_DATA_FILE_NAME                    = "cancellation-data";
    public static String CANCEL_BUNDLE_FILE_NAME                  = "cancellation-bundle";

    public static final String CSR_FILE_NAME                      = "csr" + ":" + ContentType.TEXT.getName();
    public static final String CERT_FILE_NAME                     = "cert" + ":" + ContentType.TEXT.getName();
    public static final String ANON_CERTIFICATE_REQUEST_FILE_NAME = "anon-cert-request" + ":" + MediaType.XML;
    public static final String CURRENCY_REQUEST_FILE_NAME         = "currency-request" + ":" + MediaType.XML;
    public static final String CMS_FILE_NAME                      = "cms" + ":" + MediaType.PKCS7_SIGNED;
    public static final String XML_SIGNED_FILE_NAME               = "signed" + ":" + MediaType.XML;
    public static final String BUNDLE_BASE_NAME                   = "org.votingsystem.messages";

    public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
    public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;

}