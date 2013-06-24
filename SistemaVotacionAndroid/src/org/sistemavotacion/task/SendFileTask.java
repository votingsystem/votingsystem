package org.sistemavotacion.task;

import java.io.File;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class SendFileTask extends AsyncTask<String, Void, Respuesta> {

	public static final String TAG = "SendFileTask";
	
    private File file = null;

    public SendFileTask(File file) {
		this.file = file;
    }
	
	@Override
	protected Respuesta doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", " - doInBackground - url: " + urls[0]);
        Respuesta respuesta = null;
        try {
            HttpResponse response = HttpHelper.sendFile(file, urls[0]);
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
    	Log.d(TAG + ".onPostExecute", " - statuscode: " + respuesta.getCodigoEstado());
    }

}