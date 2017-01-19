package org.votingsystem.http;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum ContentType {


    BACKUP(MediaType.BACKUP, "zip"),
    JAVASCRIPT("application/javascript", "js"),

    XML(MediaType.XML, "xml"),

    JSON(MediaType.JSON, "json"),
    PKCS7_SIGNED(MediaType.PKCS7_SIGNED, "p7s"),
    PKCS7_ENCRYPTED(MediaType.PKCS7_ENCRYPTED, "p7s"),//.p7c
    PKCS7_SIGNED_ENCRYPTED(MediaType.PKCS7_SIGNED_ENCRYPTED, "p7s"),

    MESSAGE(MediaType.MESSAGE, "vs"),
    MULTIPART_SIGNED("multipart/signed", null),
    MULTIPART_ENCRYPTED(MediaType.MULTIPART_ENCRYPTED, null),
    TEXT("text/plain", "txt"),
    HTML("text/html", "html"),
    TEXT_STREAM("text/plain", "txt"),
    TIMESTAMP_QUERY("application/timestamp-query", null),
    TIMESTAMP_RESPONSE("application/timestamp-response", null),

    ZIP(MediaType.ZIP, "zip"),
    IMAGE("application/image", null),

    OCSP_REQUEST("application/ocsp-request", null),
    OCSP_RESPONSE("application/ocsp-response", null),


    SIGNED_AND_ENCRYPTED("application/pkcs7-signature;application/pkcs7-encrypted", "p7s"),

    PKCS7_CERT("application/pkcs7-certificates","p7b"),//.spc
    PKCS7_CERT_REQ_RESP("application/pkcs7-certreqresp","p7r"),
    PKCS7_CRL("application/pkcs7-crl","crl"),
    PKCS12("application/pkcs12","p12"),//.pfx
    PKIX_CRL("application/pkix-crl", "crl"),
    PKIX_CERT("application/pkix-cert", "cer"),
    PKCS10("application/pkcs10", "p10"),//.csr
    PEM(MediaType.PEM, "pem"),

    VOTE(MediaType.VOTE, "vote"),

    X509_CA("application/x509-ca-cert", "crt"),
    X509_USER("application/x509-user-cert", "crt");

    private String name;
    private String extension;

    private ContentType(String name, String extension) {
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
        return name.contains(MediaType.PKCS7_SIGNED);
    }

    public boolean isSignedAndEncrypted() {
        return (name.contains(MediaType.PKCS7_SIGNED) && name.contains(MediaType.PKCS7_SIGNED_ENCRYPTED));
    }

    public static ContentType getByName(String contentTypeStr) {
        if(contentTypeStr == null) return null;

        if(contentTypeStr.contains(VOTE.getName())) return VOTE;

        if(contentTypeStr.contains(PKCS7_SIGNED_ENCRYPTED.getName())) return PKCS7_SIGNED_ENCRYPTED;
        if(contentTypeStr.contains(PKCS7_ENCRYPTED.getName())) return PKCS7_ENCRYPTED;
        if(contentTypeStr.contains(PKCS7_SIGNED.getName())) return PKCS7_SIGNED;
        if(contentTypeStr.contains("json")) return JSON;

        if(contentTypeStr.contains(MULTIPART_SIGNED.getName())) return MULTIPART_SIGNED;
        if(contentTypeStr.contains(TEXT.getName())) return TEXT;
        if(contentTypeStr.contains(HTML.getName())) return HTML;

        if(contentTypeStr.contains(MESSAGE.getName())) return MESSAGE;

        if(contentTypeStr.contains(SIGNED_AND_ENCRYPTED.getName())) return SIGNED_AND_ENCRYPTED;

        if(contentTypeStr.contains(MULTIPART_ENCRYPTED.getName())) return MULTIPART_ENCRYPTED;

        if(contentTypeStr.contains(TIMESTAMP_QUERY.getName())) return TIMESTAMP_QUERY;
        if(contentTypeStr.contains(TIMESTAMP_RESPONSE.getName())) return TIMESTAMP_RESPONSE;

        return null;
    }

    public static ContentType getByExtension(String extensionStr) {
        if(extensionStr == null) return null;
        if(extensionStr.contains(ZIP.getExtension())) return ZIP;
        if(extensionStr.contains(TEXT.getExtension())) return TEXT;
        if(extensionStr.contains(JSON.getExtension())) return JSON;
        if(extensionStr.contains(JAVASCRIPT.getExtension())) return JAVASCRIPT;
        return null;
    }

}
