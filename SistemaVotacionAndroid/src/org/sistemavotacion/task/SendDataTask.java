package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class SendDataTask extends AsyncTask<String, Void, String> {

	public static final String TAG = "SendDataTask";
	
	private Integer id;
    private TaskListener listener = null;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    private String data = null;
    private String message = null;
    private Exception exception = null;
    
    public SendDataTask(Integer id, TaskListener listener, String data) {
    	this.id = id;
		this.listener = listener;
		this.data = data;
    }
	
	@Override
	protected String doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground(...)", " - url:" + urls[0]);
        try {
            HttpResponse response = HttpHelper.sendData(data, urls[0]); 
            statusCode = response.getStatusLine().getStatusCode();
            message = EntityUtils.toString(response.getEntity());;
        } catch (Exception ex) {
        	ex.printStackTrace();
        	exception = ex;
        }
        return message;
	}
	
    @Override
    protected void onPostExecute(String data) {
    	Log.d(TAG + ".onPostExecute", " - data: " + data);
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