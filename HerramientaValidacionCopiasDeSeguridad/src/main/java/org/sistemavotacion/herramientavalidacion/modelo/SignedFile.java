package org.sistemavotacion.herramientavalidacion.modelo;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.herramientavalidacion.ContextoHerramienta;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignedFile {
        
    private static Logger logger = LoggerFactory.getLogger(SignedFile.class);
    
    private byte[] signedFileBytes = null;
    private String name = null;
    private TimeStampToken timeStampToken = null;
    private SMIMEMessageWrapper smimeMessageWraper = null;
    private boolean signatureVerified = false;
    private PdfPKCS7 pdfPKCS7 = null;

    
    public SignedFile(byte[] signedFileBytes, String name) throws Exception {
        this.name = name;
        this.signedFileBytes = signedFileBytes;
        if(name.toLowerCase().endsWith(".pdf")) {
            PdfReader reader = new PdfReader(signedFileBytes);
            AcroFields acroFields = reader.getAcroFields();
            ArrayList<String> names = acroFields.getSignatureNames();
            for (String sigName : names) {
                //logger.debug(" - SignedFile - covers whole document: " + 
                //        acroFields.signatureCoversWholeDocument(sigName));
                pdfPKCS7 = acroFields.verifySignature(sigName, "BC");
                timeStampToken = pdfPKCS7.getTimeStampToken();
                
                if(!pdfPKCS7.verify()) {
                    //logger.error(" - checkSignature - VERIFICATION FAILED!!!");
                    signatureVerified = false;
                } else {
                    //logger.error("checkSignature - OK");
                    signatureVerified = true;
                } 
                //X509Certificate signingCert = pk.getSigningCertificate();
                //logger.debug(" checkSignature - signingCert: " + signingCert.getSubjectDN());
                //Calendar signDate = pk.getSignDate();
                //logger.debug(" - checkSignature - signingCert: " + signDate.getTime().toString());
                //X509Certificate[] pkc = (X509Certificate[])pk.getSignCertificateChain();
                timeStampToken = pdfPKCS7.getTimeStampToken();
                //logger.debug(" - checkSignature - timeStampToken: " + 
                //        timeStampToken.getTimeStampInfo().getGenTime().toString());
                //KeyStore keyStore = firmaService.getTrustedCertsKeyStore()
                //Object[] fails = PdfPKCS7.verifyCertificates(pkc, keyStore, null, signDate);
                //if(fails != null) {...}
            }
            
        } else if(name.toLowerCase().endsWith(".p7m")){
            smimeMessageWraper = new SMIMEMessageWrapper(null,
                new ByteArrayInputStream(signedFileBytes), null);
            signatureVerified = smimeMessageWraper.isValidSignature();
            if(signatureVerified) timeStampToken = smimeMessageWraper.
                    getFirmante().getTimeStampToken();
        } else {
            logger.error(" #### UNKNOWN FILE TYPE -> " + name + 
                    " trying with SMIMEMessageWrapper");
            smimeMessageWraper = new SMIMEMessageWrapper(null,
                new ByteArrayInputStream(signedFileBytes), null);
            signatureVerified = smimeMessageWraper.isValidSignature();
        }
    }
    
    public byte[] getDigestToken(TimeStampToken timeStampToken) {
        if(timeStampToken == null) return null;
        CMSSignedData tokenCMSSignedData = timeStampToken.toCMSSignedData();		
        Collection signers = tokenCMSSignedData.getSignerInfos().getSigners();
        SignerInformation tsaSignerInfo = (SignerInformation)signers.iterator().next();

        AttributeTable signedAttrTable = tsaSignerInfo.getSignedAttributes();
        ASN1EncodableVector v = signedAttrTable.getAll(CMSAttributes.messageDigest);
        Attribute t = (Attribute)v.get(0);
        ASN1Set attrValues = t.getAttrValues();
        DERObject validMessageDigest = attrValues.getObjectAt(0).getDERObject();

        ASN1OctetString signedMessageDigest = (ASN1OctetString)validMessageDigest;			
        byte[] digestToken = signedMessageDigest.getOctets();  
        String digestTokenStr = new String(Base64.encode(digestToken));
        logger.debug("============= digestTokenStr: " + digestTokenStr);
        return digestToken;
    }
    
    public boolean isValidSignature() {
        return signatureVerified;
    }
    
    /**
     * @return the signedFileBytes
     */
    public byte[] getSignedFileBytes() {
        return signedFileBytes;
    }

    /**
     * @param signedFileBytes the signedFileBytes to set
     */
    public void setSignedFileBytes(byte[] signedFileBytes) {
        this.signedFileBytes = signedFileBytes;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public SMIMEMessageWrapper getSMIMEMessageWraper() {
        return smimeMessageWraper;
    }
    
    public JSONObject getContent() throws Exception {
        JSONObject contentJSON = (JSONObject) JSONSerializer.toJSON(
                    smimeMessageWraper.getSignedContent());
        return contentJSON;
    }    
    
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isPDF() {
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".pdf") && 
                signatureVerified) return true;
        else return false;
    }
    
    public boolean isSMIME() {
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".p7m") && 
                signatureVerified) return true;
        else return false;
    }
    
    public String getSignerNif() {
        return smimeMessageWraper.getFirmante().getNif();
    }
    
    public byte[] getFileBytes() {
        return signedFileBytes;
    }
    
    public String getNifFromRepresented() {
        if(name != null && name.contains("_RepDoc_")) {
            String nifParts = name.split("_RepDoc_")[1];
            return nifParts.split(".p7m")[0];
        } else return null;
    }
    
    public File getFile() throws Exception {
        File file = File.createTempFile("signedFile", ".votingSystem");
        file.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(signedFileBytes), file);
        return file;
    }

    public Long getSelectedOptionId() {
        if(smimeMessageWraper == null) return null;
        String signedContent = smimeMessageWraper.getSignedContent();
        Object content = JSONSerializer.toJSON(signedContent);
        if(content instanceof JSONObject) {
            JSONObject contentJSON = (JSONObject)content;
            if(contentJSON.containsKey("opcionSeleccionadaId")) {
                return contentJSON.getLong("opcionSeleccionadaId");
            }
        } else {
            logger.error(" File '" + name + "' content is instance of " + 
                    content.getClass());
        }
        return null;
    }
    
    public Long getNumSerieSignerCert() {
        if(smimeMessageWraper == null) return null;
        Usuario usuario = smimeMessageWraper.getFirmante();
        return usuario.getCert().getSerialNumber().longValue();
    }
        
    //{"representativeNif":"00000002W","operation":"REPRESENTATIVE_SELECTION","UUID":"dcfacb17-a323-4853-b446-8e28d8f2d0a4"}   
    public Respuesta validateAsRepresentationDocument(
            Set<X509Certificate> systemTrustedCerts, 
            Date dateFinish, String representativeNif, 
            X509Certificate timeStampServerCert) throws Exception {
        if(!signatureVerified) {            
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                "signatureErrorMsg", name));    
        }
        if(representativeNif == null) return new Respuesta(
                Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("badRequestMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString("missingRepresentativeNifErrorMsg"));
        Usuario usuario = smimeMessageWraper.getFirmante();
        try {
            PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                    usuario.getCertificate(), systemTrustedCerts, false);
            //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                    "certificateErrorMsg", usuario.getNif(), name));
        } 
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("timestampValidationErrorMsg", name));
        }
        Date tokenDate = timeStampToken.getTimeStampInfo().getGenTime();
        if(!tokenDate.before(dateFinish)) {
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                    "dateErrorMsg", DateUtils.getStringFromDate(dateFinish),
                    DateUtils.getStringFromDate(tokenDate)), name);
        }
        JSONObject contentJSON = getContent(); 
        if(contentJSON.containsKey("representativeNif")) {
            String jsonNIF = contentJSON.getString("representativeNif");
            if(!representativeNif.trim().equals(jsonNIF)) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("nifErrorMsg", representativeNif, jsonNIF, name));
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("jsonErrorMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString("missingRepresentativeNifErrorMsg", name));

        if(contentJSON.containsKey("operation")) {
            Operacion.Tipo operationType = Operacion.Tipo.valueOf(
                    contentJSON.getString("operation"));
            if(Operacion.Tipo.REPRESENTATIVE_SELECTION != operationType) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("operationErrorMsg", 
                        Operacion.Tipo.REPRESENTATIVE_SELECTION.toString(),
                        operationType.toString()), name);
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("jsonErrorMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString("missingOperationErrorMsg", name));
        
        return new Respuesta(Respuesta.SC_OK);
    }
    
    //{"operation":"ENVIO_VOTO_SMIME","opcionSeleccionadaId":2,"UUID":"cfbeec4a-f87c-4e4f-b442-4b127259fbd5",
    //"opcionSeleccionadaContenido":"depende","eventoURL":"http://192.168.1.20:8080/SistemaVotacionControlAcceso/eventoVotacion/1"}
    public Respuesta<Long> validateAsVote(Set<X509Certificate> systemTrustedCerts,
        Set<X509Certificate> eventTrustedCerts, 
        Long optionSelectedId, String eventURL, Date dateInit, 
        Date dateFinish, X509Certificate timeStampServerCert) throws Exception {
        Long signedFileOptionSelectedId = null;
        if(!signatureVerified) {            
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                "signatureErrorMsg", name));
            
        }
        Set<Usuario> firmantes = smimeMessageWraper.getFirmantes();
        for(Usuario firmante:firmantes) {
            try {
                if(firmante.getTimeStampToken() != null) {//user signature
                    PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                        firmante.getCertificate(), eventTrustedCerts, false);
                    //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
                } else {//server signature
                    PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                        firmante.getCertificate(), systemTrustedCerts, false);
                } 
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                        "certificateErrorMsg", firmante.getNif(), name));
            } 
        }
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("timestampValidationErrorMsg", name));
        }
        Date tokenDate = smimeMessageWraper.getFirmante().getTimeStampToken().
                getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                    "tokenDateErrorMsg", name, DateUtils.getStringFromDate(tokenDate),
                    DateUtils.getStringFromDate(dateInit),
                    DateUtils.getStringFromDate(dateFinish)));
        }
        
        JSONObject contentJSON = getContent(); 
        if(contentJSON.containsKey("operation")) {
            Operacion.Tipo operationType = Operacion.Tipo.valueOf(
                    contentJSON.getString("operation"));
            if(Operacion.Tipo.ENVIO_VOTO_SMIME != operationType) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("operationErrorMsg", 
                        Operacion.Tipo.ENVIO_VOTO_SMIME.toString(),
                        operationType.toString()), name);
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("jsonErrorMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString("missingOperationErrorMsg", name));
        if(contentJSON.containsKey("opcionSeleccionadaId")) {
            signedFileOptionSelectedId = contentJSON.getLong("opcionSeleccionadaId");
            if(optionSelectedId != null && signedFileOptionSelectedId != optionSelectedId) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("optionSelectedErrorMsg", name, 
                        optionSelectedId, signedFileOptionSelectedId));
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("jsonErrorMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString(
                "missingOptionSelectedErrorMsg", name));

        if(contentJSON.containsKey("eventoURL")) {
            String  eventoURL = contentJSON.getString("eventoURL");
            if(!eventoURL.equals(eventURL)) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("eventoURLErrorMsg", name, eventURL, eventoURL));
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                getString("jsonErrorMsg") + " - " + ContextoHerramienta.INSTANCE.
                getString("missingEventoURLErrorMsg", name));
        return new Respuesta(Respuesta.SC_OK, signedFileOptionSelectedId);
    }
    
    public Respuesta validateAsAccessRequest(
        Set<X509Certificate> systemTrustedCerts, 
        String eventURL, Date dateInit, Date dateFinish, 
        X509Certificate timeStampServerCert) throws Exception {
        if(!signatureVerified) {            
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                "signatureErrorMsg", name));
            
        }
        Usuario signer = smimeMessageWraper.getFirmante();        try {
        PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                    signer.getCertificate(), systemTrustedCerts, false);
            //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                    "certificateErrorMsg", signer.getNif(), name));
        } 
        
        JSONObject contentJSON = getContent(); 
        if(contentJSON.containsKey("operation")) {
            Operacion.Tipo operationType = Operacion.Tipo.valueOf(
                    contentJSON.getString("operation"));
            if(Operacion.Tipo.SOLICITUD_ACCESO != operationType) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("operationErrorMsg", 
                        Operacion.Tipo.SOLICITUD_ACCESO.toString(),
                        operationType.toString()), name);
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("jsonErrorMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString("missingOperationErrorMsg", name));
        
        if(contentJSON.containsKey("eventURL")) {
            String  eventoURL = contentJSON.getString("eventURL");
            if(!eventoURL.equals(eventURL)) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("eventoURLErrorMsg", name, eventURL, eventoURL));
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("jsonErrorMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString(
                "missingEventoURLErrorMsg", name));
        
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("timestampValidationErrorMsg", name));
        }
        Date tokenDate = timeStampToken.getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                "tokenDateErrorMsg", name, DateUtils.getStringFromDate(tokenDate),
                DateUtils.getStringFromDate(dateInit),
                DateUtils.getStringFromDate(dateFinish)));
        }
        return new Respuesta(Respuesta.SC_OK);
    }
    
    public Respuesta validateAsManifestSignature(
        KeyStore trustedKeyStore, Date dateInit, Date dateFinish, 
        X509Certificate timeStampServerCert) throws Exception {
        if(!signatureVerified) {            
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                "signatureErrorMsg", name));    
        }
        
        
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("timestampValidationErrorMsg", name));
        }
        
        getDigestToken(timeStampToken);
        Calendar signDate = pdfPKCS7.getSignDate();
        X509Certificate[] signCertificateChain = (X509Certificate[])pdfPKCS7.
                getSignCertificateChain();
        
        Object[] fails = PdfPKCS7.verifyCertificates(signCertificateChain, 
                trustedKeyStore, null, signDate);
        if(fails != null) {
            logger.error("signature fail");
            for(Object object:fails) {
                logger.error("signature fail - " + object);
            }
            for(X509Certificate cert:signCertificateChain) {
                    String notAfter = DateUtils.getStringFromDate(cert.getNotAfter());
                    String notBefore = DateUtils.getStringFromDate(cert.getNotBefore());
                    logger.debug("pdf checkSignature - fails - Cert:" + cert.getSubjectDN()
                            + " - NotBefore: " + notBefore + " - NotAfter:" + notAfter);
            }
            return new Respuesta (Respuesta.SC_ERROR_PETICION);	
        }
        return new Respuesta (Respuesta.SC_OK);
    }

    public Respuesta validateAsClaim(Set<X509Certificate> systemTrustedCerts, 
            String eventURL, Date dateInit, Date dateFinish, 
            X509Certificate timeStampServerCert) throws Exception {
        if(!signatureVerified) {            
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                "signatureErrorMsg", name));
            
        }
        Usuario signer = smimeMessageWraper.getFirmante();        
        try {
            PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                    signer.getCertificate(), systemTrustedCerts, false);
            //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                    "certificateErrorMsg", signer.getNif(), name));
        } 
        
        JSONObject contentJSON = getContent(); 
        if(contentJSON.containsKey("operation")) {
            Operacion.Tipo operationType = Operacion.Tipo.valueOf(
                    contentJSON.getString("operation"));
            if(Operacion.Tipo.FIRMA_RECLAMACION_SMIME != operationType) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("operationErrorMsg", 
                        Operacion.Tipo.FIRMA_RECLAMACION_SMIME.toString(),
                        operationType.toString()), name);
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("jsonErrorMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString("missingOperationErrorMsg", name));
        
        if(contentJSON.containsKey("URL")) {
            String  eventoURL = contentJSON.getString("URL");
            if(!eventoURL.equals(eventURL)) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("eventoURLErrorMsg", name, eventURL, eventoURL));
            }
        } else return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString("jsonErrorMsg") + 
                " - " + ContextoHerramienta.INSTANCE.getString(
                "missingEventoURLErrorMsg", name));
        
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.
                        getString("timestampValidationErrorMsg", name));
        }
        Date tokenDate = timeStampToken.getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
                return new Respuesta(Respuesta.SC_ERROR, ContextoHerramienta.INSTANCE.getString(
                "tokenDateErrorMsg", name, DateUtils.getStringFromDate(tokenDate),
                DateUtils.getStringFromDate(dateInit),
                DateUtils.getStringFromDate(dateFinish)));
        }
        return new Respuesta(Respuesta.SC_OK);
    }
    
}