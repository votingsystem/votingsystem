package org.controlacceso.clientegwt.client.util;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHelper {
	
    private static Logger logger = Logger.getLogger("RequestHelper");

    public static void doPost(String url, String postData, 
    		RequestCallback requestCallback) {
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);
        builder.setHeader("Content-Type", "application/json");
		logger.log(Level.INFO, "doPost - url:" + url + " - postData:" + postData);
        try {
            Request response = builder.sendRequest(postData, requestCallback);
        } catch (RequestException e) {
        	logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void doGet(String url, 
    		RequestCallback requestCallback) {
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
        builder.setHeader("Content-Type", "application/json");
        logger.log(Level.INFO, "doGet - url: " + url);
        try {
            Request response = builder.sendRequest("", requestCallback);
        } catch (RequestException e) {
        	logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void doPut(
            String url, String putData, RequestCallback requestCallback) {
    	logger.log(Level.INFO, "doPut - url: " + url + " - data: " + putData);
    	RequestBuilder builder = new RequestBuilder(RequestBuilder.PUT, url);
        builder.setHeader("Content-Type", "application/json");
        try {
            Request response = builder.sendRequest(putData, requestCallback);
        } catch (RequestException e) {
        	logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void doDelete(String url, RequestCallback requestCallback) {
    	logger.log(Level.INFO, "doDelete - url: " + url);
        RequestBuilder builder = new RequestBuilder(RequestBuilder.DELETE, url);
        builder.setHeader("Content-Type", "application/json");
        try {
            Request response = builder.sendRequest("", requestCallback);
        } catch (RequestException e) {
        	logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    
    public static void submitForm(String url, String requestData, 
    		RequestCallback requestCallback) {
    	logger.log(Level.INFO, "submitForm requestData: " + requestData + " -url: " + url);
    	RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
		requestBuilder.setHeader("Content-Type", "application/x-www-form-urlencoded");
		requestBuilder.setRequestData(requestData);
	    requestBuilder.setCallback(requestCallback);
	    try {
	    	requestBuilder.send();
	    } catch (RequestException e) {
        	logger.log(Level.SEVERE, e.getMessage(), e);
    	}
    }

}
