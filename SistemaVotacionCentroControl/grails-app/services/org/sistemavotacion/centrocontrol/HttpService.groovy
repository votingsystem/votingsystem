package org.sistemavotacion.centrocontrol

import org.apache.http.util.EntityUtils;
import org.apache.http.entity.ContentType;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams
import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.seguridad.*;
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.mime.content.*
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.conn.HttpHostConnectException;

class HttpService {
	
	private static HttpClient httpclient;
	private static PoolingClientConnectionManager cm;
	private static IdleConnectionEvictor connEvictor;
	
	static {
		cm = new PoolingClientConnectionManager();
		cm.setMaxTotal(100);
		// set the connection timeout value to 15 seconds (15000 milliseconds)
		connEvictor = new IdleConnectionEvictor(cm);
		connEvictor.start();
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
		httpclient = new DefaultHttpClient(cm, httpParams);
	 }

	public Respuesta getInfo (String serverURL, String contentType)
			throws IOException, ParseException {
		log.debug("getInfo - serverURL: " + serverURL + " - contentType: "
				+ contentType);
		Respuesta respuesta = null;
		HttpGet httpget = new HttpGet(serverURL);
		HttpResponse response = null;
		try {
			if(contentType != null) httpget.setHeader("Content-Type", contentType);
			response = httpclient.execute(httpget);
			log.debug("----------------------------------------");
			log.debug("Connections in pool: " + cm.getTotalStats().getAvailable());
			/*Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
			System.out.println(headers[i]);
			}*/
			log.debug(response.getStatusLine().toString());
			log.debug("----------------------------------------");
			byte[] responseBytes = null;
			if(Respuesta.SC_OK == response.getStatusLine().getStatusCode())
				responseBytes = EntityUtils.toByteArray(response.getEntity());
			respuesta = new Respuesta(codigoEstado:response.getStatusLine().getStatusCode(),
						mensaje:new String(responseBytes), messageBytes:responseBytes);
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex);
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:ex.getMessage());
			httpget.abort();
		} finally {
			if(response != null) EntityUtils.consume(response.getEntity());
		}
		return respuesta;
	}
			
	
	public Respuesta sendMessage(byte[] byteArray, String contentType,
		String serverURL) throws IOException {
		log.debug("sendByteArray - contentType: " + contentType +
				" - serverURL: " + serverURL);
		Respuesta respuesta = null;
		HttpPost httpPost = new HttpPost(serverURL);
		try {
			ByteArrayEntity entity = null;
			if(contentType != null) {
				entity = new ByteArrayEntity(byteArray,  ContentType.create(contentType));
			} else entity = new ByteArrayEntity(byteArray);
			httpPost.setEntity(entity);
			HttpResponse response = httpclient.execute(httpPost);
			log.debug("----------------------------------------");
			log.debug(response.getStatusLine().toString());
			log.debug("----------------------------------------");
			byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
			respuesta = new Respuesta(mensaje:new String(responseBytes),
				codigoEstado:response.getStatusLine().getStatusCode(),
				messageBytes:responseBytes);
			//EntityUtils.consume(response.getEntity());
		} catch(HttpHostConnectException ex){
			log.error(ex.getMessage(), ex);
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR,
					mensaje:"hostConnectionErrorMsg");
			httpPost.abort();
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex);
			respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
			httpPost.abort();
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

		@Override public void run() {
			try {
				while (!shutdown) {
					synchronized (this) {
						wait(3000);
						// Close expired connections
						connMgr.closeExpiredConnections();
						// Optionally, close connections
						// that have been idle longer than 5 sec
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
	
}