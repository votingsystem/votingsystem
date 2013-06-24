package org.sistemavotacion.task;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class SendDataTask extends AsyncTask<String, Void, Respuesta> {

	public static final String TAG = "SendDataTask";
	

    private byte[] data = null;
    private String contentType = null;
    
    public SendDataTask(byte[] data, String contentType) {
		this.data = data;
		this.contentType = contentType;
    }
	
	@Override
	protected Respuesta doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground(...)", " - url:" + urls[0]);
        Respuesta respuesta = null;
        try {
            HttpResponse response = HttpHelper.sendByteArray(data, contentType, urls[0]); 
            respuesta = new Respuesta(
            		response.getStatusLine().getStatusCode(),
            		EntityUtils.toString(response.getEntity()));
        } catch (Exception ex) {
        	ex.printStackTrace();
        	respuesta = new Respuesta(Respuesta.SC_ERROR,
            		ex.getMessage());
        }
        return respuesta;
	}
	
    @Override  protected void onPostExecute(Respuesta respuesta) {
    	Log.d(TAG + ".onPostExecute", " - statusCode: " + 
    				respuesta.getCodigoEstado());
    }

	
}