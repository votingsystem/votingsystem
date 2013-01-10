package org.sistemavotacion.task;

import java.io.File;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;
import android.os.AsyncTask;
import android.util.Log;

public class GetVotingCertTask extends AsyncTask<String, Void, String> {

	public static final String TAG = "GetVotingCertTask";
	
	DataListener<String> listener = null;
    File solicitudAcceso;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    private Exception exception = null;
    byte[] csr;
    
    public GetVotingCertTask(DataListener<String> listener, File solicitudAcceso, 
    		byte[] csr) {
		this.solicitudAcceso = solicitudAcceso;
		this.csr = csr;
		this.listener = listener;
    }
	
	@Override
	protected String doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", "");
        String result = null;  
        try {
        	String url = urls[0];
            HttpResponse response = HttpHelper.enviarSolicitudAcceso(csr, solicitudAcceso, url);
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