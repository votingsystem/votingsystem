package org.sistemavotacion.worker;

import java.util.Hashtable;
import java.util.List;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class TimeStampWorker extends SwingWorker<String, String> 
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
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;

    public TimeStampWorker(Integer id, String urlArchivo, 
            VotingSystemWorkerListener workerListener, byte[] digest, 
            String timeStampRequestAlg) {
        this.id = id;
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        timeStampRequest = reqgen.generate(timeStampRequestAlg, digest);
    }
    
    public TimeStampWorker(Integer id, String urlArchivo, 
            VotingSystemWorkerListener workerListener, TimeStampRequest timeStampRequest) {
        this.id = id;
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
        this.timeStampRequest = timeStampRequest;
    }
    
    public int getId() {
        return this.id;
    }
    
    @Override protected String doInBackground() throws Exception {
        try {
            HttpResponse response = Contexto.getInstancia().getHttpHelper().
                    enviarByteArray(timeStampRequest.getEncoded(), urlArchivo);
            statusCode = response.getStatusLine().getStatusCode();
            if (Respuesta.SC_OK == statusCode) {
                bytesToken = EntityUtils.toByteArray(response.getEntity());
                timeStampToken = new TimeStampToken(
                        new CMSSignedData(bytesToken));
                DERObject derObject = new ASN1InputStream(
                        timeStampToken.getEncoded()).readObject();
                DERSet derset = new DERSet(derObject);
                timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.
                		id_aa_signatureTimeStampToken, derset);
                Hashtable hashTable = new Hashtable();
                hashTable.put(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, timeStampAsAttribute);
                timeStampAsAttributeTable = new AttributeTable(hashTable);
            } else message = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
        } catch(HttpHostConnectException ex){
            logger.error(ex.getMessage(), ex);
            statusCode = Respuesta.SC_ERROR_EJECUCION;
            exception = new Exception(Contexto.getString("hostConnectionErrorMsg")); 
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            statusCode = Respuesta.SC_ERROR_EJECUCION;
            exception = ex;
        } finally {return message;}
    }

    @Override//on the EDT
    protected void done() {
        try { 
            get();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            exception = ex;
        } finally {workerListener.showResult(this);}
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
    
    @Override public String getMessage() {
        if(exception != null) return exception.getMessage();
        else return message;
    }

    @Override public int getStatusCode() {
        return statusCode;
    }

}
