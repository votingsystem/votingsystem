package org.currency.test.android;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.*;
import okio.ByteString;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.socket.SocketOperation;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.testlib.util.IOUtils;
import org.votingsystem.util.JSON;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketSessionTest extends BaseTest {

    private static final Logger log = Logger.getLogger(WebSocketSessionTest.class.getName());

    private static final boolean IS_DEBUG_SESSION = true;

    private WebSocket webSocket;
    private OkHttpClient httpClient;

    public static void main(String[] args) throws Exception {
        WebSocketSessionTest webSocketSessionTest = new WebSocketSessionTest();
        webSocketSessionTest.run();
        //System.exit(0);
    }

    public void initSession() throws Exception {
        httpClient = null;
        if (IS_DEBUG_SESSION) {
            httpClient = getUnsafeHttpClient();
        } else {
            //httpClient = new OkHttpClient.Builder().readTimeout(0,  TimeUnit.MILLISECONDS).build();
            httpClient = new OkHttpClient.Builder().build();
        }
        Request request = new Request.Builder().url(org.votingsystem.util.Constants.CURRENCY_SOCKET_SERVICE).build();
        webSocket = httpClient.newWebSocket(request, new AppWebSocketListener());
    }

    private void run() throws Exception {
        initSession();
        String commands = Arrays.asList(Command.values()).stream().map(c -> c.toString().toLowerCase()).reduce(
                (t, u) -> t + ", " + u).get();
        log.severe("Commands:" + commands + " - Enter 'quit' to exit the application");
        while (true) {
            final String stringCommand = IOUtils.readLine("> ");
            try {
                final Command command = Command.parseCommand(stringCommand);
                switch (command) {
                    case MESSAGE:
                        sendMessage(stringCommand.trim());
                        break;
                    case QUIT:
                        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
                        webSocket.close(1000, " --- Connection closed ---");
                        httpClient.dispatcher().executorService().shutdown();
                        System.exit(0);
                        break;
                    default:
                        log.warning("Unknown command " + stringCommand);
                }
            } catch (Exception ex) {
                log.warning(ex.getMessage());
            }
        }
    }

    private void sendMessage(String message) throws JsonProcessingException {
        MessageDto messageDto = new MessageDto();
        OperationTypeDto operationType = new OperationTypeDto(SocketOperation.MSG_TO_DEVICE,
                org.currency.test.Constants.CURRENCY_SERVICE_ENTITY_ID);
        messageDto.setOperation(operationType).setDate(ZonedDateTime.now());
        messageDto.setMessage(message);
        messageDto.setUUID(UUID.randomUUID().toString());
        webSocket.send(JSON.getMapper().writeValueAsString(messageDto));
    }

    private void logMessage(String message) {
        System.out.println("------------------------------------------------------");
        System.out.println(message);
        System.out.println("------------------------------------------------------");
    }

    private enum Command {
        MESSAGE, QUIT;
        public static Command parseCommand(String stringCommand) {
            try {
                return valueOf(stringCommand.trim().toUpperCase());
            } catch (IllegalArgumentException iae) {
                log.warning("Message to websocket server: " + stringCommand);
                return MESSAGE;
            }
        }
    }

    public final class AppWebSocketListener extends WebSocketListener {

        @Override public void onOpen(WebSocket webSocket, Response response) {
            log.info("Connected to server : " + org.votingsystem.util.Constants.CURRENCY_SOCKET_SERVICE);
        }

        @Override public void onMessage(WebSocket webSocket, String text) {
            log.info("Message from server: " + text);
        }

        @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
            log.info("ByteString message from server: " + bytes.hex());
        }

        @Override public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(1000, null);
            log.info("WebSocket Connection CLOSED code: " + code + " - reason: " + reason);
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
                        public void checkClientTrusted(X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
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