package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class SendByteArrayTask extends AsyncTask<String, Void, byte[]> {

	public static final String TAG = "SendByteArrayTask";
	
	private Integer id = null;
	private TaskListener listener = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    byte[] dataSend = null;
    byte[] result = null;
    private Exception exception = null;
    private String message = null;
    
    public SendByteArrayTask(Integer id, TaskListener listener, 
    		byte[] dataSend) {
    	this.id = id;
		this.listener = listener;
		this.dataSend = dataSend;
    }
	
	@Override
	protected byte[] doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", " - urls[0]: " + urls[0]);
        try {
            HttpResponse response = HttpHelper.sendByteArray(dataSend, urls[0]);
            statusCode = response.getStatusLine().getStatusCode();
            result = EntityUtils.toByteArray(response.getEntity());
        } catch (Exception ex) {
        	Log.e(TAG + ".doInBackground", ex.getMessage(), ex);
        	exception = ex;
        }
        return result;
	}
	
    @Override
    protected void onPostExecute(byte[] data) {
    	Log.d(TAG + ".onPostExecute", " - data");
    	listener.showTaskResult(this);
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
	
	public byte[] getResult() {
		return result;
	}

}