package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class SendDataTask extends AsyncTask<String, Void, String> {

	public static final String TAG = "SendDataTask";
	
	DataListener<String> listener = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    String data = null;
    private Exception exception = null;
    
    public SendDataTask(DataListener<String> listener, String data) {
		this.listener = listener;
		this.data = data;
    }
	
	@Override
	protected String doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground(...)", " - url:" + urls[0]);
        String result = null;  
        try {
            HttpResponse response = HttpHelper.sendData(data, urls[0]); 
            statusCode = response.getStatusLine().getStatusCode();
            result = EntityUtils.toString(response.getEntity());;
        } catch (Exception ex) {
        	Log.e(TAG + ".doInBackground", ex.getMessage(), ex);
        	exception = ex;
        }
        return result;
	}
	
    @Override
    protected void onPostExecute(String data) {
    	Log.d(TAG + ".onPostExecute", " - data: " + data);
    	if(data != null)listener.updateData(statusCode, data);
    	else if(exception != null) 
    		listener.setException(exception.getMessage());
    }

}