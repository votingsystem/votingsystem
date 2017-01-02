package org.votingsystem.http;


public class MediaType {

    public static final String JSON_SIGNED = "application/json;application/pkcs7-signature";
    public static final String ENCRYPTED = "application/pkcs7-encrypted";
    public static final String JSON_ENCRYPTED = "application/json;application/pkcs7-encrypted";
    public static final String JSON_SIGNED_ENCRYPTED = "application/json;application/pkcs7-signature;application/pkcs7-encrypted";
    public static final String JSON = "application/json";
    public static final String XML = "application/xml";
    public static final String XML_SIGNED = "application/xml;application/pkcs7-signature";
    public static final String VOTE = "application/vote;application/pkcs7-signature;";
    public static final String PEM = "application/pem-file";
    public static final String ZIP = "application/zip";
    public static final String MESSAGEVS = "application/pkcs7-messagevs";
    public static final String BACKUP = "application/backup";
    public static final String MULTIPART_ENCRYPTED = "multipart/encrypted";
    public static final String TIMESTAMP_QUERY = "application/timestamp-query";
    public static final String TIMESTAMP_RESPONSE = "application/timestamp-response";
    public static final String OCSP_REQUEST = "application/ocsp-request";

}
