package org.votingsystem.model;

public class ContentTypeVS {

    /**
     * application/x-pkcs12	             p12
     * application/x-pkcs12	             pfx
     * application/x-pkcs7-certificates	 p7b
     * application/x-pkcs7-certificates	 spc
     * application/x-pkcs7-certreqresp	 p7r
     * application/x-pkcs7-mime	         p7c
     * application/x-pkcs7-mime	         p7m
     * application/x-pkcs7-signature	 p7s
     * application/pkcs10	             p10
     * application/pkix-crl	             crl
     */

    public static final String JSON                     = "application/json";
    public static final String TEXT                     = "text/plain";
    public static final String JAVASCRIPT               = "application/javascript";
    public static final String MULTIPART_ENCRYPTED      = "multipart/encrypted";
    public static final String TIMESTAMP_QUERY          = "timestamp-query";
    public static final String PDF                      = "application/pdf";
    public static final String BACKUP                   = "application/zip";

    public static final String OCSP_REQUEST             = "application/ocsp-request";
    public static final String OCSP_RESPONSE            = "application/ocsp-response";

    //S/MIME signatures are usually "detached signatures": the signature information is separate from the text being
    // signed. The MIME type for this is multipart/signed with the second part having a MIME subtype of application/(x-)pkcs7-signature
    public static final String MULTIPART_SIGNED         = "multipart/signed";
    public static final String SIGNED                   = "application/x-pkcs7-signature";
    public static final String X509                     = "application/x-x509-ca-cert";
    public static final String ENCRYPTED                = "application/x-pkcs7-mime";
    public static final String SIGNED_AND_ENCRYPTED     = SIGNED + ";" + ENCRYPTED;
    public static final String PDF_SIGNED_AND_ENCRYPTED = PDF + ";" +  SIGNED + ";" + ENCRYPTED;
    public static final String PDF_SIGNED               = PDF + ";" + SIGNED;
    public static final String PDF_ENCRYPTED            = PDF + ";" + ENCRYPTED;
    
}
