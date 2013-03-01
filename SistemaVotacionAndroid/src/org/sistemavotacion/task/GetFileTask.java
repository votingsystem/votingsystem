package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class GetFileTask extends AsyncTask<String, Void, Integer> {

	public static final String TAG = "GetFileTask";
	
	private Integer id = null;
	private TaskListener listener = null;
    private Exception exception = null;
    private byte[] fileData = null;
    int statusCode = Respuesta.SC_ERROR_PETICION; 
    private String message = null;
    private String documentUrl = null;
    
    public GetFileTask(Integer id, TaskListener listener) {
    	this.id = id;
    	this.listener = listener;
    }
	
	@Override
	protected Integer doInBackground(String... urls) {
		documentUrl = urls[0];
        Log.d(TAG + ".doInBackground(...)", " - documentUrl: " + documentUrl);
        try {
            HttpResponse response = HttpHelper.getFile(urls[0]);
            statusCode = response.getStatusLine().getStatusCode();
            if(Respuesta.SC_OK == statusCode) 
            	fileData = EntityUtils.toByteArray(response.getEntity());
            else message = EntityUtils.toString(response.getEntity());
        } catch (Exception ex) {
        	ex.printStackTrace();
        	exception = ex;
        }
        return statusCode;
	}
	
    @Override
    protected void onPostExecute(Integer result) {
    	Log.d(TAG + ".onPostExecute(...)", " - statusCode: " + result);
    	listener.showTaskResult(this);
    }

	public Integer getId() {
		return id;
	}
	
    public byte[] getFileData() {
    	return fileData;
    }
    
    public int getStatusCode() {
    	return statusCode;
    }
    
	public String getMessage() {
		if(exception != null) return exception.getMessage();
		return message;
	}
	
	public String getDocumentUrl() {
		return documentUrl;
	}
	
}