package org.currency.test.android;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.*;
import okio.ByteString;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.socket.SocketOperation;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.Constants;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketInitSessionTest extends BaseTest {

    private static final Logger log = Logger.getLogger(WebSocketInitSessionTest.class.getName());

    private static final boolean IS_DEBUG_SESSION = true;

    private WebSocket ws;


    public static void main(String[] args) throws Exception {
        //new WebSocketInitSessionTest().initSession();
        new WebSocketInitSessionTest().initSession();
        //System.exit(0);
    }

    public void initSession() throws Exception {
        //OkHttpClient client = new OkHttpClient();
        OkHttpClient client = null;
        if (IS_DEBUG_SESSION) {
            client = getUnsafeHttpClient();
        } else {
            //client = new OkHttpClient.Builder().readTimeout(0,  TimeUnit.MILLISECONDS).build();
            client = new OkHttpClient.Builder().build();
        }
        Request request = new Request.Builder().url("wss://votingsystem.ddns.net/currency-server/websocket/service").build();
        //Request request = new Request.Builder().url("ws://votingsystem.ddns.net:8080/currency-server/websocket/service").build();
        ws = client.newWebSocket(request, new AppWebSocketListener());

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    public final class AppWebSocketListener extends WebSocketListener {

        @Override public void onOpen(WebSocket webSocket, Response response) {
            MessageDto messageDto = new MessageDto();
            OperationTypeDto operationType = new OperationTypeDto(SocketOperation.MSG_TO_DEVICE,
                    org.currency.test.Constants.CURRENCY_SERVICE_ENTITY_ID);
            messageDto.setOperation(operationType).setDate(ZonedDateTime.now());
            messageDto.setMessage("Hello from encryptd connection");
            messageDto.setUUID(UUID.randomUUID().toString());
            try {
                webSocket.send(JSON.getMapper().writeValueAsString(messageDto));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            webSocket.close(1000, " --- Connection closed ---");
        }

        @Override public void onMessage(WebSocket webSocket, String text) {
            System.out.println("MESSAGE: " + text);
        }

        @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
            System.out.println("MESSAGE: " + bytes.hex());
        }

        @Override public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(1000, null);
            System.out.println("CLOSE: " + code + " " + reason);
        }

        @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            t.printStackTrace();
        }

    }

    private static OkHttpClient getUnsafeHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };
            X509TrustManager x509TrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, x509TrustManager);

            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}