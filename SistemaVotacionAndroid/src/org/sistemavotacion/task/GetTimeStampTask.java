package org.sistemavotacion.task;

import java.util.Hashtable;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle2.asn1.ASN1InputStream;
import org.bouncycastle2.asn1.DERObject;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.TimeStampWrapper;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class GetTimeStampTask extends AsyncTask<String, Void, Integer> 
	implements TimeStampWrapper {

	public static final String TAG = "GetTimeStampTask";
	
	TaskListener listener = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    byte[] digest = null;
    private Exception exception = null;
    private TimeStampToken timeStampToken = null;
    private Integer id;
    private Attribute timeStampAsAttribute = null;
    private AttributeTable timeStampAsAttributeTable = null;
    private TimeStampRequest timeStampRequest = null;
    private String message = null;

    
    public GetTimeStampTask(Integer id, byte[] digest, 
    		String timeStampRequestAlg, TaskListener listener) {
        this.id = id;
		this.listener = listener;
		this.digest = digest;
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        this.timeStampRequest = reqgen.generate(timeStampRequestAlg, digest);
    }
    
    public GetTimeStampTask(Integer id, TimeStampRequest timeStampRequest, 
    		TaskListener listener) {
        this.id = id;
		this.listener = listener;
		this.timeStampRequest = timeStampRequest;
    }
	
	@Override
	protected Integer doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", "TimeStamp server: " + urls[0]);
        try {
            HttpResponse response = HttpHelper.sendByteArray(
            		timeStampRequest.getEncoded(), urls[0]);
            statusCode = response.getStatusLine().getStatusCode();
            if (Respuesta.SC_OK == statusCode) {
                byte[] bytesToken = EntityUtils.toByteArray(response.getEntity());
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
            } else message = response.getStatusLine().toString();
        } catch (Exception ex) {
        	Log.e(TAG + ".doInBackground(...)", ex.getMessage(), ex);
        	exception = ex;
        }
        return statusCode;
	}
	
    @Override
    protected void onPostExecute(Integer statusCode) {
    	Log.d(TAG + ".onPostExecute", " - statusCode: " + statusCode);
    	listener.showTaskResult(this);
    }

	@Override
	public TimeStampToken getTimeStampToken() {
		return timeStampToken;
	}

	@Override
	public Attribute getTimeStampTokenAsAttribute() {
		return timeStampAsAttribute;
	}

	@Override
	public AttributeTable getTimeStampTokenAsAttributeTable() {
		return timeStampAsAttributeTable;
	}

	public Integer getId() {
		return id;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getMessage() {
		if(exception != null) return exception.getMessage();
		return message;
	}

}