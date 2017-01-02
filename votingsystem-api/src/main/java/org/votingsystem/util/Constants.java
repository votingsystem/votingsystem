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

    public static final String VOTING_SYSTEM_BASE_OID                  = "0.0.0.0.0.0.0.0.0.";
    public static final String VOTE_OID                                = VOTING_SYSTEM_BASE_OID + 0;
    public static final String REPRESENTATIVE_VOTE_OID                 = VOTING_SYSTEM_BASE_OID + 1;
    public static final String ANON_REPRESENTATIVE_DELEGATION_OID = VOTING_SYSTEM_BASE_OID + 2;
    public static final String CURRENCY_OID                            = VOTING_SYSTEM_BASE_OID + 3;
    public static final String DEVICE_OID                              = VOTING_SYSTEM_BASE_OID + 4;
    public static final String ANON_CERT_OID                           = VOTING_SYSTEM_BASE_OID + 5;

    public static final int KEY_SIZE = 2048;
    public static final String SIG_NAME = "RSA";
    public static final String ALGORITHM_RNG = "SHA1PRNG";
    public static final String CERT_GENERATION_SIG_ALGORITHM = "SHA256WithRSAEncryption";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String DATA_DIGEST_ALGORITHM = "SHA256";
    //For tests environments
    public static final String USER_CERT_ALIAS = "userKey";
    public static final String ROOT_CERT_ALIAS = "rootKey";
    public static final String PASSW_DEMO = "local-demo";

    private String appDir;
    private String tempDir;

    public static String SETTINGS_FILE_NAME                          = "settings.properties";
    public static String WALLET_FILE_NAME                            = "wallet";
    public static String WALLET_FILE_EXTENSION                       = ".wvs";
    public static String SERIALIZED_OBJECT_EXTENSION                 = ".servs";
    public static String RECEIPT_FILE_NAME                           = "receipt";
    public static String CANCEL_DATA_FILE_NAME                       = "cancellationDataVS";
    public static String CANCEL_BUNDLE_FILE_NAME                     = "cancellationBundleVS";

    public static final String CSR_FILE_NAME                         = "csr" + ":" + ContentType.TEXT.getName();
    public static final String ANON_CERTIFICATE_REQUEST_FILE_NAME    = "anonCertRequest" + ":" + MediaType.XML;
    public static final String CURRENCY_REQUEST_FILE_NAME            = "currencyRequest" + ":" + MediaType.XML;
    public static final String CMS_FILE_NAME                         = "cms" + ":" + MediaType.JSON_SIGNED;
    public static final String CMS_ANON_FILE_NAME                    = "cmsAnonymous" + ":" + MediaType.JSON_SIGNED;

    public static final String BUNDLE_BASE_NAME                      = "org.votingsystem.messages";
    public static final String TIMESTAMP_SERVER_DIR_KEY              = "timestamp_server_dir";

    public static final int SIGNED_MAX_FILE_SIZE_KB = 512;
    public static final int SIGNED_MAX_FILE_SIZE = SIGNED_MAX_FILE_SIZE_KB * 1024;

}