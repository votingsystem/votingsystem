package org.votingsystem.test.voting

import org.votingsystem.signature.util.CertUtils
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.FileUtils
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger

import java.security.cert.X509Certificate

Logger log = TestUtils.init(Test.class, [:])

KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
trustStore.load(null, null);
Collection<java.security.cert.X509Certificate> votingSystemSSLCerts =  CertUtils.fromPEMToX509CertCollection(
        FileUtils.getBytesFromInputStream(Thread.currentThread().
                getContextClassLoader().getResourceAsStream("VotingSystemSSLCert.pem")));
X509Certificate sslServerCert = votingSystemSSLCerts.iterator().next();
trustStore.setCertificateEntry(sslServerCert.getSubjectDN().toString(), sslServerCert);

// Trust own CA and all self-signed certs
SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();
// Allow TLSv1 protocol only
String[] prots = ["TLSv1"].toArray()
SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext,  prots, null,
        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
try {

    HttpGet httpget = new HttpGet("https://www.sistemavotacion.org:8443/AccessControl/serverInfo");

    System.out.println("executing request" + httpget.getRequestLine());

    CloseableHttpResponse response = httpclient.execute(httpget);
    try {
        HttpEntity entity = response.getEntity();

        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        if (entity != null) {
            System.out.println("Response content length: " + entity.getContentLength());
        }
        EntityUtils.consume(entity);
    } finally {
        response.close();
    }
} finally {
    httpclient.close();
}
//ResponseVS responseVS = HttpHelper.getInstance().getData("https://www.sistemavotacion.org:8443", null);
/*try {
    KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
    trustStore.load(null, null);
    Collection<java.security.cert.X509Certificate> votingSystemSSLCerts =  CertUtils.fromPEMToX509CertCollection(
            FileUtils.getBytesFromInputStream(Thread.currentThread().
                    getContextClassLoader().getResourceAsStream("VotingSystemSSLCert.pem")));


    X509Certificate sslServerCert = votingSystemSSLCerts.iterator().next();
    trustStore.setCertificateEntry(sslServerCert.getSubjectDN().toString(), sslServerCert);
    SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
    Scheme sch = new Scheme("https", 8443, socketFactory);
    log.debug("Added Scheme https with port 8443 to Apache httpclient");
    final HttpParams httpParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
    PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
    cm.setMaxTotal(10);
    cm.setDefaultMaxPerRoute(10);
    HttpClient httpclient = new DefaultHttpClient(cm, httpParams);
    httpclient.getConnectionManager().getSchemeRegistry().register(sch);
    HttpGet httpget = new HttpGet("https://www.sistemavotacion.org:8443/AccessControl/serverInfo");
    response = httpclient.execute(httpget);
    println("----------------------------------------");
    /*Header[] headers = response.getAllHeaders();
    for (int i = 0; i < headers.length; i++) { System.out.println(headers[i]); }*/
    /*println(response.getStatusLine().toString());
    println("----------------------------------------");
    Header header = response.getFirstHeader("Content-Type");
    if(header != null) responseContentType = ContentTypeVS.getByName(header.getValue());
    if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
        byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
        responseVS = new ResponseVS(response.getStatusLine().getStatusCode(), responseBytes, responseContentType);
    } else {
        responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                EntityUtils.toString(response.getEntity()), responseContentType);
    }
}catch(Exception ex) {
    ex.printStackTrace()
}*/



