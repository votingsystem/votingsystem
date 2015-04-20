package org.votingsystem.web.currency.websocket;


import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.SessionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.ejb.TransactionVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@ServerEndpoint(value = "/websocket/service", configurator = SocketConfigurator.class)
public class SocketEndpointVS {

    private static Logger log = Logger.getLogger(SocketEndpointVS.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SignatureBean signatureBean;
    @Inject MessagesBean messages;

    @OnMessage
    public void onTextMessage(String msg, Session session) {
        SocketMessageDto messageDto = null;
        try {
            if (session.isOpen()) {
                messageDto = JSON.getMapper().readValue(msg, SocketMessageDto.class);
                SessionVS sessionVS = SessionVSManager.getInstance().getAuthenticatedSession(session);
                messageDto.setSession(session, sessionVS);
                processRequest(messageDto);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            String message = ex.getMessage();
            if(message == null) message = messages.get("socketRequestErrorMsg");
            try {
                if(messageDto != null) SessionVSManager.getInstance().sendMessage(messageDto.getErrorResponse(message));
                session.close();
            } catch (Exception ex1) { // Ignore }
            }
        }
    }


    //TODO QUEUE
    public void processRequest(SocketMessageDto messageDto) throws Exception {
        switch(messageDto.getOperation()) {
            case MESSAGEVS_TO_DEVICE:
                if(SessionVSManager.getInstance().sendMessageToDevice(messageDto)) {//message send OK
                    SessionVSManager.getInstance().sendMessage(messageDto.getResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null));
                } else SessionVSManager.getInstance().sendMessage(messageDto.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND, null));
                break;
            case MESSAGEVS_FROM_DEVICE:
                if(messageDto.getSessionVS() == null) SessionVSManager.getInstance().sendMessage(
                        messageDto.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                                messages.get("userNotAuthenticatedErrorMsg")));
                Session callerSession = SessionVSManager.getInstance().getAuthenticatedSession(messageDto.getSessionId());
                if(callerSession == null) callerSession = SessionVSManager.getInstance().getSession(messageDto.getSessionId());
                if(callerSession == null) {
                    SessionVSManager.getInstance().sendMessage(messageDto.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                            messages.get("messagevsSignRequestorNotFound")));
                } else {
                    callerSession.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(messageDto));
                    SessionVSManager.getInstance().sendMessage(messageDto.getResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null));
                }
                break;
            case INIT_VALIDATED_SESSION:
                MessageSMIME messageSMIME = signatureBean.validateSMIME(messageDto.getSmime(), null).getMessageSMIME();
                UserVS signer = messageSMIME.getUserVS();
                if(signer.getDeviceVS() != null) {
                    //check if accessing from one device and signing from another
                    if(!signer.getDeviceVS().getDeviceId().equals(messageDto.getDeviceFromId())) signer.setDeviceVS(null);
                }
                if(signer.getDeviceVS() == null && messageDto.getDeviceFromId() != null){
                    Query query = dao.getEM().createNamedQuery("findDeviceByUserAndDeviceId")
                            .setParameter("deviceId", messageDto.getDeviceFromId()).setParameter("userVS", signer);
                    DeviceVS deviceVS = dao.getSingleResult(DeviceVS.class, query);
                    signer.setDeviceVS(deviceVS);
                }
                if(signer.getDeviceVS() != null) {
                    SessionVSManager.getInstance().putAuthenticatedDevice(messageDto.getSession(), signer);
                    SessionVSManager.getInstance().sendMessage(messageDto.getResponse(ResponseVS.SC_WS_CONNECTION_INIT_OK, null));
                    //signer.getDeviceVS().getId()
                    dao.getEM().merge(messageSMIME.setType(TypeVS.WEB_SOCKET_INIT));
                } else SessionVSManager.getInstance().sendMessage(messageDto.getResponse(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                        messages.get("certWithoutDeviceVSInfoErrorMsg")));
                break;
            case WEB_SOCKET_BAN_SESSION:
                //talks
                break;
            default: throw new ExceptionVS("unknownSocketOperationErrorMsg: " + messageDto.getOperation());
        }
    }
    
    /*@OnMessage public void onBinaryMessage(Session session, ByteBuffer bb, boolean last) {
        try {
            if (session.isOpen()) {
                webSocketBean.onBinaryMessage(session, bb, last);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
            try {
                session.close();
            } catch (IOException ex1) { // Ignore
            }
        }
    }*/

    @OnError public void onError(Throwable t) {
        log.log(Level.SEVERE, t.getMessage(), t);
    }

    @OnOpen public void onOpen(Session session, EndpointConfig config) {
        SessionVSManager.getInstance().put(session);
    }


    @OnClose public void onClose(Session session, CloseReason closeReason) {
        //log.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
        try {
            SessionVSManager.getInstance().remove(session);
        } catch (Exception ex) {
            log.log(Level.SEVERE,"EXCEPTION CLOSING CONNECTION: " + ex.getMessage());
        }
    }


    /*@OnMessage public void echoPongMessage(PongMessage pm) {
        // Ignored
    }*/
}