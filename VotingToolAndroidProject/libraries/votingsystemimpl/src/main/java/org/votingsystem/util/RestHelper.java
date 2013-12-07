package org.votingsystem.util;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.votingsystem.model.ContentTypeVS;

public class RestHelper {
	
	private static final String CLASSTAG = RestHelper.class.getSimpleName();

   // Establish client once, as static field with static setup block.
   // (This is a best practice in HttpClient docs - but will leave reference until *process* stopped on Android.)
   private static final DefaultHttpClient httpClient;
   static {
      HttpParams params = new BasicHttpParams();
      params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
      params.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
      params.setParameter(CoreProtocolPNames.USER_AGENT, "Apache-HttpClient/Android");
	  params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
	  params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
	  SchemeRegistry schemeRegistry = new SchemeRegistry();
	  schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	  schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
      ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
      httpClient = new DefaultHttpClient(cm, params);
   }
	
    public static HttpResponse doGet(final String url) {
    	Log.d(CLASSTAG + ".doGet", " - url: " + url);
        HttpGet httpget = new HttpGet(url); 
        HttpResponse response = null;
        try {
        	// execute is a blocking call, it's best to call this code in a thread separate from the ui's
            response = httpClient.execute(httpget);
        } catch (Exception ex) {
        	Log.e(CLASSTAG + ".doGet", ex.getMessage(), ex);
        	return null;
        }
        return response;
    }
    
    
    public static HttpResponse doPost (final String url,final JSONObject json) {
    	Log.d(CLASSTAG + ".doPost a url: " + url, "- JSON: " + json.toString());
		HttpPost httpPost = new HttpPost(url);
    	httpPost.addHeader("Accept", ContentTypeVS.JSON.getName());
    	httpPost.addHeader("Content-Type", ContentTypeVS.JSON.getName());
    	HttpResponse response = null;
    	try {
    	    StringEntity entity = new StringEntity(json.toString(), "UTF-8");
    	    entity.setContentType(ContentTypeVS.JSON.getName());
    	    httpPost.setEntity(entity);
    	    // execute is a blocking call, it's best to call this code in a thread separate from the ui's
    	    response = httpClient.execute(httpPost);
    	}
    	catch (Exception ex) {	
    		Log.e(CLASSTAG + ".doPost", ex.getMessage(), ex);
    		return null;
    	}
    	return response;
    }

    public static HttpResponse doPut (final String url,final JSONObject json) {
    	Log.d(CLASSTAG + ".doPut a url: " + url, "- JSON: " + json.toString());
		HttpPut httpPut = new HttpPut(url);
		httpPut.addHeader("Accept", ContentTypeVS.JSON.getName());
		httpPut.addHeader("Content-Type", ContentTypeVS.JSON.getName());
		HttpResponse response = null;
    	try {
    	    StringEntity entity = new StringEntity(json.toString(), "UTF-8");
    	    entity.setContentType(ContentTypeVS.JSON.getName());
    	    httpPut.setEntity(entity);
    	    // execute is a blocking call, it's best to call this code in a thread separate from the ui's
    	    response = httpClient.execute(httpPut);
    	}
    	catch (Exception ex) {
    		Log.e(CLASSTAG + ".doPut", ex.getMessage(), ex);
    		return null;
    	}
    	return response;
    }
    
    public static HttpResponse doDelete (final String url) {
    	Log.d(CLASSTAG + ".doDelete" , " - url: " + url);
		HttpDelete httpDelete = new HttpDelete(url);
		httpDelete.addHeader("Accept", ContentTypeVS.JSON.getName());
		HttpResponse response = null;
    	try {
    		// execute is a blocking call, it's best to call this code in a thread separate from the ui's
    	    response = httpClient.execute(httpDelete);
    	}
    	catch (Exception ex) {	
    		Log.e(CLASSTAG + ".doDelete", ex.getMessage(), ex);
    		return null;
    	}
    	return response;
    }    

}