package org.votingsystem.callable;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.android.util.HttpHelper;

import java.util.concurrent.Callable;

public class DataGetter  implements Callable<ResponseVS> {

    public static final String TAG = "DataGetter";

    private String contentType = null;
    private String serviceURL = null;

    public DataGetter(String contentType, String serviceURL) {
        this.contentType = contentType;
        this.serviceURL = serviceURL;
    }

    @Override public ResponseVS call() {
        Log.d(TAG + ".call", " - serviceURL: " + serviceURL);
        ResponseVS responseVS = null;
        try {
            HttpResponse response = HttpHelper.getData(serviceURL, contentType);
            if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                        new String(responseBytes), responseBytes);
            } else {
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return responseVS;
    }

}
