package org.votingsystem.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.client.Browser;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.dialog.CertNotFoundDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.*;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.*;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.throwable.ExceptionVS;
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

import static org.votingsystem.client.Browser.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketAuthenticatedService extends Service<ResponseVS> implements PasswordDialog.Listener {

    private static Logger log = Logger.getLogger(WebSocketAuthenticatedService.class.getSimpleName());

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private static WebSocketAuthenticatedService instance;
    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private ActorVS targetServer;
    private Session session;
    private UserVS userVS;
    private String connectionMessage = null;

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
        } else log.info("settings for INSECURE connection");
    }

    public static WebSocketAuthenticatedService getInstance() {
        try {
            if(ContextVS.getInstance().getCurrencyServer() != null) {
                if(instance == null) instance =  new WebSocketAuthenticatedService(ContextVS.getInstance().
                        getVotingSystemSSLCerts(), ContextVS.getInstance().getCurrencyServer());
            } else Utils.checkServer(VotingSystemApp.getInstance().getCurrencyServerURL());
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
        return instance;
    }

    @Override
    public void setPassword(TypeVS passwordType, char[] password) {
        switch (passwordType) {
            case WEB_SOCKET_INIT:
                if(password == null) {
                    broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
                } else connect(password);
                break;
        }
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

        @OnOpen public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText(connectionMessage);
            WebSocketAuthenticatedService.this.session = session;
        }

        @OnClose public void onClose(Session session, CloseReason closeReason) {
            broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
            BrowserSessionService.getInstance().setIsConnected(false);
            SocketMessageDto socketMessageDto = new SocketMessageDto();
            socketMessageDto.setOperation(TypeVS.DISCONNECT);
            EventBusService.getInstance().post(socketMessageDto);
        }

        @OnMessage public void onMessage(String message) {
            try {
                consumeMessage(JSON.getMapper().readValue(message, SocketMessageDto.class));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }


    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
    }
    class WebSocketTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            try {
                log.info("WebSocketTask - Connecting to " + targetServer.getWebSocketURL() + " ...");
                client.connectToServer(new WSEndpoint(), URI.create(targetServer.getWebSocketURL()));
            }catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return null;
        }
    }

    private void connect(char[] password) {
        ProgressDialog.showDialog(new InitValidatedSessionTask(password, targetServer), null);
    }

    public void setConnectionEnabled(boolean isConnectionEnabled){
        if(BrowserSessionService.getInstance().getCryptoToken() == null) {
            CertNotFoundDialog.showDialog();
            return;
        }
        if(isConnectionEnabled) {
            if(CryptoTokenVS.MOBILE != BrowserSessionService.getCryptoTokenType()) {
                PasswordDialog.showWithoutPasswordConfirm(TypeVS.WEB_SOCKET_INIT, this,
                        ContextVS.getMessage("initAuthenticatedSessionPasswordMsg"));
            } else if(CryptoTokenVS.MOBILE == BrowserSessionService.getCryptoTokenType()) {
                connect(null);
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
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("connectionRequiredForServiceErrorMsg"));
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
            ResultListDto<DeviceVSDto> resultListDto = HttpHelper.getInstance().getData(
                    new TypeReference<ResultListDto<DeviceVSDto>>(){}, ((CurrencyServer) operationVS.getTargetServer())
                    .getDeviceVSConnectedServiceURL(operationVS.getNif()),  MediaTypeVS.JSON);
            boolean isMessageDelivered = false;
            for (DeviceVSDto deviceVSDto : resultListDto.getResultList()) {
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
        } throw new ExceptionVS(ContextVS.getMessage("authenticatedWebSocketConnectionRequiredMsg"));
    }

    private void consumeMessage(final SocketMessageDto socketMsg) {
        try {
            WebSocketSession socketSession = ContextVS.getInstance().getWSSession(socketMsg.getUUID());
            if(ResponseVS.SC_ERROR == socketMsg.getStatusCode()) {
                showMessage(socketMsg.getStatusCode(), socketMsg.getMessage());
                return;
            }
            if(socketSession == null && socketMsg.isEncrypted()) {
                BrowserSessionService.decryptMessage(socketMsg);
                socketSession = new WebSocketSession(socketMsg);
                ContextVS.getInstance().putWSSession(socketMsg.getUUID(), socketSession);
            } else if(socketSession != null && socketMsg.isEncrypted()) {
                socketMsg.decryptMessage(socketSession.getAESParams());
            }
            socketMsg.setWebSocketSession(socketSession);
            log.info("consumeMessage - type: " + socketMsg.getOperation() + " - MessageType: " +
                    socketMsg.getMessageType() + " - status: " + socketMsg.getStatusCode());
            switch(socketMsg.getOperation()) {
                case CURRENCY_WALLET_CHANGE:
                case MESSAGEVS:
                case MESSAGEVS_TO_DEVICE:
                    InboxService.getInstance().newMessage(new InboxMessage(socketMsg));
                    break;
                case MESSAGEVS_FROM_VS:
                    if(socketSession != null) {
                        socketMsg.setOperation(socketSession.getTypeVS());
                        switch(socketSession.getTypeVS()) {
                            case INIT_SIGNED_SESSION:
                                BrowserSessionService.getInstance().initAuthenticatedSession(socketMsg, userVS);
                                break;
                            default:
                                log.log(Level.SEVERE, "MESSAGEVS_FROM_VS - pong - TypeVS: " + socketSession.getTypeVS());
                        }
                    }
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
                    } else showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                    break;
                case QR_MESSAGE_INFO:
                    //the payer has read our QR code and ask for details
                    if(ResponseVS.SC_ERROR != socketMsg.getStatusCode()) {
                        SocketMessageDto msgDto = null;
                        Long deviceFromId = BrowserSessionService.getInstance().getConnectedDevice().getId();
                        try {
                            QRMessageDto<TransactionVSDto> qrDto = VotingSystemApp.getInstance().getQRMessage(
                                    socketMsg.getMessage());
                            qrDto.setHashCertVS(socketMsg.getContent().getHashCertVS());
                            TransactionVSDto transactionDto = qrDto.getData();
                            Currency currency =  new  Currency(
                                    ContextVS.getInstance().getCurrencyServer().getServerURL(),
                                    transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                                    transactionDto.isTimeLimited(), qrDto.getHashCertVS(),
                                    new TagVS(transactionDto.getTagName()));
                            qrDto.setCurrency(currency);
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
                                    ex.getMessage(), deviceFromId, TypeVS.QR_MESSAGE_INFO);
                        } finally {
                            session.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(msgDto));
                        }
                    } else showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
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
                                Wallet.saveToPlainWallet(Arrays.asList(currency));
                            }
                            SocketMessageDto response = socketMsg.getResponse(ResponseVS.SC_OK, null,
                                    BrowserSessionService.getInstance().getConnectedDevice().getId(),
                                    TypeVS.OPERATION_FINISHED);
                            sendMessage(JSON.getMapper().writeValueAsString(response));
                            VotingSystemApp.getInstance().removeQRMessage(qrDto.getUUID());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                        }
                    } else showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                    break;
                case OPERATION_CANCELED:
                    socketMsg.setOperation(socketSession.getTypeVS());
                    socketMsg.setStatusCode(ResponseVS.SC_CANCELED);
                    consumeMessage(socketMsg);
                    break;
                default:
                    log.info("unprocessed socketMsg: " + socketMsg.getOperation());
            }
            EventBusService.getInstance().post(socketMsg);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void broadcastConnectionStatus(SocketMessageDto.ConnectionStatus status) {
        if(session == null) log.info("broadcastConnectionStatus - status: " + status.toString());
        else log.info("broadcastConnectionStatus - status: " + status.toString() + " - session: " + session.getId());
        switch (status) {
            case CLOSED:
                Browser.getInstance().runJSCommand(CoreSignal.getWebSocketCoreSignalJSCommand(
                        null, SocketMessageDto.ConnectionStatus.CLOSED));
                break;
            case OPEN:
                Browser.getInstance().runJSCommand(CoreSignal.getWebSocketCoreSignalJSCommand(
                        null, SocketMessageDto.ConnectionStatus.OPEN));
                break;
        }
    }

    public class InitValidatedSessionTask extends Task<ResponseVS> {

        private char[] password;
        private ActorVS targetServer;

        public InitValidatedSessionTask (char[] password, ActorVS targetServer) {
            this.password = password;
            this.targetServer = targetServer;
        }

        @Override protected ResponseVS call() throws Exception {
            SocketMessageDto dto = SocketMessageDto.INIT_SESSION_REQUEST(
                    BrowserSessionService.getInstance().getCryptoToken().getDeviceId());
            ResponseVS responseVS = null;
            try {
                if(BrowserSessionService.getCryptoTokenType() == CryptoTokenVS.MOBILE) {
                    updateMessage(ContextVS.getMessage("checkDeviceVSCryptoTokenMsg"));
                } else updateMessage(ContextVS.getMessage("connectionMsg"));
                SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, targetServer.getName(),
                        JSON.getMapper().writeValueAsString(dto), password,
                        ContextVS.getMessage("initAuthenticatedSessionMsgSubject"));
                userVS = smimeMessage.getSigner();
                connectionMessage = JSON.getMapper().writeValueAsString(dto.setSMIME(smimeMessage));
                PlatformImpl.runLater(() -> WebSocketAuthenticatedService.this.restart());
                responseVS = ResponseVS.OK().setSMIME(smimeMessage);

            } catch(InterruptedException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
                showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            }
            return responseVS;
        }
    }

}