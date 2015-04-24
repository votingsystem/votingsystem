package org.votingsystem.util;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * S/MIME signatures are usually "detached signatures": the signature information is separate
 * from the text being signed. The MIME type for this is multipart/signed with the second part
 * having a MIME subtype of application/(x-)pkcs7-signature
 */
public enum ContentTypeVS {


    BACKUP(MediaTypeVS.BACKUP, "zip"),
    JAVASCRIPT("application/javascript", "js"),
    ASCIIDOC("asciidoc", "asciidoc"),
    ASCIIDOC_SIGNED("asciidoc;application/pkcs7-signature", "asciidoc"),

    JSON(MediaTypeVS.JSON, "json"),
    JSON_SIGNED(MediaTypeVS.JSON_SIGNED,"p7s"),
    JSON_ENCRYPTED("application/json;application/pkcs7-mime","p7m"),//.p7c
    JSON_SIGNED_AND_ENCRYPTED("application/json;application/pkcs7-signature;application/pkcs7-mime", "p7m"),

    MESSAGEVS(MediaTypeVS.MESSAGEVS, "vs"),
    MULTIPART_SIGNED("multipart/signed", null),
    MULTIPART_ENCRYPTED(MediaTypeVS.MULTIPART_ENCRYPTED, null),
    PDF("application/pdf", "pdf"),
    PDF_SIGNED_AND_ENCRYPTED("application/pdf;application/pkcs7-signature;application/pkcs7-mime", "pdf"),
    PDF_SIGNED("application/pdf;application/pkcs7-signature", "pdf"),
    PDF_ENCRYPTED("application/pdf;application/pkcs7-mime", "pdf"),
    TEXT("text/plain", "txt"),
    HTML("text/html", "html"),
    TEXT_STREAM("text/plain", "txt"),
    TIMESTAMP_QUERY("application/timestamp-query", null),
    TIMESTAMP_RESPONSE("application/timestamp-response", null),

    ZIP(MediaTypeVS.ZIP, "zip"),
    IMAGE("application/image", null),

    OCSP_REQUEST("application/ocsp-request", null),
    OCSP_RESPONSE("application/ocsp-response", null),

    CMS_SIGNED("signed-data", null),
    //smime.p7m -> Email message encrypted
    //smime.p7s -> Email message that includes a digital signature
    SIGNED("application/pkcs7-signature","p7s"),
    ENCRYPTED(MediaTypeVS.ENCRYPTED, "p7m"),//.p7c
    SIGNED_AND_ENCRYPTED("application/pkcs7-signature;application/pkcs7-mime", "p7m"),

    PKCS7_CERT("application/pkcs7-certificates","p7b"),//.spc
    PKCS7_CERT_REQ_RESP("application/pkcs7-certreqresp","p7r"),
    PKCS7_CRL("application/pkcs7-crl","crl"),
    PKCS12("application/pkcs12","p12"),//.pfx
    PKIX_CRL("application/pkix-crl", "crl"),
    PKIX_CERT("application/pkix-cert", "cer"),
    PKCS10("application/pkcs10", "p10"),//.csr
    PEM(MediaTypeVS.PEM, "pem"),

    VOTE(MediaTypeVS.VOTE, "vote"),

    CURRENCY(MediaTypeVS.CURRENCY, "servs"),

    X509_CA("application/x509-ca-cert", "crt"),
    X509_USER("application/x509-user-cert", "crt");

    private String name;
    private String extension;
    private TypeVS typeVS;

    private ContentTypeVS(String name, String extension) {
        this.name = name;
        this.extension = "." + extension;
    }

    private ContentTypeVS(String name, String extension, TypeVS typeVS) {
        this.name = name;
        this.extension = "." + extension;
    }


    public TypeVS getTypeVS() {
        return typeVS;
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
        return name.contains("application/pkcs7-mime");
    }

    public boolean isSignedAndEncrypted() {
        return (name.contains("application/pkcs7-signature") && name.contains("application/pkcs7-mime"));
    }

    public static ContentTypeVS getByName(String contentTypeStr) {
        if(contentTypeStr == null) return null;

        if(contentTypeStr.contains(JSON_SIGNED.getName())) return JSON_SIGNED;
        if(contentTypeStr.contains("json")) return JSON;

        if(contentTypeStr.contains(MULTIPART_SIGNED.getName())) return MULTIPART_SIGNED;
        if(contentTypeStr.contains(TEXT.getName())) return TEXT;
        if(contentTypeStr.contains(HTML.getName())) return HTML;

        if(contentTypeStr.contains(ASCIIDOC.getName())) return ASCIIDOC;
        if(contentTypeStr.contains(ASCIIDOC_SIGNED.getName())) return ASCIIDOC_SIGNED;

        if(contentTypeStr.contains(MESSAGEVS.getName())) return MESSAGEVS;

        if(contentTypeStr.contains(ENCRYPTED.getName())) return ENCRYPTED;
        if(contentTypeStr.contains(SIGNED.getName())) return SIGNED;
        if(contentTypeStr.contains(SIGNED_AND_ENCRYPTED.getName())) return SIGNED_AND_ENCRYPTED;

        if(contentTypeStr.contains(PDF.getName())) return PDF;
        if(contentTypeStr.contains(PDF_ENCRYPTED.getName())) return PDF_ENCRYPTED;
        if(contentTypeStr.contains(PDF_SIGNED.getName())) return PDF_SIGNED;
        if(contentTypeStr.contains(PDF_SIGNED_AND_ENCRYPTED.getName())) return PDF_SIGNED_AND_ENCRYPTED;


        if(contentTypeStr.contains(JSON_ENCRYPTED.getName())) return JSON_ENCRYPTED;

        if(contentTypeStr.contains(JSON_SIGNED_AND_ENCRYPTED.getName())) return JSON_SIGNED_AND_ENCRYPTED;

        if(contentTypeStr.contains(MULTIPART_ENCRYPTED.getName())) return MULTIPART_ENCRYPTED;


        if(contentTypeStr.contains(TIMESTAMP_QUERY.getName())) return TIMESTAMP_QUERY;
        if(contentTypeStr.contains(TIMESTAMP_RESPONSE.getName())) return TIMESTAMP_RESPONSE;

        if(contentTypeStr.contains(VOTE.getName())) return VOTE;

        if(contentTypeStr.contains(CURRENCY.getName())) return CURRENCY;

        return null;
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
