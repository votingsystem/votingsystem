package org.votingsystem.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class HttpHelper {
    
    private static Logger logger = Logger.getLogger(HttpHelper.class);
    
    private HttpClient httpclient;
    private PoolingClientConnectionManager cm;
    private IdleConnectionEvictor connEvictor;

    public static HttpHelper INSTANCE = null;
    
    
    private HttpHelper() {
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
    

    
    public static HttpHelper getInstance() {
        if(INSTANCE == null) INSTANCE = new HttpHelper();
        return INSTANCE;
    }

    
    public synchronized void initMultiThreadedMode() {
        logger.debug("initMultiThreadedMode");
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
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public ResponseVS getData (String serverURL, String contentType) 
            throws IOException, ParseException {
        logger.debug("getData - serverURL: " + serverURL + " - contentType: " 
                + contentType);  
        ResponseVS responseVS = null;
        HttpResponse response = null;
        HttpGet httpget = null;
        try {
            httpget = new HttpGet(serverURL);
            if(contentType != null) httpget.setHeader("Content-Type", contentType);
            response = httpclient.execute(httpget);
            logger.debug("----------------------------------------");
            /*Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
            System.out.println(headers[i]);
            }*/
            logger.debug(response.getStatusLine().toString());
            logger.debug("----------------------------------------");
            if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes);
            } else {
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()));
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                    getMessage("hostConnectionErrorMsg", serverURL));
            if(httpget != null) httpget.abort();
        } finally {
            if(response != null) EntityUtils.consume(response.getEntity());
            return responseVS;
        }
    }
    
    public X509Certificate getCertFromServer (String serverURL) throws Exception {
        logger.debug("getCertFromServer - serverURL: " + serverURL);
        HttpGet httpget = new HttpGet(serverURL);
        X509Certificate certificate = null;
        HttpResponse response = httpclient.execute(httpget);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
            certificate = CertUtil.fromPEMToX509Cert(EntityUtils.toByteArray(entity));
        }
        EntityUtils.consume(response.getEntity());
        return certificate;
    }
    
    public Collection<X509Certificate> getCertChainHTTP (
            String serverURL) throws Exception {
        logger.debug("getCertChainHTTP - serverURL: " + serverURL);
        HttpGet httpget = new HttpGet(serverURL);
        Collection<X509Certificate> certificates = null;
        logger.debug("getCertFromServer - lanzando: " + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
            certificates = CertUtil.fromPEMToX509CertCollection(
                    EntityUtils.toByteArray(entity));
        }
        EntityUtils.consume(response.getEntity());
        return certificates;
    }
   
    public PKIXParameters getPKIXParametersHTTP (String certChainURL) throws Exception {
        logger.debug("getPKIXParametersHTTP - certChainURL: " + certChainURL);
        Set<TrustAnchor> anchors = getTrustAnchorHTTP(certChainURL);
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false); // tell system do not check CRL's
        return params;       
    }   
    
    public Set<TrustAnchor> getTrustAnchorHTTP (String certChainURL) throws Exception {
        logger.debug("getTrustAnchorHTTP - certChainURL: " + certChainURL);
        Collection<X509Certificate> certificates = getCertChainHTTP(certChainURL);
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        for (X509Certificate certificate:certificates) {
            TrustAnchor certAnchor = new TrustAnchor(certificate, null);
            anchors.add(certAnchor);
        }
        return anchors;
    }

    
    public ResponseVS sendFile (File file, String contentType, String serverURL, 
            String... headerNames) {
        logger.debug("sendFile - contentType: " + contentType + 
                " - serverURL: " + serverURL); 
        ResponseVS responseVS = null;
        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(serverURL);
            FileEntity entity = new FileEntity(file, ContentType.create(contentType));
            httpPost.setEntity(entity);
            HttpResponse response = httpclient.execute(httpPost);
            logger.debug("----------------------------------------");
            logger.debug(response.getStatusLine().toString());
            logger.debug("----------------------------------------"); 
            byte[] responseBytes =  EntityUtils.toByteArray(response.getEntity());
            responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes);
            if(headerNames != null) {
                List<String> headerValues = new ArrayList<String>();
                for(String headerName: headerNames) {
                    org.apache.http.Header headerValue = 
                            response.getFirstHeader(headerName);
                    headerValues.add(headerValue.getValue());
                }
                responseVS.setData(headerValues);
            }
            EntityUtils.consume(response.getEntity());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }
        return responseVS;
    }
        
        
    public ResponseVS sendString (String stringToSend, 
            String paramName, String serverURL) {
        logger.debug("sendString - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(serverURL);
            StringBody stringBody = new StringBody(stringToSend);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
            reqEntity.addPart(paramName, stringBody);
            httpPost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httpPost);
            logger.debug("----------------------------------------");
            logger.debug(response.getStatusLine().toString());
            logger.debug("----------------------------------------");
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }
        return responseVS;
    }
    
    public ResponseVS sendData(byte[] byteArray, String contentType,
            String serverURL, String... headerNames) throws IOException {
        logger.debug("sendData - contentType: " + contentType + " - serverURL: " + serverURL);
        ResponseVS responseVS = null;
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
            ContentTypeVS responseContentType = ContentTypeVS.getByName(
                    response.getFirstHeader("Content-Type").getValue());
            logger.debug("------------------------------------------------");
            logger.debug("status: " + response.getStatusLine().toString() + " - contentType:" + responseContentType);
            logger.debug("------------------------------------------------");
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes);
            responseVS.setContentType(responseContentType);
            if(headerNames != null && headerNames.length > 0) {
                List<String> headerValues = new ArrayList<String>();
                for(String headerName: headerNames) {
                    org.apache.http.Header headerValue = response.getFirstHeader(headerName);
                    if(headerValue != null) headerValues.add(headerValue.getValue());
                }
                responseVS.setData(headerValues);
            }
            EntityUtils.consume(response.getEntity());
        } catch(HttpHostConnectException ex){
            logger.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                    getMessage("hostConnectionErrorMsg", serverURL));
            if(httpPost != null) httpPost.abort();
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }  finally {
            if(response != null) EntityUtils.consume(response.getEntity());
            return responseVS;
        }
    }
    
    
    public ResponseVS sendObjectMap(Map<String, Object> fileMap, String serverURL) throws Exception {
        logger.debug("sendObjectMap - serverURL: " + serverURL); 
        ResponseVS responseVS = null;
        if(fileMap == null || fileMap.isEmpty()) throw new Exception(
                ContextVS.getInstance().getMessage("requestWithoutFileMapErrorMsg"));
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
                    logger.debug("sendFileMap - fileName: " + objectName + " - filePath: " + file.getAbsolutePath());
                    FileBody  fileBody = new FileBody(file);
                    reqEntity.addPart(objectName, fileBody);
                } else if (objectToSend instanceof byte[]) {
                    byte[] byteArray = (byte[])objectToSend;
                    reqEntity.addPart(objectName, new ByteArrayBody(byteArray, objectName));
                }
            }
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);     
            logger.debug("----------------------------------------");
            logger.debug(response.getStatusLine().toString());
            logger.debug("----------------------------------------");
            byte[] responseBytes =  EntityUtils.toByteArray(response.getEntity());
            responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes);
            //EntityUtils.consume(response.getEntity());
        } catch(Exception ex) {
            String statusLine = null;
            if(response != null) {
                statusLine = response.getStatusLine().toString();
            }
            logger.error(ex.getMessage() + " - StatusLine: " + statusLine, ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }
        return responseVS;  
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
                logger.error(ex.getMessage(), ex);
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }


    public static String getLocalIP() throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)){
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                if(inetAddress.isSiteLocalAddress()) {
                    String inetAddressStr = inetAddress.toString();
                    while(inetAddressStr.startsWith("/")) inetAddressStr = inetAddressStr.substring(1);
                    return inetAddressStr;
                }

            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException, Exception { }
    
}