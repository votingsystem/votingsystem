package org.sistemavotacion.task;

import java.io.File;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class SendFileTask extends AsyncTask<String, Void, String> {

	public static final String TAG = "SendFileTask";
	
    private DataListener<String> listener = null;
    File file = null;
    private Exception exception = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    
    public SendFileTask(DataListener<String> listener, File file) {
		this.listener = listener;
		this.file = file;
    }
	
	@Override
	protected String doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", "");
        String result = null;  
        try {
            HttpResponse response = HttpHelper.sendFile(file, urls[0]);
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