package org.votingsystem.client.webextension.service;

import com.google.common.collect.Sets;
import javafx.concurrent.Task;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.dialog.CertNotFoundDialog;
import org.votingsystem.client.webextension.dialog.QRDialog;
import org.votingsystem.client.webextension.util.InboxMessage;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.*;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.service.EventBusService;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.KeyStoreUtil;
import org.votingsystem.util.currency.Wallet;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketService {

    private static Logger log = Logger.getLogger(WebSocketService.class.getName());

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private static WebSocketService instance;
    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private ActorVS targetServer;
    private Session session;
    private String serverSessionId;
    private UserVS userVS;

    private WebSocketService(Collection<X509Certificate> sslServerCertCollection, ActorVS targetServer) {
        this.targetServer = targetServer;
        if(targetServer.getWebSocketURL().startsWith("wss")) {
            log.info("settings for SECURE connection");
            try {
                KeyStore p12Store = KeyStore.getInstance("PKCS12");
                p12Store.load(null, null);
                for(X509Certificate serverCert: sslServerCertCollection) {
                    p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                }
                byte[] p12KeyStoreBytes = KeyStoreUtil.getBytes(p12Store, keyStorePassw.toCharArray());
                SslContextConfigurator sslContext = new SslContextConfigurator();
                sslContext.setTrustStoreType("PKCS12");
                sslContext.setTrustStoreBytes(p12KeyStoreBytes);
                sslContext.setTrustStorePassword(keyStorePassw);

                SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(sslContext);
                sslEngineConfigurator.setHostnameVerifier(new HostnameVerifier() {
                    @Override public boolean verify(String hostname, SSLSession sslSession) {
                        log.log(Level.SEVERE, "HostnameVerifier - DEVELOPMENT - bypassing normal validation!!! - hostname: " + hostname);
                        return true;
                    }
                });
                client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else log.info("settings for WebSocket - INSECURE - connection");
    }

    public static WebSocketService getInstance() {
        try {
            if(ContextVS.getInstance().getCurrencyServer() != null) {
                if(instance == null) instance =  new WebSocketService(ContextVS.getInstance().
                        getVotingSystemSSLCerts(), ContextVS.getInstance().getCurrencyServer());
            } else BrowserHost.showMessage(ResponseVS.SC_ERROR, "SOCKETS - " +
                    ContextVS.getInstance().getMessage("connectionErrorMsg"));
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
        return instance;
    }

    public static class EndpointConfigurator extends ClientEndpointConfig.Configurator {

        @Override public void beforeRequest(Map<String, List<String>> headers) {
            //headers.put("Cookie", Arrays.asList("sessionVS=7180db71-3331-4e57-a448-5e7755e5dd3c"));
            headers.put("Origin", Arrays.asList(ContextVS.getInstance().getCurrencyServer().getServerURL()));
        }

        @Override public void afterResponse(HandshakeResponse handshakeResponse) {
            //final Map<String, List<String>> headers = handshakeResponse.getHeaders();
        }
    }

    @ClientEndpoint(configurator = EndpointConfigurator.class)
    public class WSEndpoint {

        private String connectionMessage;

        public WSEndpoint(String connectionMessage) {
            this.connectionMessage = connectionMessage;
        }

        @OnOpen public void onOpen(Session session) throws IOException {
            if(connectionMessage != null) session.getBasicRemote().sendText(connectionMessage);
            WebSocketService.this.session = session;
        }

        @OnClose public void onClose(Session session, CloseReason closeReason) {
            EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.DISCONNECT));
        }

        @OnMessage public void onMessage(String message) {
            try {
                consumeMessage(JSON.getMapper().readValue(message, SocketMessageDto.class));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public void setConnectionEnabled(boolean isConnectionEnabled){
        if(isConnectionEnabled) {
            if(session == null || !session.isOpen()) {
                try {
                    client.connectToServer(new WSEndpoint(null), URI.create(targetServer.getWebSocketURL()));
                } catch(Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.DISCONNECT));
                    BrowserHost.showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
                }
            } else {
                showQRConnectionDialog();
                EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.CONNECT));
            }
        } else  {
            if(session != null && session.isOpen()) {
                try {session.close();}
                catch(Exception ex) {log.log(Level.SEVERE, ex.getMessage(), ex);}
            } else EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.DISCONNECT));
        }
    }

    public boolean isConnected() {
        if(session != null && session.isOpen()) return true;
        else return false;
    }

    public boolean isConnectedWithAlert() {
        if(isConnected()) return true;
        else {
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("connectionRequiredForServiceErrorMsg"));
            return false;
        }
    }

    public void sendMessage(String message) {
        log.info("sendMessage: " + message);
        try {
            session.getBasicRemote().sendText(message);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public ResponseVS sendMessageVS(OperationVS operationVS) throws Exception {
        log.info("sendMessageVS");
        if(isConnected()) {
            Collection<DeviceVSDto> userDevices = null;
            if(operationVS.getUserVS() != null) {
                userDevices = operationVS.getUserVS().getConnectedDevices();
            } else {
                UserVSDto userVSDto = HttpHelper.getInstance().getData(UserVSDto.class,
                        ContextVS.getInstance().getCurrencyServer().getDeviceVSConnectedServiceURL(
                                BrowserSessionService.getInstance().getUserVS().getNif()), MediaTypeVS.JSON);
                userDevices = userVSDto.getConnectedDevices();
            }
            if(userDevices == null || userDevices.isEmpty()) return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage(
                    "uservsWithoutDevicesConnectedMsg", operationVS.getNif()));
            for (DeviceVSDto deviceVSDto : userDevices) {
                DeviceVS deviceVS = deviceVSDto.getDeviceVS();
                SocketMessageDto messageDto = SocketMessageDto.getMessageVSToDevice(BrowserSessionService.getInstance().getUserVS(),
                        deviceVS, operationVS.getNif(), operationVS.getMessage());
                sendMessage(JSON.getMapper().writeValueAsString(messageDto));
            }
        } return ResponseVS.ERROR(ContextVS.getMessage("authenticatedWebSocketConnectionRequiredMsg"));
    }

    private void showQRConnectionDialog() {
        QRMessageDto qrDto = new QRMessageDto().setSessionId(serverSessionId).setOperation(TypeVS.INIT_REMOTE_SIGNED_SESSION);
        QRDialog.showDialog(qrDto, ContextVS.getMessage("initAuthenticatedSessionMsg"),
                ContextVS.getMessage("initAuthenticatedWebSocketSessionMsg"));
    }

    private void consumeMessage(final SocketMessageDto socketMsg) {
        if(ResponseVS.SC_ERROR == socketMsg.getStatusCode()) {
            BrowserHost.showMessage(socketMsg.getStatusCode(), socketMsg.getMessage());
            return;
        }
        try {
            if(socketMsg.getOperation() == TypeVS.MESSAGEVS_FROM_VS) { //check messages from system
                socketMsg.setOperation(socketMsg.getMessageType());
                log.info("MESSAGEVS_FROM_VS - operation: " + socketMsg.getOperation());
                switch (socketMsg.getOperation()) {
                    case TRANSACTIONVS_INFO:
                        break;
                    case INIT_SESSION:
                        serverSessionId = socketMsg.getSessionId();
                        showQRConnectionDialog();
                        break;
                    case INIT_REMOTE_SIGNED_SESSION:
                        BrowserSessionService.getInstance().initAuthenticatedSession(socketMsg);
                        break;
                    default: log.info("MESSAGEVS_FROM_VS - UNPROCESSED - MessageType: " + socketMsg.getMessageType());
                }
                if(ResponseVS.SC_WS_CONNECTION_NOT_FOUND == socketMsg.getStatusCode()) {
                    BrowserHost.showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                }
                EventBusService.getInstance().post(socketMsg);
                return;
            }
            WebSocketSession socketSession = ContextVS.getInstance().getWSSession(socketMsg.getUUID());
            if(socketSession == null) {
                BrowserSessionService.decryptMessage(socketMsg);
                socketSession = new WebSocketSession(socketMsg);
                ContextVS.getInstance().putWSSession(socketMsg.getUUID(), socketSession);
            } else socketMsg.decryptMessage(socketSession.getAESParams());
            socketMsg.setWebSocketSession(socketSession);
            log.info("consumeMessage - type: " + socketMsg.getOperation() + " - MessageType: " +
                    socketMsg.getMessageType() + " - status: " + socketMsg.getStatusCode());
            switch(socketMsg.getOperation()) {
                case CURRENCY_WALLET_CHANGE:
                case MESSAGEVS:
                case MSG_TO_DEVICE_BY_TARGET_DEVICE_ID:
                    InboxService.getInstance().newMessage(new InboxMessage(socketMsg));
                    break;
                case MSG_TO_DEVICE_BY_TARGET_SESSION_ID:
                    break;
                case MESSAGEVS_SIGN:
                    if(ResponseVS.SC_CANCELED == socketMsg.getStatusCode()){
                        socketMsg.setStatusCode(ResponseVS.SC_ERROR);
                        BrowserSessionService.setSignResponse(socketMsg);
                    }
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    BrowserSessionService.setSignResponse(socketMsg);
                    break;
                case QR_MESSAGE_INFO:
                    //the payer has read our QR code and ask for details
                    if(ResponseVS.SC_ERROR != socketMsg.getStatusCode()) {
                        SocketMessageDto msgDto = null;
                        Long deviceFromId = BrowserSessionService.getInstance().getConnectedDevice().getId();
                        try {
                            QRMessageDto<TransactionVSDto> qrDto = BrowserHost.getInstance().getQRMessage(
                                    socketMsg.getMessage());
                            qrDto.setHashCertVS(socketMsg.getContent().getHashCertVS());
                            TransactionVSDto transactionDto = qrDto.getData();
                            //we send the csr of the currency in order to allow anonymous transactions. If the payer wants
                            //to make the transaction anonymous, he will send us the signed CSR
                            Currency currency =  new  Currency(
                                    ContextVS.getInstance().getCurrencyServer().getServerURL(),
                                    transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                                    transactionDto.isTimeLimited(), qrDto.getHashCertVS(),
                                    new TagVS(transactionDto.getTagName()));
                            qrDto.setCurrency(currency);
                            //we sign the CSR in order to provide the payer a proof that we asked for the payment.
                            //If the payer decides to make the payment anonymous and request the currency from the server
                            //he can't spend it without the private key.
                            CMSSignedMessage cmsMessage = BrowserSessionService.getCMS(null, targetServer.getName(),
                                    new String(currency.getCertificationRequest().getCsrPEM()), null,
                                    ContextVS.getMessage("currencyChangeSubject"));
                            transactionDto.setCmsMessagePEM(cmsMessage.toPEMStr());
                            msgDto = socketMsg.getResponse(ResponseVS.SC_OK,JSON.getMapper().writeValueAsString(transactionDto),
                                    deviceFromId, cmsMessage, TypeVS.TRANSACTIONVS_INFO);
                            socketSession.setData(qrDto);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            msgDto = socketMsg.getResponse(ResponseVS.SC_ERROR,
                                    ex.getMessage(), deviceFromId, null, TypeVS.QR_MESSAGE_INFO);
                        } finally {
                            session.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(msgDto));
                        }
                    } else BrowserHost.showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                    break;
                case TRANSACTIONVS_RESPONSE:
                    //the payer has completed the payment and send the details
                    if(ResponseVS.SC_ERROR != socketMsg.getStatusCode()) {
                        try {
                            CMSSignedMessage cmsMessage = socketMsg.getCMS();
                            QRMessageDto<TransactionVSDto> qrDto =
                                    (QRMessageDto<TransactionVSDto>) socketSession.getData();
                            TransactionVSDto dto = cmsMessage.getSignedContent(TransactionVSDto.class);
                            if(TypeVS.CURRENCY_CHANGE == dto.getOperation()) {
                                Currency currency = qrDto.getCurrency();
                                currency.initSigner(socketMsg.getMessage().getBytes());
                                qrDto.setCurrency(currency);
                                qrDto.setTypeVS(TypeVS.CURRENCY_CHANGE);
                                Wallet.saveToPlainWallet(Sets.newHashSet(currency));
                            }
                            BrowserHost.getInstance().removeQRMessage(qrDto.getUUID());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                        }
                    } else BrowserHost.showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                    break;
                default:
                    log.info("unprocessed socketMsg: " + socketMsg.getOperation());
            }
            EventBusService.getInstance().post(socketMsg);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public class InitSessionTask extends Task<ResponseVS> {

        private char[] password;
        private ActorVS targetServer;

        public InitSessionTask (char[] password, ActorVS targetServer) {
            this.password = password;
            this.targetServer = targetServer;
        }

        @Override public ResponseVS call()  {
            ResponseVS responseVS = null;
            try {
                SocketMessageDto dto = SocketMessageDto.INIT_SESSION_REQUEST(
                        BrowserSessionService.getInstance().getDevice().getDeviceId());
                //updateMessage(ContextVS.getMessage("connectionMsg"));
                updateMessage(ContextVS.getMessage("checkDeviceVSCryptoTokenMsg"));
                CMSSignedMessage cmsMessage = BrowserSessionService.getCMS(null, targetServer.getName(),
                        JSON.getMapper().writeValueAsString(dto), password,
                        ContextVS.getMessage("initAuthenticatedSessionMsg"));
                userVS = cmsMessage.getSigner();
                String connectionMessage = JSON.getMapper().writeValueAsString(dto.setCMS(cmsMessage));
                client.connectToServer(new WSEndpoint(connectionMessage), URI.create(targetServer.getWebSocketURL()));
                responseVS = ResponseVS.OK().setCMS(cmsMessage);
            } catch (KeyStoreExceptionVS ex) {
                CertNotFoundDialog.showDialog(ex.getMessage());
            } catch(InterruptedException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.DISCONNECT));
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.DISCONNECT));
                BrowserHost.showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            } finally {
                return responseVS;
            }
        }
    }

}