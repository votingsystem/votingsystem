package org.votingsystem.accesscontrol.service

//import groovyx.net.http.*
//import static groovyx.net.http.ContentType.*
//import static groovyx.net.http.Method.*
import org.apache.http.entity.ContentType;
import java.io.IOException;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.votingsystem.accesscontrol.model.*
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.*;

import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import org.apache.http.HttpResponse
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.codehaus.groovy.grails.web.json.*

import java.util.Locale;
import java.io.IOException;
import java.text.ParseException;

import org.apache.http.util.EntityUtils;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.conn.HttpHostConnectException;

import javax.persistence.Transient;

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

	public ResponseVS getInfo (String serverURL, String contentType)
			throws IOException, ParseException {
		log.debug("getInfo - serverURL: " + serverURL + " - contentType: "
				+ contentType);
		ResponseVS respuesta = null;
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
			if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode())
				responseBytes = EntityUtils.toByteArray(response.getEntity());
			respuesta = new ResponseVS(statusCode:response.getStatusLine().getStatusCode(),
						message:new String(responseBytes), messageBytes:responseBytes);
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex);
			respuesta = new ResponseVS(statusCode:ResponseVS.SC_ERROR, 
				message:ex.getMessage());
			httpget.abort();
		} finally {
			if(response != null) EntityUtils.consume(response.getEntity());
		}
		return respuesta;
	}
			
	public ResponseVS sendMessage(byte[] byteArray, String contentType,
			String serverURL) throws IOException {
		log.debug("sendByteArray - contentType: " + contentType +
				" - serverURL: " + serverURL);
		ResponseVS respuesta = null;
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
			respuesta = new ResponseVS(message:new String(responseBytes),
				statusCode:response.getStatusLine().getStatusCode(),
				messageBytes:responseBytes);
			//EntityUtils.consume(response.getEntity());
		} catch(HttpHostConnectException ex){
			log.error(ex.getMessage(), ex);
			respuesta = new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					message:"hostConnectionErrorMsg");
			httpPost.abort();
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex);
			respuesta = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
			httpPost.abort();
		}
		return respuesta;
	}

	
	/* ConcurrentHashMap notificarActoresInicializacionDeEvento (byte[] message,
			EventoVotacion eventoVotacion, Set<ActorConIP> actores) {
		log.debug("notificarActoresInicializacionDeEvento - eventoVotacion: ${eventoVotacion.asunto} - numero actores: ${actores.size()}")
		Executor executor = Executors.newFixedThreadPool(actores.size())
		final ConcurrentHashMap<ActorConIP, Respuesta> mapaRespuestas =
			new ConcurrentHashMap<ActorConIP, Respuesta>(actores.size())
		try {
			for (final ActorConIP actor : actores) {
				executor.execute(new Runnable(){
					public void run () {//No añadir en estos hilos lógica de acceso a la BD!!!
						def respuesta = notificarActorInicializacionDeEvento(actor, eventoVotacion)
						mapaRespuestas.put(actor, respuesta)
					}
				})
			}
		} finally {
				executor.shutdown()
				//executor.awaitTermination(Long.valueOf(grailsApplication.config.VotingSystem.requestTimeOut), TimeUnit.MILLISECONDS)
				executor.awaitTermination(2000, TimeUnit.MILLISECONDS)
		}
		//comprobarRespuestasActores(mapaRespuestas, eventoVotacion)
		return mapaRespuestas
	}*/
		

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
