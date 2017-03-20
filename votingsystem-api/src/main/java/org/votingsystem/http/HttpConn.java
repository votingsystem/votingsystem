package org.votingsystem.http;


import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.throwable.*;
import org.votingsystem.util.JSON;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class HttpConn {
    
    private static Logger log = Logger.getLogger(HttpConn.class.getName());

    public enum HTTPS_POLICY {
        //validate HTTPS server certificates with jvm keystore
        DEFAULT,
        //work without HTTPS server certificate validations
        ALL,
        //only allow HTTPS connections with application provided certificates
        PROVIDED;
    }

    private PoolingHttpClientConnectionManager connManager;
    private IdleConnectionEvictor connEvictor;
    private CloseableHttpClient httpClient;
    private HttpContext httpContext;
    public static HttpConn INSTANCE;

    private HttpConn(HTTPS_POLICY https_policy, Collection<X509Certificate> trustedCerts) throws KeyStoreException,
            NoSuchAlgorithmException, IOException, CertificateException, KeyManagementException {
        String message = (trustedCerts == null) ? "without trusted certs" : "trustedCerts size: " + trustedCerts.size();
        log.info(message + " - https_policy: " + https_policy);
        SSLContext sslContext = null;
        HostnameVerifier hostnameVerifier = null;
        SSLConnectionSocketFactory sslSocketFactory = null;
        Registry<ConnectionSocketFactory> socketFactoryRegistry = null;
        switch (https_policy) {
            case DEFAULT:
                connManager = new PoolingHttpClientConnectionManager();
                break;
            case ALL:
                log.severe(" **** HTTP REQUEST WILL NOT VALIDATE SSL CERTIFICATES **** ");
                // setup a Trust Strategy that allows all certificates.
                sslContext = new SSLContextBuilder().loadTrustMaterial(null, new org.apache.http.ssl.TrustStrategy() {
                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                        log.log(Level.SEVERE, "REQUEST WITHOUT VALIDATING SSL CERTIFICATE");
                        return true;
                    }
                }).build();
                // don't check Hostnames, either.
                //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
                hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

                // here's the special part:
                //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
                //      -- and create a Registry, to register it.
                sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
                socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslSocketFactory)
                        .build();

                // now, we create connection-manager using our Registry.
                //      -- allows multi-threaded use
                connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
                break;
            case PROVIDED:
                KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                for(X509Certificate certificate : trustedCerts) {
                    log.info("loading SSLContext with certificate: " + certificate.getSubjectDN().toString());
                    trustStore.setCertificateEntry(certificate.getSubjectDN().toString(), certificate);
                }

                sslContext = SSLContexts.custom().loadTrustMaterial(
                        trustStore, new TrustSelfSignedStrategy()).build();
                // don't check Hostnames, either.
                //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
                hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
                // here's the special part:
                //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
                //      -- and create a Registry, to register it.
                sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
                socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslSocketFactory).build();
                // now, we create connection-manager using our Registry.
                //      -- allows multi-threaded use
                connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
                break;
        }

        connManager.setMaxTotal(200);
        connManager.setDefaultMaxPerRoute(100);
        connEvictor = new IdleConnectionEvictor(connManager);
        connEvictor.start();
        httpClient = HttpClients.custom().setConnectionManager(connManager).build();
        CookieStore cookieStore = new BasicCookieStore();
        httpContext = new BasicHttpContext();
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
    }

    public Cookie getCookie(String domain) {
        CookieStore cookieStore = (CookieStore) httpContext.getAttribute(HttpClientContext.COOKIE_STORE);
        Cookie result = null;
        for(Cookie cookie : cookieStore.getCookies()) {
            if(cookie.getDomain().equals(domain)) 
                result = cookie;
        }
        return result;
    }

    public String getSessionId(String domain) {
        Cookie cookie = getCookie(domain);
        String result = null;
        if(cookie != null) {
            result = cookie.getValue().contains(".") ? cookie.getValue().split("\\.")[0] : cookie.getValue();
        }
        return result;
    }

    /**
     *
     * @param https_policy
     * @param trustedCerts allowed certificates to make HTTPS connections
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws IOException
     */
    public static void init(HTTPS_POLICY https_policy, Collection<X509Certificate> trustedCerts) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        INSTANCE = new HttpConn(https_policy, trustedCerts);
    }

    public static HttpConn getInstance() {
        return INSTANCE;
    }

    public void shutdown () {
        try {
            if(connEvictor != null) {
                connEvictor.shutdown();
                connEvictor.join();
            }
            if(httpClient != null) 
                httpClient.close();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public <T> T doGetRequest(TypeReference type, String serverURL, String mediaType) throws Exception {
        return doGetRequest(null, type, serverURL, mediaType);
    }

    public <T> T doGetRequest(Class<T> type, String serverURL, String mediaType) throws Exception {
        return doGetRequest(type, null, serverURL, mediaType);
    }

    public <T> T doGetRequest(Class<T> type, TypeReference typeReference, String targetURL, String mediaType) throws Exception {
        log.info("targetURL: " + targetURL + " - contentType: " + mediaType);
        HttpGet httpget = new HttpGet(targetURL);
        httpget.setHeader("Accept-Language", Locale.getDefault().getLanguage());
        if(mediaType != null) 
            httpget.setHeader("Content-Type", mediaType);
        CloseableHttpResponse response = httpClient.execute(httpget, httpContext);
        log.info("----------------------------------------");
        log.info(response.getStatusLine().toString() + " - connManager stats: " + connManager.getTotalStats().toString());
        log.info("----------------------------------------");
        try {
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            if(ResponseDto.SC_OK == response.getStatusLine().getStatusCode()) {
                if(type != null) 
                    return JSON.getMapper().readValue(responseBytes, type);
                else 
                    return JSON.getMapper().readValue(responseBytes, typeReference);
            } else {
                String responseStr = new String(responseBytes, StandardCharsets.UTF_8);
                switch (response.getStatusLine().getStatusCode()) {
                    case ResponseDto.SC_NOT_FOUND: 
                        throw new NotFoundException(responseStr);
                    case ResponseDto.SC_ERROR_REQUEST_REPEATED: 
                        throw new RequestRepeatedException(responseStr);
                    case ResponseDto.SC_ERROR_REQUEST: 
                        throw new BadRequestException(responseStr);
                    case ResponseDto.SC_ERROR: 
                        throw new ServerException(EntityUtils.toString(response.getEntity()));
                    default:
                        throw new ExceptionBase(responseStr);
                }
            }
        } finally {
            if(response != null) 
                response.close();
        }
    }

    public ResponseDto doGetRequest (String targetURL, String contentType) {
        ResponseDto responseDto = null;
        CloseableHttpResponse response = null;
        HttpGet httpget = null;
        log.info("targetURL: " + targetURL + " - contentType: " + contentType);
        try {
            httpget = new HttpGet(targetURL);
            if(contentType != null) httpget.setHeader("Content-Type", contentType);
            response = httpClient.execute(httpget, httpContext);
            String resContentType = null;
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) 
                resContentType = header.getValue();
            log.info("----------------------------------------");
            log.info(response.getStatusLine().toString() + " - resContentType:" +
                    resContentType + " - connManager stats: " + connManager.getTotalStats().toString());
            log.info("----------------------------------------");
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            if(responseDto.SC_OK == response.getStatusLine().getStatusCode()) {
                responseDto = new ResponseDto(response.getStatusLine().getStatusCode(),
                        responseBytes, resContentType);
            } else {
                responseDto = new ResponseDto(response.getStatusLine().getStatusCode(),
                        responseBytes, resContentType);
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseDto = new ResponseDto(responseDto.SC_ERROR, ex.getMessage());
            if(httpget != null) httpget.abort();
        } finally {
            try {
                if(response != null) response.close();
            } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
            return responseDto;
        }
    }

    public ResponseDto doPostRequest(byte[] byteArray, String contentType, String targetURL) {
        ResponseDto responseDto = null;
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        log.info("targetURL: " + targetURL + " - contentType: " + contentType);
        try {
            httpPost = new HttpPost(targetURL);
            //we send the User-Agent this way to avoid problems with http://ocsp.dnie.es
            httpPost.setHeader("User-Agent", "votingsystem-http-client");
            ByteArrayEntity entity = null;
            if(contentType != null) 
                entity = new ByteArrayEntity(byteArray,  org.apache.http.entity.ContentType.create(contentType));
            else 
                entity = new ByteArrayEntity(byteArray);
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost, httpContext);
            String resContentType = null;
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) 
                resContentType = header.getValue();
            log.info("----------------------------------------");
            log.info(response.getStatusLine().toString() + " - resContentType:" + resContentType +
                    " - connManager stats: " + connManager.getTotalStats().toString());
            log.info("----------------------------------------");
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            responseDto = new ResponseDto(response.getStatusLine().getStatusCode(), responseBytes, resContentType);
        } catch(HttpHostConnectException ex){
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseDto = new ResponseDto(responseDto.SC_ERROR, ex.getMessage());
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseDto = new ResponseDto(responseDto.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        } finally {
            try {
                if(response != null) 
                    response.close();
            } catch (Exception ex) { 
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return responseDto;
        }
    }

    public ResponseDto doPostForm(String targetURL, List<NameValuePair> urlParameters) {
        ResponseDto responseDto = null;
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        log.info("targetURL: " + targetURL);
        try {
            httpPost = new HttpPost(targetURL);
            //we send the User-Agent this way to avoid problems with http://ocsp.dnie.es
            httpPost.setHeader("User-Agent", "votingsystem-http-client");
            httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
            response = httpClient.execute(httpPost, httpContext);
            String resContentType = null;
            Header header = response.getFirstHeader("Content-Type");
            if(header != null)
                resContentType = header.getValue();
            log.info("----------------------------------------");
            log.info(response.getStatusLine().toString() + " - resContentType:" + resContentType +
                    " - connManager stats: " + connManager.getTotalStats().toString());
            log.info("----------------------------------------");
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            responseDto = new ResponseDto(response.getStatusLine().getStatusCode(), responseBytes, resContentType);
        } catch(HttpHostConnectException ex){
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseDto = new ResponseDto(responseDto.SC_ERROR, ex.getMessage());
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseDto = new ResponseDto(responseDto.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        } finally {
            try {
                if(response != null)
                    response.close();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return responseDto;
        }
    }

    public ResponseDto doPostSOAP(SOAPMessage soapMessage, String targetURL, String soapAction) throws Exception {
        byte[] soapMessageBytes = getSOAPMessageBytes(Level.FINEST, targetURL, soapMessage);
        ResponseDto responseDto = null;
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        log.info("targetURL: " + targetURL + " - soapAction: " + soapAction);
        try {
            httpPost = new HttpPost(targetURL);
            ByteArrayEntity entity = new ByteArrayEntity(soapMessageBytes,
                    org.apache.http.entity.ContentType.create("text/xml; charset=ISO-8859-1"));
            httpPost.setEntity(entity);
            httpPost.setHeader("SOAPAction", soapAction);
            response = httpClient.execute(httpPost, httpContext);
            String resContentType = null;
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) 
                resContentType = header.getValue();
            log.info("----------------------------------------");
            log.info(response.getStatusLine().toString() + " - resContentType: " +
                    resContentType + " - connManager stats: " + connManager.getTotalStats().toString());
            log.info("----------------------------------------");
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            responseDto = new ResponseDto(response.getStatusLine().getStatusCode(), responseBytes, resContentType);
        } catch(HttpHostConnectException ex){
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseDto = new ResponseDto(responseDto.SC_ERROR, ex.getMessage());
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseDto = new ResponseDto(responseDto.SC_ERROR, ex.getMessage());
            if(httpPost != null) 
                httpPost.abort();
        } finally {
            try {
                if(response != null) 
                    response.close();
                // Release current connection to the connection pool once you are done
                httpPost.releaseConnection();
            } catch (Exception ex) { 
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return responseDto;
        }
    }

    public byte[] getSOAPMessageBytes(Level logLevel, String msg, SOAPMessage soapMessage)
            throws IOException, SOAPException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapMessage.writeTo(baos);
        byte[] result = baos.toByteArray();
        if(logLevel == null) 
            logLevel = Level.INFO;
        log.log(logLevel, msg + " - soapMessage: " + new String(result));
        baos.close();
        return result;
    }


    public ResponseDto doPostMultipartRequest(Map<String, byte[]> fileMap, String targetURL) throws Exception {
        log.info("targetURL: " + targetURL);
        ResponseDto responseDto = null;
        if(fileMap == null || fileMap.isEmpty())
            throw new Exception("Empty request map");
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        ContentType resContentType = null;
        try {
            httpPost = new HttpPost(targetURL);
            Set<String> fileNames = fileMap.keySet();
            MultipartEntity reqEntity = new MultipartEntity();
            for(String fileName: fileNames){
                reqEntity.addPart(fileName, new ByteArrayBody(fileMap.get(fileName), fileName));
            }
            httpPost.setEntity(reqEntity);
            response = httpClient.execute(httpPost, httpContext);
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) 
                resContentType = ContentType.getByName(header.getValue());
            log.info("----------------------------------------");
            log.info(response.getStatusLine().toString() + " - resContentType: " + resContentType +
                    " - connManager stats: " + connManager.getTotalStats().toString());
            log.info("----------------------------------------");
            byte[] responseBytes =  EntityUtils.toByteArray(response.getEntity());
            responseDto = new ResponseDto(response.getStatusLine().getStatusCode(), responseBytes, resContentType);
            EntityUtils.consume(response.getEntity());
        } catch(Exception ex) {
            String statusLine = null;
            if(response != null) 
                statusLine = response.getStatusLine().toString();
            log.log(Level.SEVERE, ex.getMessage() + " - statusLine: " + statusLine, ex);
            responseDto = new ResponseDto(ResponseDto.SC_ERROR, ex.getMessage());
            if(httpPost != null) 
                httpPost.abort();
        }  finally {
            try {
                if(response != null) 
                    response.close();
            } catch (Exception ex) { 
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return responseDto;
        }
    }

    public <T> T doPostRequest(TypeReference type, byte[] data, String serverURL, ContentType contentType) throws Exception {
        return doPostRequest(null, type, data, contentType, serverURL);
    }

    public <T> T doPostRequest(Class<T> type, byte[] data, String serverURL, ContentType contentType) throws Exception {
        return doPostRequest(type, null, data, contentType, serverURL);
    }

    public <T> T doPostRequest(Class<T> type, TypeReference typeReference, byte[] byteArray,
                          ContentType contentType, String targetURL) throws IOException, ExceptionBase {
        log.info("targetURL: " + targetURL + " - contentType: " + contentType);
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        httpPost = new HttpPost(targetURL);
        ByteArrayEntity entity = null;
        if(contentType != null) 
            entity = new ByteArrayEntity(byteArray,  org.apache.http.entity.ContentType.create(contentType.getName()));
        else entity = new ByteArrayEntity(byteArray);
        httpPost.setEntity(entity);
        response = httpClient.execute(httpPost, httpContext);
        String resContentType = "";
        Header header = response.getFirstHeader("Content-Type");
        if(header != null) 
            resContentType = header.getValue();
        log.info("------------------------------------------------");
        log.info(response.getStatusLine().toString() + " - resContentType: " + resContentType +
                " - connManager stats: " + connManager.getTotalStats().toString());
        log.info("------------------------------------------------");
        byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
        if(ResponseDto.SC_OK == response.getStatusLine().getStatusCode()) {
            if(type != null) 
                return JSON.getMapper().readValue(responseBytes, type);
            else 
                return JSON.getMapper().readValue(responseBytes, typeReference);
        } else {
            String responseStr = new String(responseBytes, "UTF-8");
            switch (response.getStatusLine().getStatusCode()) {
                case ResponseDto.SC_NOT_FOUND: 
                    throw new NotFoundException(responseStr);
                case ResponseDto.SC_ERROR_REQUEST_REPEATED: 
                    throw new RequestRepeatedException(responseStr);
                case ResponseDto.SC_ERROR_REQUEST: 
                    throw new BadRequestException(responseStr);
                case ResponseDto.SC_ERROR: 
                    throw new ServerException(EntityUtils.toString(response.getEntity()));
                default:
                    throw new HttpRequestException(responseStr);
            }
        }
    }

    public static class IdleConnectionEvictor extends Thread {

        private final HttpClientConnectionManager connMgr;

        private volatile boolean shutdown;

        public IdleConnectionEvictor(HttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(1000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections that have been idle longer than 30 sec
                        connMgr.closeIdleConnections(5, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public static String getMAC() throws SocketException {
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        byte[] mac = nis.nextElement().getHardwareAddress();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        return sb.toString();
    }

    public static String getLocalIP() throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)){
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                if(inetAddress.isSiteLocalAddress()) {
                    String inetAddressStr = inetAddress.toString();
                    while(inetAddressStr.startsWith("/")) 
                        inetAddressStr = inetAddressStr.substring(1);
                    return inetAddressStr;
                }

            }
        }
        return null;
    }
    
}