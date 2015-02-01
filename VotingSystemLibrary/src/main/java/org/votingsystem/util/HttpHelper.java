package org.votingsystem.util;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
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
import org.apache.log4j.Logger;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class HttpHelper {
    
    private static Logger log = Logger.getLogger(HttpHelper.class);

    private static final int REQUEST_TIME_OUT = 30000; //30 seconds

    private PoolingHttpClientConnectionManager connManager;
    private IdleConnectionEvictor connEvictor;
    private RequestConfig requestConfig;
    private SSLConnectionSocketFactory sslSocketFactory;
    private boolean sslMode = false;
    public static HttpHelper INSTANCE = new HttpHelper();

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

    private HttpHelper() {
        try {
            KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            SSLContext sslcontext = null;
            if(ContextVS.getInstance().getVotingSystemSSLCerts() != null) {
                log.debug("loading SSLContext with app certifcates");
                X509Certificate sslServerCert = ContextVS.getInstance().getVotingSystemSSLCerts().iterator().next();
                trustStore.setCertificateEntry(sslServerCert.getSubjectDN().toString(), sslServerCert);
                sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore).build();
            } else {
                sslcontext = SSLContexts.createSystemDefault();
                log.debug("loading default SSLContext");
            }
            // Create a registry of custom connection socket factories for supported protocol schemes.
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", new SSLConnectionSocketFactory(sslcontext))
                    .build();
            // Create socket configuration
            SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
            // Configure the connection manager to use socket configuration either
            // by default or for a specific host.
            connManager.setDefaultSocketConfig(socketConfig);
            connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, dnsResolver);
            connManager.setMaxTotal(200);
            connManager.setDefaultMaxPerRoute(20);
            connEvictor = new IdleConnectionEvictor(connManager);
            connEvictor.start();
            requestConfig = RequestConfig.custom().setConnectTimeout(REQUEST_TIME_OUT)
                    .setConnectionRequestTimeout(REQUEST_TIME_OUT).setSocketTimeout(REQUEST_TIME_OUT).build();
        } catch(Exception ex) { log.error(ex.getMessage(), ex);}
    }

    public static HttpHelper getInstance() {
        return INSTANCE;
    }


    public void shutdown () {
        try {
            if(connEvictor != null) {
                connEvictor.shutdown();
                connEvictor.join();
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public ResponseVS getData (String serverURL, ContentTypeVS contentType) {
        log.debug("getData - contentType: "  + contentType + " - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        HttpResponse response = null;
        HttpGet httpget = null;
        ContentTypeVS responseContentType = null;
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(
                requestConfig).build();;
        try {
            httpget = new HttpGet(serverURL);
            if(contentType != null) httpget.setHeader("Content-Type", contentType.getName());
            response = httpClient.execute(httpget);
            log.debug("----------------------------------------");
            /*Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
            System.out.println(headers[i]);
            }*/
            log.debug(response.getStatusLine().toString());
            log.debug("----------------------------------------");
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
            log.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "hostConnectionErrorMsg", serverURL));
            if(httpget != null) httpget.abort();
        } finally {
            try {
                if(response != null) EntityUtils.consume(response.getEntity());
                if(httpClient != null) httpClient.close();
            } catch (Exception ex) { log.error(ex.getMessage(), ex);}
            return responseVS;
        }
    }
    
    public ResponseVS sendFile (File file, ContentTypeVS contentTypeVS, String serverURL,  String... headerNames) {
        log.debug("sendFile - contentType: " + contentTypeVS +  " - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        HttpPost httpPost = null;
        ContentTypeVS responseContentType = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(
                requestConfig).build();;
        try {
            httpPost = new HttpPost(serverURL);
            ContentType contentType = null;
            if(contentTypeVS != null) contentType = ContentType.create(contentTypeVS.getName());
            FileEntity entity = new FileEntity(file, contentType);
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost);
            log.debug("----------------------------------------");
            log.debug(response.getStatusLine().toString());
            log.debug("----------------------------------------");
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
            log.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        } finally {
            try {
                if(response != null) EntityUtils.consume(response.getEntity());
                if(httpClient != null) httpClient.close();
            } catch (Exception ex) { log.error(ex.getMessage(), ex);}
            return responseVS;
        }
    }
    
    public ResponseVS sendData(byte[] byteArray, ContentTypeVS contentType,
            String serverURL, String... headerNames) throws IOException {
        log.debug("sendData - contentType: " + contentType + " - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        HttpPost httpPost = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(
                requestConfig).build();;
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
            log.debug("------------------------------------------------");
            log.debug(response.getStatusLine().toString() + " - contentTypeVS: " + responseContentType);
            log.debug("------------------------------------------------");
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
            log.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                    getMessage("hostConnectionErrorMsg", serverURL));
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        } finally {
            if(response != null) EntityUtils.consume(response.getEntity());
            if(httpClient != null) httpClient.close();
            return responseVS;
        }
    }
    
    public ResponseVS sendObjectMap(Map<String, Object> fileMap, String serverURL) throws Exception {
        log.debug("sendObjectMap - serverURL: " + serverURL);
        ResponseVS responseVS = null;
        if(fileMap == null || fileMap.isEmpty()) throw new Exception(
                ContextVS.getInstance().getMessage("requestWithoutFileMapErrorMsg"));
        HttpPost httpPost = null;
        HttpResponse response = null;
        ContentTypeVS responseContentType = null;
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(
                requestConfig).build();
        try {
            httpPost = new HttpPost(serverURL);
            Set<String> fileNames = fileMap.keySet();
            MultipartEntity reqEntity = new MultipartEntity();
            for(String objectName: fileNames){
                Object objectToSend = fileMap.get(objectName);
                if(objectToSend instanceof File) {
                    File file = (File)objectToSend;
                    log.debug("sendObjectMap - fileName: " + objectName + " - filePath: " + file.getAbsolutePath());
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
            log.debug("----------------------------------------");
            log.debug(response.getStatusLine().toString() + " - contentTypeVS: " + responseContentType);
            log.debug("----------------------------------------");
            byte[] responseBytes =  EntityUtils.toByteArray(response.getEntity());
            responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes, responseContentType);
            EntityUtils.consume(response.getEntity());
        } catch(Exception ex) {
            String statusLine = null;
            if(response != null) statusLine = response.getStatusLine().toString();
            log.error(ex.getMessage() + " - StatusLine: " + statusLine, ex);
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            if(httpPost != null) httpPost.abort();
        }  finally {
            if(response != null) EntityUtils.consume(response.getEntity());
            if(httpClient != null) httpClient.close();
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