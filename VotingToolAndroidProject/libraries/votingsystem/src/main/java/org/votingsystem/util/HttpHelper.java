package org.votingsystem.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.votingsystem.model.ContentTypeVS;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.votingsystem.model.ContextVS.SIGNED_FILE_NAME;
import static org.votingsystem.util.LogUtils.LOGD;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class HttpHelper {
    
	public static final String TAG = HttpHelper.class.getSimpleName();
    
    private static DefaultHttpClient httpclient;
    private static ThreadSafeClientConnManager cm;
    private static HttpHelper INSTANCE;

    private HttpHelper(X509Certificate trustedSSLServerCert) {
        try {
            HttpParams params = new BasicHttpParams();
            params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            params.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
            params.setParameter(CoreProtocolPNames.USER_AGENT, "Apache-HttpClient/Android");
            params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
            params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry(trustedSSLServerCert.getSubjectDN().toString(),
                    trustedSSLServerCert);
            SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
            //schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
            schemeRegistry.register(new Scheme("https", socketFactory, 443));
            LOGD(TAG, "Added Scheme https with port 443 to Apache httpclient");
            cm = new ThreadSafeClientConnManager(params, schemeRegistry);
            httpclient = new DefaultHttpClient(cm, params);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public static void init(X509Certificate trustedSSLServerCert) {
        INSTANCE = new HttpHelper(trustedSSLServerCert);
    }
    
    public static void shutdown () {
        Log.d(TAG, "shutdown");
        try {
            httpclient.getConnectionManager().shutdown();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    
    public static ResponseVS getData (String serverURL, ContentTypeVS contentType) {
        Log.d(TAG + ".getData" , "serverURL: " + serverURL + " - contentType: " + contentType);
        HttpResponse response = null;
        ResponseVS responseVS = null;
        ContentTypeVS responseContentType = null;
        try {
            HttpGet httpget = new HttpGet(serverURL);
            if(contentType != null) httpget.setHeader("Content-Type", contentType.getName());
            response = httpclient.execute(httpget);
            Log.d(TAG + ".getData" ,"----------------------------------------");
            /*Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) { System.out.println(headers[i]); }*/
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
            Log.d(TAG + ".getData" ,"Connections in pool: " + cm.getConnectionsInPool());
            Log.d(TAG + ".getData" ,response.getStatusLine().toString() + " - " +
                    response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
            Log.d(TAG + ".getData" ,"----------------------------------------");
            if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                        responseBytes, responseContentType);
            } else {
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()),responseContentType);
            }
        } catch(ConnectTimeoutException ex) {
            responseVS = new ResponseVS(ResponseVS.SC_CONNECTION_TIMEOUT, ex.getMessage());
        } catch(Exception ex) {
            Log.d(TAG + ".getData" , "exception: " + ex.getMessage());
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return responseVS;
    }

    public static ResponseVS sendData(byte[] data, ContentTypeVS contentType,
              String serverURL, String... headerNames) {
        HttpPost httpPost = new HttpPost(serverURL);
        Log.d(TAG + ".sendData" , "serverURL: " + serverURL + " - contentType: " + contentType);
        HttpResponse response = null;
        ResponseVS responseVS = null;
        ContentTypeVS responseContentType = null;
        try {
            ByteArrayEntity reqEntity = new ByteArrayEntity(data);
            if(contentType != null) reqEntity.setContentType(contentType.getName());
            httpPost.setEntity(reqEntity);
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
            Log.d(TAG + ".sendData" ,"----------------------------------------");
            Log.d(TAG + ".sendData" , response.getStatusLine().toString() + " - " +
                    response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
            Log.d(TAG + ".sendData" , "----------------------------------------");
            responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                    EntityUtils.toByteArray(response.getEntity()), responseContentType);
            if(headerNames != null && headerNames.length > 0) {
                List<String> headerValues = new ArrayList<String>();
                for(String headerName: headerNames) {
                    org.apache.http.Header headerValue = response.getFirstHeader(headerName);
                    if(headerValue != null) headerValues.add(headerValue.getValue());
                }
                responseVS.setData(headerValues);
            }
        } catch(ConnectTimeoutException ex) {
            responseVS = new ResponseVS(ResponseVS.SC_CONNECTION_TIMEOUT, ex.getMessage());
        }  catch(Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return responseVS;
    }

    public static ResponseVS sendFile (File file, String serverURL) {
        ResponseVS responseVS = null;
        ContentTypeVS responseContentType = null;
        try {
            HttpPost httpPost = new HttpPost(serverURL);
            Log.d(TAG + ".sendFile" , "serverURL: " + httpPost.getURI()
                    + " - file: " + file.getAbsolutePath());
            FileBody fileBody = new FileBody(file);
            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart(SIGNED_FILE_NAME, fileBody);
            httpPost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httpPost);
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
            Log.d(TAG + ".sendFile" , "----------------------------------------");
            Log.d(TAG + ".sendFile" , response.getStatusLine().toString() + " - " +
                    response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
            Log.d(TAG + ".sendFile" , "----------------------------------------");
            if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),responseBytes,
                        responseContentType);
            } else {
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()), responseContentType);
            }
        } catch(ConnectTimeoutException ex) {
            responseVS = new ResponseVS(ResponseVS.SC_CONNECTION_TIMEOUT, ex.getMessage());
        }  catch(Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return responseVS;
    }
     
     public static ResponseVS sendObjectMap(
             Map<String, Object> fileMap, String serverURL) throws Exception {
    	 Log.d(TAG + ".sendObjectMap" , "serverURL: " + serverURL);
         ResponseVS responseVS = null;
         ContentTypeVS responseContentType = null;
         if(fileMap == null || fileMap.isEmpty()) throw new Exception("Empty Map");
         HttpPost httpPost = new HttpPost(serverURL);
         HttpResponse response = null;
         try {
             Set<String> fileNames = fileMap.keySet();
             MultipartEntity reqEntity = new MultipartEntity();
             for(String objectName: fileNames){
                 Object objectToSend = fileMap.get(objectName);
                 if(objectToSend instanceof File) {
                     File file = (File)objectToSend;
                     Log.d(TAG + ".sendObjectMap" , ".sendObjectMap - fileName: " + objectName + 
                             " - filePath: " + file.getAbsolutePath());  
                     FileBody  fileBody = new FileBody(file);
                     reqEntity.addPart(objectName, fileBody);
                 } else if (objectToSend instanceof byte[]) {
                     byte[] byteArray = (byte[])objectToSend;
                     reqEntity.addPart(
                             objectName, new ByteArrayBody(byteArray, objectName));
                 }
             }
             httpPost.setEntity(reqEntity);
             response = httpclient.execute(httpPost);
             Header header = response.getFirstHeader("Content-Type");
             if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
             Log.d(TAG + ".sendObjectMap" ,"----------------------------------------");
             Log.d(TAG + ".sendObjectMap" ,response.getStatusLine().toString() + " - " +
                     response.getFirstHeader("Content-Type") + " - contentTypeVS: " + responseContentType);
             Log.d(TAG + ".sendObjectMap" ,"----------------------------------------");
             byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
             responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes,
                     responseContentType);
         } catch(ConnectTimeoutException ex) {
             responseVS = new ResponseVS(ResponseVS.SC_CONNECTION_TIMEOUT, ex.getMessage());
         }  catch(Exception ex) {
             ex.printStackTrace();
        	 String statusLine = null;
             if(response != null) {
                 statusLine = response.getStatusLine().toString();
             }
             responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
             httpPost.abort();
         }
         return responseVS;
     }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) return true;
        return false;
    }
}