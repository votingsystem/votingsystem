package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class SendByteArrayTask extends AsyncTask<String, Void, byte[]> {

	public static final String TAG = "SendByteArrayTask";
	
	DataListener<byte[]> listener = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    byte[] dataSend = null;
    private Exception exception = null;
    
    public SendByteArrayTask(DataListener<byte[]> listener, 
    		byte[] dataSend) {
		this.listener = listener;
		this.dataSend = dataSend;
    }
	
	@Override
	protected byte[] doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", "");
        byte[] result = null;  
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
    	if(data != null) {
    		if(Respuesta.SC_OK == statusCode) 
    			listener.updateData(statusCode, data);
    		else listener.setException(new String(data));
    	} else if(exception != null) 
    		listener.setException(exception.getMessage());
    }

}