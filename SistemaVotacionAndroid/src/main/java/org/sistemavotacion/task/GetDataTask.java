package org.sistemavotacion.task;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;

public class GetDataTask extends AsyncTask<String, Void, Respuesta> {

	public static final String TAG = "GetDataTask";
	

    private String contentType = null;
    
    public GetDataTask(String contentType) {
    	this.contentType = contentType;
    }
	
	@Override
	protected Respuesta doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", " - url: " + urls[0]);
        Respuesta respuesta = null;
        try {
            HttpResponse response = HttpHelper.getData(urls[0], contentType);
            if(Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                respuesta = new Respuesta(response.getStatusLine().getStatusCode(),
                        new String(responseBytes), responseBytes);
            } else {
                respuesta = new Respuesta(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception ex) {
        	ex.printStackTrace();
        	respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        }
        return respuesta;
	}

	
}