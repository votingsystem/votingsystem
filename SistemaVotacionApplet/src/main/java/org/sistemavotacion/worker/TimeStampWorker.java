package org.sistemavotacion.worker;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import javax.swing.SwingWorker;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TimeStampWorker extends SwingWorker<Respuesta, String> 
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(TimeStampWorker.class);

    private Integer id;
    private String urlArchivo;
    private VotingSystemWorkerListener workerListener;
    private TimeStampToken timeStampToken = null;
    private Attribute timeStampAsAttribute = null;
    private AttributeTable timeStampAsAttributeTable = null;
    private TimeStampRequest timeStampRequest = null;
    private byte[] bytesToken = null;
    private Respuesta respuesta = null;
        
    private X509Certificate timeStampCert = null;
    private SignerInformationVerifier timeStampSignerInfoVerifier;
    
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    /*public TimeStampWorker(Integer id, String urlArchivo, 
            VotingSystemWorkerListener workerListener, byte[] digest, 
            String timeStampRequestAlg) {
        this.id = id;
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        timeStampRequest = reqgen.generate(timeStampRequestAlg, digest);
    }*/
    
    public TimeStampWorker(Integer id, String urlArchivo, 
            VotingSystemWorkerListener workerListener, 
            TimeStampRequest timeStampRequest, X509Certificate timeStampServerCert) 
            throws OperatorCreationException, Exception {
        this.id = id;
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
        
        if(timeStampRequest == null) {
            throw new Exception("timeStampRequest null");
        }
        this.timeStampRequest = timeStampRequest;
        this.timeStampCert = timeStampServerCert;
        timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                setProvider(BC).build(timeStampCert); 
    }
    
 
    @Override protected Respuesta doInBackground() throws Exception {
        respuesta = Contexto.getInstancia().getHttpHelper().
            sendByteArray(timeStampRequest.getEncoded(), "timestamp-query", urlArchivo);
        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            bytesToken = respuesta.getBytesArchivo();
            
            timeStampToken = new TimeStampToken(
                    new CMSSignedData(bytesToken));
            
            timeStampToken.validate(timeStampSignerInfoVerifier);

            DERObject derObject = new ASN1InputStream(
                    timeStampToken.getEncoded()).readObject();
            DERSet derset = new DERSet(derObject);
            timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, derset);
            Hashtable hashTable = new Hashtable();
            hashTable.put(PKCSObjectIdentifiers.
                        id_aa_signatureTimeStampToken, timeStampAsAttribute);
            timeStampAsAttributeTable = new AttributeTable(hashTable);
        }
        return respuesta;
    }

    @Override protected void done() {//on the EDT
        try {
            respuesta = get();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        } 
        workerListener.showResult(this);
    }
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        workerListener.process(messages);
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
    
    public Attribute getTimeStampTokenAsAttribute() {
        return timeStampAsAttribute;
    }
    
    public AttributeTable getTimeStampTokenAsAttributeTable() {
        return timeStampAsAttributeTable;
    }

    public byte[] getBytesToken() {
        return bytesToken;
    }
    
    public TimeStampRequest getTimeStampRequest() {
        return timeStampRequest;
    }
    
    public byte[] getDigestToken() {
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
        //String digestTokenStr = new String(Base64.encode(digestToken));
        //logger.debug(" digestTokenStr: " + digestTokenStr);
        return digestToken;
    }
    
   @Override public String getMessage() {
        if(respuesta == null) return null;
        else return respuesta.getMensaje();
    }

    @Override public int getId() {
        return this.id;
    }

    @Override  public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }
    
    @Override public Respuesta getRespuesta() {
        return respuesta;
    }

}
