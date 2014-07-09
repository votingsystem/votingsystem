package org.votingsystem.model;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * S/MIME signatures are usually "detached signatures": the signature information is separate
 * from the text being signed. The MIME type for this is multipart/signed with the second part
 * having a MIME subtype of application/(x-)pkcs7-signature
 */
public enum ContentTypeVS {

    BACKUP("application/backup", "zip"),
    JAVASCRIPT("application/javascript", "js"),

    JSON("application/json;charset=UTF-8", "json"),
    JSON_SIGNED("application/json;application/pkcs7-signature","p7s"),
    JSON_ENCRYPTED("application/json;application/pkcs7-mime","p7m"),//.p7c
    JSON_SIGNED_AND_ENCRYPTED("application/json;application/pkcs7-signature;application/pkcs7-mime", "p7m"),

    MULTIPART_SIGNED("multipart/signed", null),
    MULTIPART_ENCRYPTED("multipart/encrypted", null),
    MESSAGEVS("application/pkcs7-messagevs", "vs"),
    PDF("application/pdf", "pdf"),
    PDF_SIGNED_AND_ENCRYPTED("application/pdf;application/pkcs7-signature;application/pkcs7-mime", "pdf"),
    PDF_SIGNED("application/pdf;application/pkcs7-signature", "pdf"),
    PDF_ENCRYPTED("application/pdf;application/pkcs7-mime", "pdf"),
    TEXT("text/plain", "txt"),
    HTML("text/html", "html"),
    TEXT_STREAM("text/plain", "txt"),
    TIMESTAMP_QUERY("timestamp-query", null),
    TIMESTAMP_RESPONSE("timestamp-response", null),

    ZIP("application/zip", "zip"),
    IMAGE("application/image", null),

    OCSP_REQUEST("application/ocsp-request", null),
    OCSP_RESPONSE("application/ocsp-response", null),

    CMS_SIGNED("signed-data", null),
    SIGNED("application/pkcs7-signature","p7s"),
    ENCRYPTED("application/pkcs7-mime","p7m"),//.p7c
    SIGNED_AND_ENCRYPTED("application/pkcs7-signature;application/pkcs7-mime", "p7m"),

    PKCS7_CERT("application/pkcs7-certificates","p7b"),//.spc
    PKCS7_CERT_REQ_RESP("application/pkcs7-certreqresp","p7r"),
    PKCS7_CRL("application/pkcs7-crl","crl"),
    PKCS12("application/pkcs12","p12"),//.pfx
    PKIX_CRL("application/pkix-crl", "crl"),
    PKIX_CERT("application/pkix-cert", "cer"),
    PKCS10("application/pkcs10", "p10"),//.csr
    PEM("application/pem-file", "pem"),

    VOTE("application/vote;application/pkcs7-signature;application/pkcs7-mime", "vote"),

    VICKET("application/model;application/pkcs7-signature;application/pkcs7-mime", "model"),

    X509_CA("application/x509-ca-cert", "crt"),
    X509_USER("application/x509-user-cert", "crt");

    private String name;
    private String extension;

    private ContentTypeVS(String name, String extension) {
        this.name = name;
        this.extension = "." + extension;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isPKCS7() {
        return name.contains("pkcs7");
    }

    public boolean isJSON() {
        return name.contains("application/json");
    }

    public boolean isVOTE() {
        return name.contains("application/vote");
    }

    public boolean isSigned() {
        return name.contains("application/pkcs7-signature");
    }

    public boolean isEncrypted() {
        return (name.contains("application/pkcs7-mime") || name.contains("multipart/encrypted"));
    }

    public boolean isSignedAndEncrypted() {
        return (name.contains("application/pkcs7-signature") && name.contains("application/pkcs7-mime"));
    }

    public static ContentTypeVS getByName(String contentTypeStr) {
        if(contentTypeStr == null) return null;

        ContentTypeVS result = null;

        if(contentTypeStr.contains(TEXT.getName())) result = TEXT;

        if(contentTypeStr.contains(HTML.getName())) result = HTML;

        if(contentTypeStr.contains(ENCRYPTED.getName())) result = ENCRYPTED;
        if(contentTypeStr.contains(SIGNED.getName())) result = SIGNED;
        if(contentTypeStr.contains(SIGNED_AND_ENCRYPTED.getName())) result = SIGNED_AND_ENCRYPTED;

        if(contentTypeStr.contains(PDF.getName())) result = PDF;
        if(contentTypeStr.contains(PDF_ENCRYPTED.getName())) result = PDF_ENCRYPTED;
        if(contentTypeStr.contains(PDF_SIGNED.getName())) result = PDF_SIGNED;
        if(contentTypeStr.contains(PDF_SIGNED_AND_ENCRYPTED.getName())) result = PDF_SIGNED_AND_ENCRYPTED;

        if(contentTypeStr.contains(JSON.getName())) result = JSON;
        if(contentTypeStr.contains(JSON_ENCRYPTED.getName())) result = JSON_ENCRYPTED;
        if(contentTypeStr.contains(JSON_SIGNED.getName())) result = JSON_SIGNED;
        if(contentTypeStr.contains(JSON_SIGNED_AND_ENCRYPTED.getName())) result = JSON_SIGNED_AND_ENCRYPTED;

        if(contentTypeStr.contains(MULTIPART_ENCRYPTED.getName())) result = MULTIPART_ENCRYPTED;
        if(contentTypeStr.contains(MULTIPART_SIGNED.getName())) result = MULTIPART_SIGNED;

        if(contentTypeStr.contains(TIMESTAMP_QUERY.getName())) result = TIMESTAMP_QUERY;
        if(contentTypeStr.contains(TIMESTAMP_RESPONSE.getName())) result = TIMESTAMP_RESPONSE;

        if(contentTypeStr.contains(VOTE.getName())) result = VOTE;

        if(contentTypeStr.contains(VICKET.getName())) result = VICKET;

        return result;
    }

    public static ContentTypeVS getByExtension(String extensionStr) {
        if(extensionStr == null) return null;
        if(extensionStr.contains(PDF.getExtension())) return PDF;
        if(extensionStr.contains(ZIP.getExtension())) return ZIP;
        if(extensionStr.contains(TEXT.getExtension())) return TEXT;
        if(extensionStr.contains(JSON.getExtension())) return JSON;
        if(extensionStr.contains(JAVASCRIPT.getExtension())) return JAVASCRIPT;
        return null;
    }

}
