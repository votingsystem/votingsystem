package org.sistemavotacion.task;

import java.io.File;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class SendFileTask extends AsyncTask<String, Void, Integer> {

	public static final String TAG = "SendFileTask";
	
	private Integer id;
    private TaskListener listener = null;
    private File file = null;
    private Exception exception = null;
    private String message = null;
    private String contentType = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    
    public SendFileTask(Integer id, TaskListener listener, 
    		File file, String contentType) {
    	this.id = id;
		this.listener = listener;
		this.file = file;
		this.contentType = contentType;
    }
	
	@Override
	protected Integer doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", " - doInBackground - url: " + urls[0]);
        try {
            HttpResponse response = HttpHelper.sendFile(file, contentType, urls[0]);
            statusCode = response.getStatusLine().getStatusCode();
            message = EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
        	Log.e(TAG + ".doInBackground", ex.getMessage(), ex);
        	exception = ex;
        }
        return statusCode;
	}
	
    @Override
    protected void onPostExecute(Integer data) {
    	Log.d(TAG + ".onPostExecute", " - statuscode: " + data);
    	if(Respuesta.SC_OK != data)
    		Log.d(TAG + ".onPostExecute", " - message: " + message);
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