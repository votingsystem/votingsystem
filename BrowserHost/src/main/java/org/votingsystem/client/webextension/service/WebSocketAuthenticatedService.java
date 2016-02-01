package org.votingsystem.client.webextension.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import javafx.concurrent.Task;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.dialog.CertNotFoundDialog;
import org.votingsystem.client.webextension.dialog.PasswordDialog;
import org.votingsystem.client.webextension.dialog.ProgressDialog;
import org.votingsystem.client.webextension.util.InboxMessage;
import org.votingsystem.dto.*;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.*;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.service.EventBusService;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.KeyStoreExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.currency.Wallet;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketAuthenticatedService {

    private static Logger log = Logger.getLogger(WebSocketAuthenticatedService.class.getSimpleName());

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private static WebSocketAuthenticatedService instance;
    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private ActorVS targetServer;
    private Session session;
    private UserVS userVS;

    private PasswordDialog.Listener websocketInitPasswordListener = new PasswordDialog.Listener() {
        @Override public void processPassword(char[] password) {
            if(password == null) broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
            else ProgressDialog.show(new InitSessionTask(password, targetServer), null);
        }
    };

    private WebSocketAuthenticatedService(Collection<X509Certificate> sslServerCertCollection, ActorVS targetServer) {
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
                // Grizzly ssl configuration
                SSLContextConfigurator sslContext = new SSLContextConfigurator();
                sslContext.setTrustStoreType("PKCS12");
                sslContext.setTrustStoreBytes(p12KeyStoreBytes);
                sslContext.setTrustStorePass(keyStorePassw);
                SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContext, true, false, false);
                client.getProperties().put(SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else log.info("settings for WebSocket - INSECURE - connection");
    }

    public static WebSocketAuthenticatedService getInstance() {
        try {
            if(ContextVS.getInstance().getCurrencyServer() != null) {
                if(instance == null) instance =  new WebSocketAuthenticatedService(ContextVS.getInstance().
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
            session.getBasicRemote().sendText(connectionMessage);
            WebSocketAuthenticatedService.this.session = session;
        }

        @OnClose public void onClose(Session session, CloseReason closeReason) {
            broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
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
        if(BrowserSessionService.getInstance().getCryptoToken() == null) {
            CertNotFoundDialog.showDialog(ContextVS.getMessage("cryptoTokenNotFoundErrorMsg"));
            return;
        }
        if(isConnectionEnabled) {
            if(CryptoTokenVS.MOBILE != BrowserSessionService.getCryptoTokenType()) {
                PasswordDialog.showWithoutPasswordConfirm(websocketInitPasswordListener,
                        ContextVS.getMessage("initAuthenticatedSessionPasswordMsg"));
            } else if(CryptoTokenVS.MOBILE == BrowserSessionService.getCryptoTokenType()) {
                ProgressDialog.show(new InitSessionTask(null, targetServer), null);
            }
        } else  {
            if(session != null && session.isOpen()) {
                try {session.close();}
                catch(Exception ex) {log.log(Level.SEVERE, ex.getMessage(), ex);}
            } else broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
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
                ResultListDto<DeviceVSDto> resultListDto = HttpHelper.getInstance().getData(
                        new TypeReference<ResultListDto<DeviceVSDto>>(){}, ((CurrencyServer) operationVS.getTargetServer())
                                .getDeviceVSConnectedServiceURL(operationVS.getNif()),  MediaTypeVS.JSON);
                userDevices = resultListDto.getResultList();
            }
            boolean isMessageDelivered = false;
            for (DeviceVSDto deviceVSDto : userDevices) {
                DeviceVS deviceVS = deviceVSDto.getDeviceVS();
                if(!BrowserSessionService.getInstance().getCryptoToken().getDeviceId().equals(deviceVS.getDeviceId())) {
                    SocketMessageDto messageDto = SocketMessageDto.getMessageVSToDevice(BrowserSessionService.getInstance().getUserVS(),
                            deviceVS, operationVS.getNif(), operationVS.getMessage());
                    sendMessage(JSON.getMapper().writeValueAsString(messageDto));
                    isMessageDelivered = true;
                }
            }
            if(isMessageDelivered) return new ResponseVS(ResponseVS.SC_OK);
            else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage(
                    "uservsWithoutDevicesConnectedMsg", operationVS.getNif()));
        } return ResponseVS.ERROR(ContextVS.getMessage("authenticatedWebSocketConnectionRequiredMsg"));
    }

    private void consumeMessage(final SocketMessageDto socketMsg) {
        if(ResponseVS.SC_ERROR == socketMsg.getStatusCode()) {
            BrowserHost.showMessage(socketMsg.getStatusCode(), socketMsg.getMessage());
            return;
        }
        try {
            WebSocketSession socketSession = ContextVS.getInstance().getWSSession(socketMsg.getUUID());
            switch(socketMsg.getOperation()) { //check messages from system
                case MESSAGEVS_FROM_VS:
                    if(socketSession != null) {
                        log.info("MESSAGEVS_FROM_VS - TypeVS: " + socketSession.getTypeVS());
                        socketMsg.setOperation(socketSession.getTypeVS());
                        switch(socketSession.getTypeVS()) {
                            case INIT_SIGNED_SESSION:
                                BrowserSessionService.getInstance().initAuthenticatedSession(socketMsg, userVS);
                                break;
                        }
                    } else {
                        log.info("MESSAGEVS_FROM_VS - MessageType: " + socketMsg.getMessageType());
                        switch (socketMsg.getMessageType()) {
                            case TRANSACTIONVS_INFO:
                                break;
                            default: log.info("MESSAGEVS_FROM_VS - UNPROCESSED - MessageType: " + socketMsg.getMessageType());

                        }
                    }
                    if(ResponseVS.SC_WS_CONNECTION_NOT_FOUND == socketMsg.getStatusCode()) {
                        BrowserHost.showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                    }
                    EventBusService.getInstance().post(socketMsg);
                    return;
            }
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
                case MESSAGEVS_TO_DEVICE:
                    InboxService.getInstance().newMessage(new InboxMessage(socketMsg));
                    break;
                case MESSAGEVS_FROM_DEVICE:
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
                case TRANSACTIONVS_INFO:
                    //response after asking for the details of a QR code
                    if(ResponseVS.SC_ERROR != socketMsg.getStatusCode()) {
                        TransactionVSDto dto = socketMsg.getMessage(TransactionVSDto.class);
                        //dto.setSocketMessageDto(socketMsg);
                        throw new ExceptionVS("TODO - missing payment form");
                    } else BrowserHost.showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
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
                            SMIMEMessage simeMessage = BrowserSessionService.getSMIME(null, targetServer.getName(),
                                    new String(currency.getCertificationRequest().getCsrPEM()), null,
                                    ContextVS.getMessage("currencyChangeSubject"));
                            transactionDto.setMessageSMIME(Base64.getEncoder().encodeToString(simeMessage.getBytes()));
                            msgDto = socketMsg.getResponse(ResponseVS.SC_OK,JSON.getMapper().writeValueAsString(transactionDto),
                                    deviceFromId, simeMessage, TypeVS.TRANSACTIONVS_INFO);
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
                            SMIMEMessage smimeMessage = socketMsg.getSMIME();
                            QRMessageDto<TransactionVSDto> qrDto =
                                    (QRMessageDto<TransactionVSDto>) socketSession.getData();
                            TypeVS typeVS = TypeVS.valueOf(smimeMessage.getHeader("TypeVS")[0]);
                            if(TypeVS.CURRENCY_CHANGE == typeVS) {
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

    public void broadcastConnectionStatus(SocketMessageDto.ConnectionStatus status) {
        if(session == null) log.info("broadcastConnectionStatus - status: " + status.toString());
        else log.info("broadcastConnectionStatus - status: " + status.toString() + " - session: " + session.getId());
        switch (status) {
            case CLOSED:
                BrowserHost.sendMessageToBrowser(MessageDto.WEB_SOCKET(ResponseVS.SC_CANCELED, null));
                EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.DISCONNECT));
                break;
            case OPEN:
                BrowserHost.sendMessageToBrowser(MessageDto.WEB_SOCKET(ResponseVS.SC_OK, null));
                EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.CONNECT));
                break;
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
                        BrowserSessionService.getInstance().getCryptoToken().getDeviceId());
                if(BrowserSessionService.getCryptoTokenType() == CryptoTokenVS.MOBILE) {
                    updateMessage(ContextVS.getMessage("checkDeviceVSCryptoTokenMsg"));
                } else updateMessage(ContextVS.getMessage("connectionMsg"));
                SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, targetServer.getName(),
                        JSON.getMapper().writeValueAsString(dto), password,
                        ContextVS.getMessage("initAuthenticatedSessionMsgSubject"));
                userVS = smimeMessage.getSigner();
                String connectionMessage = JSON.getMapper().writeValueAsString(dto.setSMIME(smimeMessage));
                client.connectToServer(new WSEndpoint(connectionMessage), URI.create(targetServer.getWebSocketURL()));
                responseVS = ResponseVS.OK().setSMIME(smimeMessage);
            } catch (KeyStoreExceptionVS ex) {
                CertNotFoundDialog.showDialog(ex.getMessage());
            } catch(InterruptedException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
                BrowserHost.showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            } finally {
                return responseVS;
            }
        }
    }

}