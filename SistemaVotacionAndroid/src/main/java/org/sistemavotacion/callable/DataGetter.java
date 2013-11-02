package org.sistemavotacion.callable;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.HttpHelper;

import java.util.concurrent.Callable;

public class DataGetter  implements Callable<Respuesta> {

    public static final String TAG = "DataGetter";

    private String contentType = null;
    private String serviceURL = null;

    public DataGetter(String contentType, String serviceURL) {
        this.contentType = contentType;
        this.serviceURL = serviceURL;
    }

    @Override public Respuesta call() {
        Log.d(TAG + ".call", " - serviceURL: " + serviceURL);
        Respuesta respuesta = null;
        try {
            HttpResponse response = HttpHelper.getData(serviceURL, contentType);
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
            return new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        }
        return respuesta;
    }

}
