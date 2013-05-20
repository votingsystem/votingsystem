package org.sistemavotacion.red;

import java.io.IOException;
import java.text.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.File;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.sistemavotacion.seguridad.CertUtil;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class HttpHelper {
    
    private static Logger logger = LoggerFactory.getLogger(HttpHelper.class);
    
    private HttpClient httpclient;

    public HttpHelper () {
        /*SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(
        new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));*/
        ClientConnectionManager cm = new PoolingClientConnectionManager();
        // set the connection timeout value to 15 seconds (15000 milliseconds)
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
        httpclient = new DefaultHttpClient(cm, httpParams);
    }
    
    public void shutdown () {
        try { httpclient.getConnectionManager().shutdown(); } 
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public HttpResponse obtenerArchivo (String serverURL) 
            throws IOException, ParseException {
        logger.debug("obtenerArchivo - serverURL: " + serverURL);             
        HttpGet httpget = new HttpGet(serverURL);
        HttpResponse response = httpclient.execute(httpget);
        logger.debug("----------------------------------------");
        /*Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
        System.out.println(headers[i]);
        }*/
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;    
    }
    
    public X509Certificate obtenerCertificadoDeServidor (String serverURL) throws Exception {
        logger.debug("obtenerCertificadoDeServidor - serverURL: " + serverURL);           
        HttpGet httpget = new HttpGet(serverURL);
        X509Certificate certificado = null;
        HttpResponse response = httpclient.execute(httpget);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (200 == response.getStatusLine().getStatusCode()) {
            certificado = CertUtil.fromPEMToX509Cert(EntityUtils.toByteArray(entity));
        }
        return certificado;
    }
    
    public Collection<X509Certificate> obtenerCadenaCertificacionDeServidor (
            String serverURL) throws Exception {
        logger.debug("obtenerCadenaCertificacionDeServidor - serverURL: " + serverURL);   
        HttpGet httpget = new HttpGet(serverURL);
        Collection<X509Certificate> certificados = null;
        logger.debug("obtenerCertificadoDeServidor - lanzando: " + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (200 == response.getStatusLine().getStatusCode()) {
            certificados = 
                    CertUtil.fromPEMToX509CertCollection(EntityUtils.toByteArray(entity));
        }
        return certificados;
    }
   
    public PKIXParameters obtenerPKIXParametersDeServidor (String certChainURL) throws Exception {
        logger.debug("obtenerPKIXParametersDeServidor - certChainURL: " + certChainURL);
        Set<TrustAnchor> anchors = obtenerAnchorsDeServidor(certChainURL);
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false); // tell system do not check CRL's
        return params;       
    }   
    
    public Set<TrustAnchor> obtenerAnchorsDeServidor (String certChainURL) throws Exception {
        logger.debug("obtenerAnchorsDeServidor - certChainURL: " + certChainURL);
        Collection<X509Certificate> certificados = obtenerCadenaCertificacionDeServidor(certChainURL);
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        for (X509Certificate certificado:certificados) {
            TrustAnchor anchorCertificado = new TrustAnchor(certificado, null);
            anchors.add(anchorCertificado);
        }
        return anchors;
    }
    
    /*public HttpResponse enviarArchivoFirmado (
            File archivoFirmado, String serverURL) throws IOException {
        logger.debug("enviarArchivoFirmado - lanzando: " + serverURL);        
        HttpPost httpPost = new HttpPost(serverURL);
        FileBody fileBody = new FileBody(archivoFirmado);
        /*MultipartEntity reqEntity = new MultipartEntity();
        logger.debug("enviarArchivoFirmado - reqEntity.getContentType().getValue(): " + reqEntity.getContentType().getValue());   
        reqEntity.addPart(Contexto.SMIME_FILE_NAME, fileBody);
        httpPost.setEntity(reqEntity);
        httpPost.addHeader(new BasicHeader(
                "Content-Type", "application/x-pkcs7-signature, application/x-pkcs7-mime, "
                + reqEntity.getContentType().getValue()));*/
        /*FileEntity entity;
        entity = new FileEntity(archivoFirmado, ContentType.create(
                "application/x-pkcs7-signature, application/x-pkcs7-mime"));
         httpPost.setEntity(entity);
        HttpResponse response = httpclient.execute(httpPost); 
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;*/
    //}
    
    public HttpResponse sendFile (File file, String contentType, 
            String serverURL) throws IOException {
        logger.debug("sendFile - contentType: " + contentType + 
                " - serverURL: " + serverURL);        
        HttpPost httpPost = new HttpPost(serverURL);
        FileEntity entity = new FileEntity(file, ContentType.create(contentType));
        httpPost.setEntity(entity);
        HttpResponse response = httpclient.execute(httpPost);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------"); 
        
        ContentType.create(
                "application/x-pkcs7-signature, application/x-pkcs7-mime");
        
        return response;
    }
    
    public HttpResponse sendObjectMap(
            Map<String, Object> fileMap, String serverURL) throws Exception {
        logger.debug("sendObjectMap - serverURL: " + serverURL);        
        if(fileMap == null || fileMap.isEmpty()) throw new Exception(
                Contexto.getString("requestWithoutFileMapErrorMsg"));
        HttpPost httpPost = new HttpPost(serverURL);
        Set<String> fileNames = fileMap.keySet();
        MultipartEntity reqEntity = new MultipartEntity();
        for(String objectName: fileNames){
            Object objectToSend = fileMap.get(objectName);
            if(objectToSend instanceof File) {
                File file = (File)objectToSend;
                logger.debug("sendFileMap - fileName: " + objectName + 
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
        HttpResponse response = httpclient.execute(httpPost);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;  
    }
    
    public HttpResponse sendByteArray(byte[] byteArray, String contentType,
            String serverURL) throws IOException {
        logger.debug("sendByteArray - serverURL: " + serverURL);
        HttpPost httpPost = new HttpPost(serverURL);
        ByteArrayEntity entity = null;
        if(contentType != null) {
            entity = new ByteArrayEntity(byteArray,  ContentType.create(contentType));
        } else entity = new ByteArrayEntity(byteArray); 
        httpPost.setEntity(entity);
        HttpResponse response = httpclient.execute(httpPost);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;  
    }
    
    public HttpResponse sendString (String stringToSend, 
            String paramName, String serverURL) throws IOException {
        logger.debug("sendString - serverURL: " + serverURL);
        HttpPost httpPost = new HttpPost(serverURL);
        StringBody stringBody = new StringBody(stringToSend);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        reqEntity.addPart(paramName, stringBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;
    }

    public static void main(String[] args) throws IOException, Exception {
        HttpHelper httpHelper = new HttpHelper();
        /*HttpResponse response = httpHelper.sendByteArray("hola".getBytes(),
                "http://localhost:8080/SistemaVotacionControlAcceso/timeStamp");*/
        /*HttpResponse response = httpHelper.sendFile(
                new File("/home/jgzornoza/emailPDF"), 
                "application/x-pkcs7-signature, application/x-pkcs7-mime",
                "http://localhost:8080/SistemaVotacionControlAcceso/prueba");*/
        Map<String, Object> mapToSend = new HashMap<String, Object>();
        mapToSend.put("def", new File("/home/jgzornoza/emailPDF"));
        mapToSend.put("solicitudAcceso:" + Contexto.ENCRYPTED_CONTENT_TYPE, 
                new File("/home/jgzornoza/caldecott.log"));
        
        HttpResponse response = Contexto.getHttpHelper().sendObjectMap(
                mapToSend, "http://localhost:8080/SistemaVotacionControlAcceso/prueba");
        String message = EntityUtils.toString(response.getEntity());
        logger.debug("message: " + message);
    }
}