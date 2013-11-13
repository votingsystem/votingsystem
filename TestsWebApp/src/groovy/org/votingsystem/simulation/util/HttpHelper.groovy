package org.votingsystem.simulation.util;

import java.io.IOException;
import java.text.ParseException;
import java.io.File;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.model.ResponseVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class HttpHelper {
    
	private static Logger log = Logger.getLogger(
		HttpHelper.class);
    
    private HttpClient httpclient;
    private PoolingClientConnectionManager cm;
    private IdleConnectionEvictor connEvictor;

    public HttpHelper () {
        /*SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(
        new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));*/
        cm = new PoolingClientConnectionManager();
        cm.setMaxTotal(10);
        cm.setDefaultMaxPerRoute(10); 
        connEvictor = new IdleConnectionEvictor(cm);
        connEvictor.start();
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
        httpclient = new DefaultHttpClient(cm, httpParams);
    }
    
    public synchronized void initMultiThreadedMode() {
        log.debug("initMultiThreadedMode");
        if(cm != null) cm.shutdown();
        cm = new PoolingClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(200); 
        connEvictor = new IdleConnectionEvictor(cm);
        connEvictor.start();
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 40000);
        httpclient = new DefaultHttpClient(cm, httpParams);
    }
    
    public void shutdown () {
        try { 
            httpclient.getConnectionManager().shutdown(); 
            if(connEvictor != null) {
                connEvictor.shutdown();
                connEvictor.join();
            }
        } 
        catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
    
    public ResponseVS getData (String serverURL, String contentType) 
            throws IOException, ParseException {
        log.debug("getData - serverURL: " + serverURL + " - contentType: " 
                + contentType);  
        
        ResponseVS respuesta = null;
        HttpResponse response = null;
		HttpGet httpget = null;
        try {
			httpget = new HttpGet(serverURL);
            if(contentType != null) httpget.setHeader("Content-Type", contentType);
            response = httpclient.execute(httpget);
            log.debug("----------------------------------------");
            /*Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
            System.out.println(headers[i]);
            }*/
            log.debug(response.getStatusLine().toString());
            log.debug("----------------------------------------");
            if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                respuesta = new ResponseVS(response.getStatusLine().getStatusCode(),
                        new String(responseBytes), responseBytes);
            } else {
                respuesta = new ResponseVS(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()));
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            respuesta = new ResponseVS(ResponseVS.SC_ERROR, Contexto.INSTANCE.
                    getString("hostConnectionErrorMsg", serverURL));
            if(httpget != null) httpget.abort();
        } finally {
            if(response != null) EntityUtils.consume(response.getEntity());
            return respuesta;
        }
    }
    
    public X509Certificate obtenerCertificadoDeServidor (String serverURL) throws Exception {
        log.debug("obtenerCertificadoDeServidor - serverURL: " + serverURL);           
        HttpGet httpget = new HttpGet(serverURL);
        X509Certificate certificado = null;
        HttpResponse response = httpclient.execute(httpget);
        log.debug("----------------------------------------");
        log.debug(response.getStatusLine().toString());
        log.debug("----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
            certificado = CertUtil.fromPEMToX509Cert(EntityUtils.toByteArray(entity));
        }
        EntityUtils.consume(response.getEntity());
        return certificado;
    }
    
    public Collection<X509Certificate> obtenerCadenaCertificacionDeServidor (
            String serverURL) throws Exception {
        log.debug("obtenerCadenaCertificacionDeServidor - serverURL: " + serverURL);   
        HttpGet httpget = new HttpGet(serverURL);
        Collection<X509Certificate> certificados = null;
        log.debug("obtenerCertificadoDeServidor - lanzando: " + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        log.debug("----------------------------------------");
        log.debug(response.getStatusLine().toString());
        log.debug("----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
            certificados = CertUtil.fromPEMToX509CertCollection(
                    EntityUtils.toByteArray(entity));
        }
        EntityUtils.consume(response.getEntity());
        return certificados;
    }
   
    public PKIXParameters obtenerPKIXParametersDeServidor (String certChainURL) throws Exception {
        log.debug("obtenerPKIXParametersDeServidor - certChainURL: " + certChainURL);
        Set<TrustAnchor> anchors = obtenerAnchorsDeServidor(certChainURL);
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false); // tell system do not check CRL's
        return params;       
    }   
    
    public Set<TrustAnchor> obtenerAnchorsDeServidor (String certChainURL) throws Exception {
        log.debug("obtenerAnchorsDeServidor - certChainURL: " + certChainURL);
        Collection<X509Certificate> certificados = obtenerCadenaCertificacionDeServidor(certChainURL);
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        for (X509Certificate certificado:certificados) {
            TrustAnchor anchorCertificado = new TrustAnchor(certificado, null);
            anchors.add(anchorCertificado);
        }
        return anchors;
    }

    
    public ResponseVS sendFile (File file, String contentType, String serverURL, 
            String... headerNames) {
        log.debug("sendFile - contentType: " + contentType + 
                " - serverURL: " + serverURL); 
        ResponseVS respuesta = null;
        HttpPost httpPost = null;
        try {
			httpPost = new HttpPost(serverURL);
            FileEntity entity = new FileEntity(file, ContentType.create(contentType));
            httpPost.setEntity(entity);
            HttpResponse response = httpclient.execute(httpPost);
            log.debug("----------------------------------------");
            log.debug(response.getStatusLine().toString());
            log.debug("----------------------------------------"); 
            byte[] responseBytes =  EntityUtils.toByteArray(response.getEntity());
            respuesta = new ResponseVS(response.getStatusLine().getStatusCode(),
                    new String(responseBytes), responseBytes);
            if(headerNames != null) {
                List<String> headerValues = new ArrayList<String>();
                for(String headerName: headerNames) {
                    org.apache.http.Header headerValue = 
                            response.getFirstHeader(headerName);
                    headerValues.add(headerValue.getValue());
                }
                respuesta.setData(headerValues);
            }
            EntityUtils.consume(response.getEntity());
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            respuesta = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }
        return respuesta;
    }
        
        
    public ResponseVS sendString (String stringToSend, 
            String paramName, String serverURL) {
        log.debug("sendString - serverURL: " + serverURL);
        ResponseVS respuesta = null;
        HttpPost httpPost = null;
        try {
			httpPost = new HttpPost(serverURL);
            StringBody stringBody = new StringBody(stringToSend);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
            reqEntity.addPart(paramName, stringBody);
            httpPost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httpPost);
            log.debug("----------------------------------------");
            log.debug(response.getStatusLine().toString());
            log.debug("----------------------------------------");
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            respuesta = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }
        return respuesta;
    }
    
    public ResponseVS sendByteArray(byte[] byteArray, String contentType,
            String serverURL, String... headerNames) throws IOException {
        log.debug("sendByteArray - contentType: " + contentType + 
                " - serverURL: " + serverURL);
        ResponseVS respuesta = null;
        HttpPost httpPost = null;
        HttpResponse response = null;
        try {
			httpPost = new HttpPost(serverURL);
            ByteArrayEntity entity = null;
            if(contentType != null) {
                entity = new ByteArrayEntity(byteArray,  ContentType.create(contentType));
            } else entity = new ByteArrayEntity(byteArray); 
            httpPost.setEntity(entity);
            response = httpclient.execute(httpPost);
            log.debug("----------------------------------------");
            log.debug(response.getStatusLine().toString());
            log.debug("----------------------------------------");
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            respuesta = new ResponseVS(response.getStatusLine().getStatusCode(),
                    new String(responseBytes), responseBytes);
            if(headerNames != null) {
                List<String> headerValues = new ArrayList<String>();
                for(String headerName: headerNames) {
                    org.apache.http.Header headerValue = 
                            response.getFirstHeader(headerName);
					if(headerValue != null) {
						headerValues.add(headerValue.getValue());
					} else log.error(" - headerName '" + headerName + "' without response!!!");
                    
                }
                respuesta.setData(headerValues);
            }
            EntityUtils.consume(response.getEntity());
        } catch(HttpHostConnectException ex){
            log.error(ex.getMessage(), ex);
            respuesta = new ResponseVS(ResponseVS.SC_ERROR, Contexto.INSTANCE.
                    getString("hostConnectionErrorMsg", serverURL));
            if(httpPost != null)  httpPost.abort();
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            respuesta = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }  finally {
            if(response != null) EntityUtils.consume(response.getEntity());
            return respuesta;
        }
    }
    
    
    public ResponseVS sendObjectMap(
            Map<String, Object> fileMap, String serverURL) throws Exception {
        log.debug("sendObjectMap - serverURL: " + serverURL); 
        ResponseVS respuesta = null;
        if(fileMap == null || fileMap.isEmpty()) throw new Exception(
                Contexto.INSTANCE.getString("requestWithoutFileMapErrorMsg"));
        HttpPost httpPost = null;
        HttpResponse response = null;
        try {
			httpPost = new HttpPost(serverURL);
            Set<String> fileNames = fileMap.keySet();
            MultipartEntity reqEntity = new MultipartEntity();
            for(String objectName: fileNames){
                Object objectToSend = fileMap.get(objectName);
                if(objectToSend instanceof File) {
                    File file = (File)objectToSend;
                    log.debug("sendFileMap - fileName: " + objectName + 
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
            log.debug("----------------------------------------");
            log.debug(response.getStatusLine().toString());
            log.debug("----------------------------------------");
            byte[] responseBytes =  EntityUtils.toByteArray(response.getEntity());
            respuesta = new ResponseVS(response.getStatusLine().getStatusCode(),
                    new String(responseBytes), responseBytes);
            //EntityUtils.consume(response.getEntity());
        } catch(Exception ex) {
            String statusLine = null;
            if(response != null) {
                statusLine = response.getStatusLine().toString();
            }
            log.error(ex.getMessage() + " - StatusLine: " + statusLine, ex);
            respuesta = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }
        return respuesta;  
    }


    public static class IdleConnectionEvictor extends Thread {

        private final ClientConnectionManager connMgr;

        private volatile boolean shutdown;

        public IdleConnectionEvictor(ClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(3000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than 30 sec
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                log.error(ex.getMessage(), ex);
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }
        
    public static void main(String[] args) throws IOException, Exception { }
    
}