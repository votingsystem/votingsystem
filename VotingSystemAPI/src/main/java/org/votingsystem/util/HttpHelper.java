package org.votingsystem.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.*;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.*;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class HttpHelper {
    
    private static Logger log = Logger.getLogger(HttpHelper.class.getSimpleName());

    private static final int REQUEST_TIME_OUT = 60000; //60 seconds

    private PoolingHttpClientConnectionManager connManager;
    private IdleConnectionEvictor connEvictor;
    private SSLConnectionSocketFactory sslSocketFactory;
    private CloseableHttpClient httpClient;
    public static HttpHelper INSTANCE;

    // Use custom message parser / writer to customize the way HTTP
    // messages are parsed from and written out to the data stream.
    HttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory() {
        @Override public HttpMessageParser<HttpResponse> create(
                SessionInputBuffer buffer, MessageConstraints constraints) {
            LineParser lineParser = new BasicLineParser() {
                @Override public Header parseHeader(final CharArrayBuffer buffer) {
                    try {
                        return super.parseHeader(buffer);
                    } catch (ParseException ex) {
                        return new BasicHeader(buffer.toString(), null);
                    }
                }
            };
            return new DefaultHttpResponseParser(
                    buffer, lineParser, DefaultHttpResponseFactory.INSTANCE, constraints) {
                @Override protected boolean reject(final CharArrayBuffer line, int count) {
                    // try to ignore all garbage preceding a status line infinitely
                    return false;
                }
            };
        }
    };
    HttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();
    // Use a custom connection factory to customize the process of
    // initialization of outgoing HTTP connections. Beside standard connection
    // configuration parameters HTTP connection factory can define message
    // parser / writer routines to be employed by individual connections.
    HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(
            requestWriterFactory, responseParserFactory);

    // Use custom DNS resolver to override the system DNS resolution.
    DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
        @Override public InetAddress[] resolve(final String host) throws UnknownHostException {
            /*if (host.equalsIgnoreCase("myhost")) {
                return new InetAddress[] { InetAddress.getByAddress(new byte[] {127, 0, 0, 1}) };
            } else { return super.resolve(host);  }*/
            return super.resolve(host);
        }

    };

    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    try {
                        return ContextVS.getInstance().getVotingSystemSSLCerts().toArray(new X509Certificate[]{});
                    } catch (Exception ex) {log.log(Level.SEVERE, ex.getMessage(), ex);}
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    log.info("trustAllCerts - checkClientTrusted");
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType ) throws CertificateException {
                    log.info("trustAllCerts - checkServerTrusted");
                    try {
                        CertUtils.verifyCertificate(ContextVS.getInstance().getVotingSystemSSLTrustAnchors(), false,
                                Arrays.asList(certs));
                    } catch(Exception ex) {
                        throw new CertificateException(ex.getMessage());
                    }
                }
            }
    };

    private HttpHelper() {
        try {
            KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            SSLContext sslcontext = null;
            SSLConnectionSocketFactory sslsf = null;
            if(ContextVS.getInstance().getVotingSystemSSLCerts() != null) {
                log.info("loading SSLContext with app certificates");
                X509Certificate sslServerCert = ContextVS.getInstance().getVotingSystemSSLCerts().iterator().next();
                trustStore.setCertificateEntry(sslServerCert.getSubjectDN().toString(), sslServerCert);
                sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore).build();
                X509HostnameVerifier hostnameVerifier = (X509HostnameVerifier)new AllowAllHostnameVerifier();
                sslsf = new SSLConnectionSocketFactory(sslcontext,  new String[] { "TLSv1" }, null, hostnameVerifier);
            } else {
                sslcontext = SSLContexts.createSystemDefault();
                sslsf = new SSLConnectionSocketFactory(sslcontext);
                log.info("loading default SSLContext");
            }
            // Create a registry of custom connection socket factories for supported protocol schemes.
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", sslsf).build();
            //Create socket configuration
            //SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
            //Configure the connection manager to use socket configuration either by default or for a specific host.
            //connManager.setDefaultSocketConfig(socketConfig);
            connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, dnsResolver);
            connManager.setMaxTotal(200);
            connManager.setDefaultMaxPerRoute(100);
            connEvictor = new IdleConnectionEvictor(connManager);
            connEvictor.start();
            HttpRoute httpRouteVS = new HttpRoute( new HttpHost("www.sistemavotacion.org", 80));
            connManager.setMaxPerRoute(httpRouteVS, 200);
            /* timeouts with large simulations ->
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(REQUEST_TIME_OUT)
                    .setConnectionRequestTimeout(REQUEST_TIME_OUT).setSocketTimeout(REQUEST_TIME_OUT).build();
            httpClient = HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(
                    requestConfig).build();*/
            httpClient = HttpClients.custom().setConnectionManager(connManager).build();
        } catch(Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
    }


    public static HttpHelper getInstance() {
        if(INSTANCE == null) INSTANCE = new HttpHelper();
        return INSTANCE;
    }

    public void shutdown () {
        try {
            if(connEvictor != null) {
                connEvictor.shutdown();
                connEvictor.join();
            }
            if(httpClient != null) httpClient.close();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public <T> T getData(TypeReference type, String serverURL, String mediaType) throws Exception {
        return getData(null, type, serverURL, mediaType);
    }

    public <T> T getData(Class<T> type, TypeReference typeReference, String serverURL, String mediaType) throws Exception {
        log.info("getData - contentType: " + mediaType + " - serverURL: " + serverURL);
        CloseableHttpResponse response = null;
        HttpGet httpget = null;
        String responseContentType = null;
        httpget = new HttpGet(serverURL);
        httpget.setHeader("Accept-Language", Locale.getDefault().getLanguage());
        if(mediaType != null) httpget.setHeader("Content-Type", mediaType);
        response = httpClient.execute(httpget);
        log.info("----------------------------------------");
            /*Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) { System.out.println(headers[i]); }*/
        log.info(response.getStatusLine().toString() + " - connManager stats: " +
                connManager.getTotalStats().toString());
        log.info("----------------------------------------");
        Header header = response.getFirstHeader("Content-Type");
        if(header != null) responseContentType = header.getValue();
        try {
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
                if(type != null) return JSON.getMapper().readValue(responseBytes, type);
                else return JSON.getMapper().readValue(responseBytes, typeReference);
            } else {
                MessageDto messageDto = null;
                String responseStr = null;
                if(responseContentType != null && responseContentType.contains(MediaTypeVS.JSON)) messageDto =
                        JSON.getMapper().readValue(responseBytes, MessageDto.class);
                else responseStr = new String(responseBytes, StandardCharsets.UTF_8);
                switch (response.getStatusLine().getStatusCode()) {
                    case ResponseVS.SC_NOT_FOUND: throw new NotFoundExceptionVS(responseStr, messageDto);
                    case ResponseVS.SC_ERROR_REQUEST_REPEATED: throw new RequestRepeatedExceptionVS(responseStr, messageDto);
                    case ResponseVS.SC_ERROR_REQUEST: throw new BadRequestExceptionVS(responseStr, messageDto);
                    case ResponseVS.SC_ERROR: throw new ServerExceptionVS(EntityUtils.toString(response.getEntity()), messageDto);
                    default:throw new ExceptionVS(responseStr, messageDto);
                }
            }
        } finally {
            if(response != null) response.close();
        }
    }

    public <T> T getData(Class<T> type, String serverURL, String mediaType) throws Exception {
        return getData(type, null, serverURL, mediaType);
    }


    public ResponseVS getData (String serverURL, ContentTypeVS contentType) {
        log.info("getData - contentType: " + contentType + " - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        CloseableHttpResponse response = null;
        HttpGet httpget = null;
        ContentTypeVS responseContentType = null;
        try {
            httpget = new HttpGet(serverURL);
            //httpget.setHeader("Accept-Language", Locale.getDefault().getLanguage());
            if(contentType != null) httpget.setHeader("Content-Type", contentType.getName());
            response = httpClient.execute(httpget);
            log.info("----------------------------------------");
            /*Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) { System.out.println(headers[i]); }*/
            log.info(response.getStatusLine().toString() + " - connManager stats: " +
                    connManager.getTotalStats().toString());
            log.info("----------------------------------------");
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
            if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes, responseContentType);
            } else {
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()), responseContentType);
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "hostConnectionErrorMsg", serverURL));
            if(httpget != null) httpget.abort();
        } finally {
            try {
                if(response != null) response.close();
            } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
            return responseVS;
        }
    }
    
    public ResponseVS sendFile (File file, ContentTypeVS contentTypeVS, String serverURL,  String... headerNames) {
        log.info("sendFile - contentType: " + contentTypeVS + " - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        HttpPost httpPost = null;
        ContentTypeVS responseContentType = null;
        CloseableHttpResponse response = null;
        try {
            httpPost = new HttpPost(serverURL);
            ContentType contentType = null;
            if(contentTypeVS != null) contentType = ContentType.create(contentTypeVS.getName());
            FileEntity entity = new FileEntity(file, contentType);
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost);
            log.info("----------------------------------------");
            log.info(response.getStatusLine().toString() + " - connManager stats: " +
                    connManager.getTotalStats().toString());
            log.info("----------------------------------------");
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
            byte[] responseBytes =  EntityUtils.toByteArray(response.getEntity());
            responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes, responseContentType);
            if(headerNames != null) {
                List<String> headerValues = new ArrayList<String>();
                for(String headerName: headerNames) {
                    org.apache.http.Header headerValue = response.getFirstHeader(headerName);
                    headerValues.add(headerValue.getValue());
                }
                responseVS.setData(headerValues);
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        } finally {
            try {
                if(response != null) response.close();
            } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
            return responseVS;
        }
    }

    public <T> T sendData(TypeReference type, byte[] data, String serverURL, ContentTypeVS contentType) throws Exception {
        return sendData(null, type, data, contentType, serverURL);
    }

    public <T> T sendData(Class<T> type, byte[] data, String serverURL, ContentTypeVS contentType) throws Exception {
        return sendData(type, null, data, contentType, serverURL);
    }

    public <T> T sendData(Class<T> type, TypeReference typeReference, byte[] byteArray,
                                 ContentTypeVS contentType, String serverURL, String... headerNames) throws IOException, ExceptionVS {
        log.info("sendData - contentType: " + contentType + " - serverURL: " + serverURL);
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        httpPost = new HttpPost(serverURL);
        ByteArrayEntity entity = null;
        if(contentType != null) entity = new ByteArrayEntity(byteArray,  ContentType.create(contentType.getName()));
        else entity = new ByteArrayEntity(byteArray);
        httpPost.setEntity(entity);
        response = httpClient.execute(httpPost);
        String responseContentType = "";
        Header header = response.getFirstHeader("Content-Type");
        if(header != null) responseContentType = header.getValue();
        log.info("------------------------------------------------");
        log.info(response.getStatusLine().toString() + " - contentTypeVS: " + responseContentType +
                " - connManager stats: " + connManager.getTotalStats().toString());
        log.info("------------------------------------------------");
        byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
        if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
            if(type != null) return JSON.getMapper().readValue(responseBytes, type);
            else return JSON.getMapper().readValue(responseBytes, typeReference);
        } else {
            MessageDto messageDto = null;
            String responseStr = null;
            if(responseContentType.contains(MediaTypeVS.JSON)) messageDto =
                    JSON.getMapper().readValue(responseBytes, MessageDto.class);
            else responseStr = new String(responseBytes, "UTF-8");
            switch (response.getStatusLine().getStatusCode()) {
                case ResponseVS.SC_NOT_FOUND: throw new NotFoundExceptionVS(responseStr, messageDto);
                case ResponseVS.SC_ERROR_REQUEST_REPEATED: throw new RequestRepeatedExceptionVS(responseStr, messageDto);
                case ResponseVS.SC_ERROR_REQUEST: throw new BadRequestExceptionVS(responseStr, messageDto);
                case ResponseVS.SC_ERROR: throw new ServerExceptionVS(EntityUtils.toString(response.getEntity()), messageDto);
                default:throw new ExceptionVS(responseStr, messageDto);
            }
        }
    }

    public ResponseVS sendData(byte[] byteArray, ContentTypeVS contentType,
            String serverURL, String... headerNames) throws IOException {
        log.info("sendData - contentType: " + contentType + " - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        try {
            httpPost = new HttpPost(serverURL);
            ByteArrayEntity entity = null;
            if(contentType != null) entity = new ByteArrayEntity(byteArray,  ContentType.create(contentType.getName()));
            else entity = new ByteArrayEntity(byteArray);
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost);
            ContentTypeVS responseContentType = null;
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
            log.info("------------------------------------------------");
            log.info(response.getStatusLine().toString() + " - contentTypeVS: " + responseContentType +
                    " - connManager stats: " + connManager.getTotalStats().toString());
            log.info("------------------------------------------------");
            byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
            responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes, responseContentType);
            if(headerNames != null && headerNames.length > 0) {
                List<String> headerValues = new ArrayList<String>();
                for(String headerName: headerNames) {
                    org.apache.http.Header headerValue = response.getFirstHeader(headerName);
                    if(headerValue != null) headerValues.add(headerValue.getValue());
                }
                responseVS.setData(headerValues);
            }
        } catch(HttpHostConnectException ex){
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                    getMessage("hostConnectionErrorMsg", serverURL));
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        } finally {
            try {
                if(response != null) response.close();
            } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
            return responseVS;
        }
    }
    
    public ResponseVS sendObjectMap(Map<String, Object> fileMap, String serverURL) throws Exception {
        log.info("sendObjectMap - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        if(fileMap == null || fileMap.isEmpty()) throw new Exception(
                ContextVS.getInstance().getMessage("requestWithoutFileMapErrorMsg"));
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        ContentTypeVS responseContentType = null;
        try {
            httpPost = new HttpPost(serverURL);
            Set<String> fileNames = fileMap.keySet();
            MultipartEntity reqEntity = new MultipartEntity();
            for(String objectName: fileNames){
                Object objectToSend = fileMap.get(objectName);
                if(objectToSend instanceof File) {
                    File file = (File)objectToSend;
                    log.info("sendObjectMap - fileName: " + objectName + " - filePath: " + file.getAbsolutePath());
                    FileBody  fileBody = new FileBody(file);
                    reqEntity.addPart(objectName, fileBody);
                } else if (objectToSend instanceof byte[]) {
                    byte[] byteArray = (byte[])objectToSend;
                    reqEntity.addPart(objectName, new ByteArrayBody(byteArray, objectName));
                }
            }
            httpPost.setEntity(reqEntity);
            response = httpClient.execute(httpPost);
            Header header = response.getFirstHeader("Content-Type");
            if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
            log.info("----------------------------------------");
            log.info(response.getStatusLine().toString() + " - contentTypeVS: " + responseContentType +
                    " - connManager stats: " + connManager.getTotalStats().toString());
            log.info("----------------------------------------");
            byte[] responseBytes =  EntityUtils.toByteArray(response.getEntity());
            responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes, responseContentType);
            EntityUtils.consume(response.getEntity());
        } catch(Exception ex) {
            String statusLine = null;
            if(response != null) statusLine = response.getStatusLine().toString();
            log.log(Level.SEVERE, ex.getMessage() + " - StatusLine: " + statusLine, ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }  finally {
            try {
                if(response != null) response.close();
            } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
            return responseVS;
        }
    }

    public static class IdleConnectionEvictor extends Thread {

        private final HttpClientConnectionManager connMgr;

        private volatile boolean shutdown;

        public IdleConnectionEvictor(HttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override public void run() {
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
                    while(inetAddressStr.startsWith("/")) inetAddressStr = inetAddressStr.substring(1);
                    return inetAddressStr;
                }

            }
        }
        return null;
    }
    
}