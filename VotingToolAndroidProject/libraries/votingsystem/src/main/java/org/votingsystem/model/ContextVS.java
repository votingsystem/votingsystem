package org.votingsystem.model;

public class ContextVS {
	
	public static final String TAG = "ContextVS";

    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";

    public static final String PREFS_ESTADO               = "state";
    public static final String PREFS_ID_SOLICTUD_CSR      = "idSolicitudCSR";
    public static final String PREFS_ID_APLICACION        = "idAplicacion";
    public static final String EVENT_KEY                  = "eventKey";
    public static final String SIGNED_FILE_NAME     = "signedFile";
    public static final String CSR_FILE_NAME              = "csr";
    public static final String ACCESS_REQUEST_FILE_NAME   = "accessRequest";
    public static final String SIGNED_PART_EXTENSION      = ".p7m";
    public static final String DEFAULT_SIGNED_FILE_NAME   = "smimeMessage.p7m";
    public static final String PROVIDER                   = "BC";
    public static final String SERVER_URL_EXTRA_PROP_NAME = "serverURL";

    public static final int KEY_SIZE = 1024;
    public static final int EVENTS_PAGE_SIZE = 30;
    public static final int MAX_SUBJECT_SIZE = 60;
    public static final int SELECTED_OPTION_MAX_LENGTH       = 60;
    //TODO por el bug en froyo de -> JcaDigestCalculatorProviderBuilder
    public static final String SIG_HASH = "SHA256";
    public static final String SIG_NAME = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    //public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    public static final String VOTE_SIGN_MECHANISM = "SHA256WithRSA";
    public static final String USER_CERT_ALIAS = "CertificadoUsuario";
    public static final String KEY_STORE_FILE = "keyStoreFile.p12";

    public static final String TIMESTAMP_USU_HASH = "2.16.840.1.101.3.4.2.1";//TSPAlgorithms.SHA256
    public static final String TIMESTAMP_VOTE_HASH = "2.16.840.1.101.3.4.2.1";//TSPAlgorithms.SHA256

    public static final String ASUNTO_MENSAJE_FIRMA_DOCUMENTO = "[Firma]-";
    public static final String VOTING_HEADER_LABEL  = "votingSystemMessageType";


    public static final String CERT_NOT_FOUND_DIALOG_ID      = "certNotFoundDialog";

}