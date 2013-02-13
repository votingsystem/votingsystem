package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class GetDataTask extends AsyncTask<String, Void, String> {

	public static final String TAG = "GetDataTask";
	
	private Integer id;
    private TaskListener listener = null;
    private Exception exception = null;
    private String message = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    
    public GetDataTask(Integer id, TaskListener listener) {
    	this.id = id;
    	this.listener = listener;
    }
	
	@Override
	protected String doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", " - url: " + urls[0]);
        try {
            HttpResponse response = HttpHelper.obtenerInformacion(urls[0]);
            statusCode = response.getStatusLine().getStatusCode();
            message = EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
        	ex.printStackTrace();
        	exception = ex;
        }
        return message;
	}
	
    @Override
    protected void onPostExecute(String data) {
    	Log.d(TAG + ".onPostExecute", " - statusCode: " + statusCode);
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
	
}