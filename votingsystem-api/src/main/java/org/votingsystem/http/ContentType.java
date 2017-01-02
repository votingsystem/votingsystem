package org.votingsystem.http;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum ContentType {


    BACKUP(MediaType.BACKUP, "zip"),
    JAVASCRIPT("application/javascript", "js"),

    JSON(MediaType.JSON, "json"),
    JSON_SIGNED(MediaType.JSON_SIGNED,"p7s"),
    JSON_ENCRYPTED(MediaType.JSON_ENCRYPTED,"p7s"),//.p7c
    JSON_SIGNED_ENCRYPTED(MediaType.JSON_SIGNED_ENCRYPTED, "p7s"),

    MESSAGEVS(MediaType.MESSAGEVS, "vs"),
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

    CMS_SIGNED("signed-data", null),
    SIGNED("application/pkcs7-signature","p7s"),
    ENCRYPTED(MediaType.ENCRYPTED, "p7s"),//.p7c
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
        return name.contains(MediaType.ENCRYPTED);
    }

    public boolean isSignedAndEncrypted() {
        return (name.contains("application/pkcs7-signature") && name.contains(MediaType.ENCRYPTED));
    }

    public static ContentType getByName(String contentTypeStr) {
        if(contentTypeStr == null) return null;

        if(contentTypeStr.contains(VOTE.getName())) return VOTE;

        if(contentTypeStr.contains(JSON_SIGNED_ENCRYPTED.getName())) return JSON_SIGNED_ENCRYPTED;
        if(contentTypeStr.contains(JSON_ENCRYPTED.getName())) return JSON_ENCRYPTED;
        if(contentTypeStr.contains(JSON_SIGNED.getName())) return JSON_SIGNED;
        if(contentTypeStr.contains("json")) return JSON;

        if(contentTypeStr.contains(MULTIPART_SIGNED.getName())) return MULTIPART_SIGNED;
        if(contentTypeStr.contains(TEXT.getName())) return TEXT;
        if(contentTypeStr.contains(HTML.getName())) return HTML;

        if(contentTypeStr.contains(MESSAGEVS.getName())) return MESSAGEVS;

        if(contentTypeStr.contains(ENCRYPTED.getName())) return ENCRYPTED;
        if(contentTypeStr.contains(SIGNED.getName())) return SIGNED;
        if(contentTypeStr.contains(SIGNED_AND_ENCRYPTED.getName())) return SIGNED_AND_ENCRYPTED;

        if(contentTypeStr.contains(JSON_ENCRYPTED.getName())) return JSON_ENCRYPTED;

        if(contentTypeStr.contains(JSON_SIGNED_ENCRYPTED.getName())) return JSON_SIGNED_ENCRYPTED;

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
