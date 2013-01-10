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
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.Contexto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.sistemavotacion.seguridad.CertUtil;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class HttpHelper {
    
    private static Logger logger = LoggerFactory.getLogger(HttpHelper.class);
    
    private HttpClient httpclient;
    ThreadSafeClientConnManager cm;

    public HttpHelper () {
        cm = new ThreadSafeClientConnManager();
        cm.setMaxTotal(50);
        httpclient = new DefaultHttpClient(cm);
    }
    
    public void shutdown () {
        try { httpclient.getConnectionManager().shutdown(); } 
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public HttpResponse obtenerArchivo (String serverURL) 
            throws IOException, ParseException {
        checkConnections();
        logger.debug("obtenerArchivo - lanzando: " + serverURL);             
        HttpGet httpget = new HttpGet(serverURL);
        logger.debug("obtenerInformacion - lanzando: " + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        logger.debug("----------------------------------------");
        /*Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
        System.out.println(headers[i]);
        }*/
        logger.debug("Connections in pool: " + cm.getConnectionsInPool());
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;    
    }
    
    public X509Certificate obtenerCertificadoDeServidor (String serverURL) throws Exception {
        checkConnections();
        logger.debug("obtenerCertificadoDeServidor - lanzando: " + serverURL);           
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
    
    public Collection<X509Certificate> obtenerCadenaCertificacionDeServidor (String serverURL) throws Exception {
        checkConnections();
        logger.debug("obtenerCadenaCertificacionDeServidor - lanzando: " + serverURL);   
        HttpGet httpget = new HttpGet(serverURL);
        Collection<X509Certificate> certificados = null;
        logger.debug("obtenerCertificadoDeServidor - lanzando: " + httpget.getURI());
        HttpResponse response = httpclient.execute(httpget);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        HttpEntity entity = response.getEntity();
        if (200 == response.getStatusLine().getStatusCode()) {
            certificados = CertUtil.fromPEMChainToX509Certs(EntityUtils.toByteArray(entity));
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
    
    public HttpResponse enviarArchivoFirmado (
            File archivoFirmado, String serverURL) throws IOException {
        checkConnections();
        logger.debug("enviarArchivoFirmado - lanzando: " + serverURL);        
        HttpPost httpPost = new HttpPost(serverURL);
        logger.debug("enviarArchivoFirmado - lanzando: " + httpPost.getURI());
        FileBody fileBody = new FileBody(archivoFirmado);
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart(Contexto.NOMBRE_ARCHIVO_ENVIADO_FIRMADO, fileBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;
    }
    
    public HttpResponse enviarSolicitudAcceso(byte[] solicitudCSR, 
            File solicitudAccesoSMIME, String serverURL) throws IOException {
        checkConnections();
        logger.debug("enviarSolicitudAcceso - lanzando: " + serverURL);        
        HttpPost httpPost = new HttpPost(serverURL);
        FileBody fileBody = new FileBody(solicitudAccesoSMIME);
        ByteArrayBody  csrBody = new ByteArrayBody(solicitudCSR, 
                Contexto.NOMBRE_ARCHIVO_CSR_ENVIADO);
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart(Contexto.NOMBRE_ARCHIVO_ENVIADO_FIRMADO, fileBody);
        reqEntity.addPart(Contexto.NOMBRE_ARCHIVO_CSR_ENVIADO, csrBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;  
    }
    
    public HttpResponse enviarByteArray(byte[] byteArray, String serverURL) throws IOException {
        checkConnections();
        logger.debug("enviarByteArray - lanzando: " + serverURL);
        HttpPost httpPost = new HttpPost(serverURL);
        ByteArrayBody  byteArrayBody = new ByteArrayBody(byteArray, 
                Contexto.NOMBRE_ARCHIVO_ENVIADO_FIRMADO);
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart(Contexto.NOMBRE_ARCHIVO_ENVIADO_FIRMADO, byteArrayBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;  
    }
    
    public HttpResponse enviarCadena (
            String cadenaFirmada, String serverURL) throws IOException {
        checkConnections();
        logger.debug("enviarCadena - lanzando: " + serverURL);
        HttpPost httpPost = new HttpPost(serverURL);
        StringBody stringBody = new StringBody(cadenaFirmada);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        reqEntity.addPart(Contexto.NOMBRE_ARCHIVO_ENVIADO_FIRMADO, stringBody);
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        logger.debug("----------------------------------------");
        logger.debug(response.getStatusLine().toString());
        logger.debug("----------------------------------------");
        return response;
    }

    private void checkConnections () {
        if (cm.getConnectionsInPool() == 0) {
            logger.debug("No hay conexiones en pool");
            cm.closeIdleConnections(10, TimeUnit.SECONDS);
        }
    }

}