package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class GetFileTask extends AsyncTask<String, Void, String> {

	public static final String TAG = "GetFileTask";
	
    private FileListener listener = null;
    private Exception exception = null;
    private byte[] fileData = null;
    int statusCode = Respuesta.SC_ERROR_PETICION;  
    
    public GetFileTask(FileListener listener) {
    	this.listener = listener;
    }
	
	@Override
	protected String doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground(...)", " - url: " + urls[0]);
        String result = null;
        try {
            HttpResponse response = HttpHelper.getFile(urls[0]);
            statusCode = response.getStatusLine().getStatusCode();
            if(Respuesta.SC_OK == statusCode) 
            	fileData = EntityUtils.toByteArray(response.getEntity());
            else result = EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
        	Log.e(TAG + ".doInBackground", ex.getMessage(), ex);
        	exception = ex;
        }
        return result;
	}
	
    @Override
    protected void onPostExecute(String result) {
    	Log.d(TAG + ".onPostExecute(...)", " - statusCode: " + statusCode);
        if(Respuesta.SC_OK == statusCode) listener.porcessFileData(fileData);
    	else if(exception != null) 
    		listener.setException(exception.getMessage());
    	else listener.setException(result);
    }

}