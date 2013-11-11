package org.votingsystem.model;

public class ContentTypeVS {

    public static final String JSON    = "application/json";
    
    public static final String PDF    = "application/pdf";
    public static final String SIGNED = "application/x-pkcs7-signature";
    public static final String X509 = "application/x-x509-ca-cert";
    public static final String ENCRYPTED = "application/x-pkcs7-mime";
    public static final String SIGNED_AND_ENCRYPTED = 
            SIGNED + "," + ENCRYPTED;
    public static final String PDF_SIGNED_AND_ENCRYPTED = 
            PDF + "," +  SIGNED + ";" + ENCRYPTED;    
    public static final String PDF_SIGNED = 
    		PDF + "," + SIGNED;     
    public static final String PDF_ENCRYPTED = 
    		PDF + "," + ENCRYPTED; 
    
}
