package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class GetDataTask extends AsyncTask<String, Void, String> {

	public static final String TAG = "GetDataTask";
	
    private DataListener listener = null;
    private Exception exception = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    
    public GetDataTask(DataListener listener) {
    	this.listener = listener;
    }
	
	@Override
	protected String doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", " - url: " + urls[0]);
        String result = null;  
        try {
            HttpResponse response = HttpHelper.obtenerInformacion(urls[0]);
            statusCode = response.getStatusLine().getStatusCode();
            result = EntityUtils.toString(response.getEntity());
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