package org.sistemavotacion.util;

import static org.sistemavotacion.android.Aplicacion.NOMBRE_ARCHIVO_BYTE_ARRAY;
import static org.sistemavotacion.android.Aplicacion.NOMBRE_ARCHIVO_CSR;
import static org.sistemavotacion.android.Aplicacion.NOMBRE_ARCHIVO_FIRMADO;

import java.io.File;
import java.io.IOException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.seguridad.CertUtil;

import android.util.Log;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class HttpHelper {
    
	public static final String TAG = "HttpHelper";
    
    private static final DefaultHttpClient httpclient;
    private static final ThreadSafeClientConnManager cm;
    
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
       cm = new ThreadSafeClientConnManager(params, schemeRegistry);
       httpclient = new DefaultHttpClient(cm, params);
    }
    
    
    public void shutdown () {
        try { httpclient.getConnectionManager().shutdown(); } 
        catch (Exception ex) {
            Log.e(TAG + ".shutdown" , ex.getMessage());
        }
    }
    
    public static HttpResponse obtenerInformacion (String serverURL) 
            throws IOException, ParseException {
        HttpGet httpget = new HttpGet(serverURL);
        Log.d(TAG + ".obtenerInformacion(...)" ," - serverURL: " + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        Log.d(TAG + ".obtenerInformacion" ,"----------------------------------------");
        /*Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
        System.out.println(headers[i]);
        }*/
        Log.d(TAG + ".obtenerInformacion" ,"Connections in pool: " + cm.getConnectionsInPool());
        Log.d(TAG + ".obtenerInformacion" ,response.getStatusLine().toString());
        Log.d(TAG + ".obtenerInformacion" ,"----------------------------------------");
        return response;    
    }
    
    public X509Certificate obtenerCertificadoDeServidor (String serverURL) throws Exception {
        HttpGet httpget = new HttpGet(serverURL);
        X509Certificate certificado = null;
        Log.d(TAG + ".obtenerCertificadoDeServidor(...)", " - serverURL: " + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        Log.d(TAG + ".obtenerCertificadoDeServidor" ,"----------------------------------------");
        Log.d(TAG + ".obtenerCertificadoDeServidor" , response.getStatusLine().toString());
        Log.d(TAG + ".obtenerCertificadoDeServidor" ,"----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (200 == response.getStatusLine().getStatusCode()) {
            certificado = CertUtil.fromPEMToX509Cert(EntityUtils.toByteArray(entity));
        }
        return certificado;
    }
    
    public Collection<X509Certificate> obtenerCadenaCertificacionDeServidor (String serverURL) throws Exception {
        HttpGet httpget = new HttpGet(serverURL);
        Collection<X509Certificate> certificados = null;
        Log.d(TAG + ".obtenerCadenaCertificacionDeServidor(...)" ," - serverURL: " + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        Log.d(TAG + ".obtenerCadenaCertificacionDeServidor" ,"----------------------------------------");
        Log.d(TAG + ".obtenerCadenaCertificacionDeServidor" ,response.getStatusLine().toString());
        Log.d(TAG + ".obtenerCadenaCertificacionDeServidor" ,"----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (200 == response.getStatusLine().getStatusCode()) {
            certificados = CertUtil.fromPEMToX509CertCollection(EntityUtils.toByteArray(entity));
        }
        return certificados;
    }
    
    public PKIXParameters obtenerPKIXParametersDeServidor (String serverURL) throws Exception {
        Log.d(TAG + ".obtenerPKIXParametersDeServidor(...)" ," - serverURL: " + serverURL);
        String urlCadenaCertificacion = ServerPaths.getURLCadenaCertificacion(serverURL);
        Collection<X509Certificate> certificados = obtenerCadenaCertificacionDeServidor(urlCadenaCertificacion);
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        for (X509Certificate certificado:certificados) {
            TrustAnchor anchorCertificado = new TrustAnchor(certificado, null);
            anchors.add(anchorCertificado);
        }
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false); // tell system do not check CRL's
        return params;       
    }    
    
    public static HttpResponse sendFile (File file, String serverURL) throws IOException {
        HttpPost httpPost = new HttpPost(serverURL);
        Log.d(TAG + ".sendFile(...)" , " - serverURL: " + httpPost.getURI() 
        		+ " - file: " + file.getAbsolutePath());
        FileBody fileBody = new FileBody(file);
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart(NOMBRE_ARCHIVO_FIRMADO, fileBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        Log.d(TAG + ".sendFile" , "----------------------------------------");
        Log.d(TAG + ".sendFile" , response.getStatusLine().toString());
        Log.d(TAG + ".sendFile" , "----------------------------------------");
        return response;
    }
    
    public static HttpResponse sendSignedData (String cadenaFirmada, String serverURL) throws IOException {
        HttpPost httpPost = new HttpPost(serverURL);
        Log.d(TAG + ".sendSignedData(...)" ," - serverURL: " + httpPost.getURI());
        StringBody stringBody = new StringBody(cadenaFirmada);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        reqEntity.addPart(NOMBRE_ARCHIVO_FIRMADO, stringBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        Log.d(TAG + ".sendSignedData" ,"----------------------------------------");
        Log.d(TAG + ".sendSignedData" ,response.getStatusLine().toString());
        Log.d(TAG + ".sendSignedData" ,"----------------------------------------");
        return response;
    }
    
    public static HttpResponse sendData (String data, String serverURL) throws IOException {
        HttpPost httpPost = new HttpPost(serverURL);
        Log.d(TAG + ".sendData(...)" ," - serverURL: " + httpPost.getURI());
        StringEntity stringEntity = new StringEntity(data);
        httpPost.setEntity(stringEntity);
        HttpResponse response = httpclient.execute(httpPost);
        Log.d(TAG + ".sendData" ,"----------------------------------------");
        Log.d(TAG + ".sendData" ,response.getStatusLine().toString());
        Log.d(TAG + ".sendData" ,"----------------------------------------");
        return response;
    }

    public static HttpResponse sendByteArray(
    		byte[] data, String serverURL) throws IOException {
        HttpPost httpPost = new HttpPost(serverURL);
        Log.d(TAG + ".sendByteArray(...)" , " - serverURL: " + httpPost.getURI());
        ByteArrayBody  byteArrayBody = new ByteArrayBody(data, NOMBRE_ARCHIVO_BYTE_ARRAY);
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart(NOMBRE_ARCHIVO_BYTE_ARRAY, byteArrayBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        Log.d(TAG + ".sendByteArray(...)" ,"----------------------------------------");
        Log.d(TAG + ".sendByteArray(...)" , response.getStatusLine().toString());
        Log.d(TAG + ".sendByteArray(...)" , "----------------------------------------");
        return response;  
    }
    
     public static HttpResponse enviarSolicitudAcceso(byte[] solicitudCSR, 
            File solicitudAccesoSMIME, String serverURL) throws IOException {
        HttpPost httpPost = new HttpPost(serverURL);
        Log.d(TAG + ".enviarSolicitudAcceso(...)" , " - serverURL: " + httpPost.getURI());
        FileBody fileBody = new FileBody(solicitudAccesoSMIME);
        ByteArrayBody  csrBody = new ByteArrayBody(solicitudCSR, 
                NOMBRE_ARCHIVO_CSR);
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart(NOMBRE_ARCHIVO_FIRMADO, fileBody);
        reqEntity.addPart(NOMBRE_ARCHIVO_CSR, csrBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        Log.d(TAG + ".enviarSolicitudAcceso" ,"----------------------------------------");
        Log.d(TAG + ".enviarSolicitudAcceso" ,response.getStatusLine().toString());
        Log.d(TAG + ".enviarSolicitudAcceso" ,"----------------------------------------");
        return response;  
    }
     
     public static HttpResponse enviarSolicitudAcceso(File solicitudCSR, 
             File solicitudAccesoSMIME, String serverURL) throws IOException {
         HttpPost httpPost = new HttpPost(serverURL);
         Log.d(TAG + ".enviarSolicitudAcceso(...)" , " - serverURL: " + httpPost.getURI());
         FileBody fileBody = new FileBody(solicitudAccesoSMIME);
         FileBody  csrBody = new FileBody(solicitudCSR);
         MultipartEntity reqEntity = new MultipartEntity();
         reqEntity.addPart(NOMBRE_ARCHIVO_FIRMADO, fileBody);
         reqEntity.addPart(NOMBRE_ARCHIVO_CSR, csrBody);
         httpPost.setEntity(reqEntity);
         HttpResponse response = httpclient.execute(httpPost);
         Log.d(TAG + ".enviarSolicitudAcceso" ,"----------------------------------------");
         Log.d(TAG + ".enviarSolicitudAcceso" ,response.getStatusLine().toString());
         Log.d(TAG + ".enviarSolicitudAcceso" ,"----------------------------------------");
         return response;  
     }
     
     public static HttpResponse getFile (String serverURL) throws Exception {
         HttpGet httpget = new HttpGet(serverURL);
         Log.d(TAG + ".getFile(...)", " - serverURL: " + httpget.getURI());
         HttpResponse response = httpclient.execute(httpget);
         Log.d(TAG + ".obtenerCertificadoDeServidor" ,"----------------------------------------");
         Log.d(TAG + ".obtenerCertificadoDeServidor" , response.getStatusLine().toString());
         Log.d(TAG + ".obtenerCertificadoDeServidor" ,"----------------------------------------");
         return response;
     }

}