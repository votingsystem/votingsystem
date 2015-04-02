package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.util.SocketServiceRequest;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class WebSocketBean {

    private static Logger log = Logger.getLogger(WebSocketBean.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SignatureBean signatureBean;
    @Inject MessagesBean messages;

    public void onTextMessage(Session session, String msg , boolean last) throws JsonProcessingException, ExceptionVS {
        try {
            SocketServiceRequest request = new SocketServiceRequest(session, msg, last);
            processRequest(request);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            String message = ex.getMessage();
            if(message == null) message = messages.get("socketRequestErrorMsg");
            processResponse(SocketServiceRequest.getResponse(ResponseVS.SC_ERROR, message, session, msg));
        }
    }

    public void onBinaryMessage(Session session, ByteBuffer byteBuffer, boolean last) {
        log.info("onBinaryMessage");
        //session.getBasicRemote().sendBinary(byteBuffer, last);
    }

    public void onOpen(Session session) {
        SessionVSManager.getInstance().put(session);
    }

    public void onClose(Session session, CloseReason closeReason) {
        log.info("onClose - session id:" + session.getId() + " - closeReason:" + closeReason);
        SessionVSManager.getInstance().remove(session);
    }

    //TODO QUEUE
    public void processRequest(SocketServiceRequest request) throws Exception {
        switch(request.getOperation()) {
            case MESSAGEVS_TO_DEVICE:
                if(SessionVSManager.getInstance().sendMessageToDevice(
                        request.getDeviceToId(), request.getJsonData().toString())) {//message send OK
                    processResponse(request.getResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null));
                } else processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND, null, null));
                break;
            case MESSAGEVS_FROM_DEVICE:
                if(request.getSessionVS() == null) processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                        messages.get("userNotAuthenticatedErrorMsg")));
                Session callerSession = SessionVSManager.getInstance().getAuthenticatedSession(request.getSessionId());
                if(callerSession == null) callerSession = SessionVSManager.getInstance().getSession(request.getSessionId());
                if(callerSession == null) {
                    processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND, messages.get(
                            "messagevsSignRequestorNotFound")));
                } else {
                    callerSession.getBasicRemote().sendText(request.getJsonData().toString());
                    processResponse(request.getResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null));
                }
                break;
            case INIT_VALIDATED_SESSION:
                MessageSMIME messageSMIME = signatureBean.processSMIMERequest(request.getSMIME(), null);
                UserVS signer = messageSMIME.getUserVS();
                if(signer.getDeviceVS() != null) {
                    //check if accessing from one device and signing from another
                    if(!signer.getDeviceVS().getDeviceId().equals(request.getDeviceFromId())) signer.setDeviceVS(null);
                }
                if(signer.getDeviceVS() == null && request.getDeviceFromId() > 0){
                    Query query = dao.getEM().createNamedQuery("findDeviceByUserAndDeviceId")
                            .setParameter("deviceId", request.getDeviceFromId()).setParameter("userVS", signer);
                    DeviceVS deviceVS = dao.getSingleResult(DeviceVS.class, query);
                    signer.setDeviceVS(deviceVS);
                }
                if(signer.getDeviceVS() != null) {
                    SessionVSManager.getInstance().putAuthenticatedDevice(request.getSession(), signer);
                    processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_INIT_OK, null,
                            signer.getDeviceVS().getId()));
                    dao.getEM().merge(messageSMIME.setType(TypeVS.WEB_SOCKET_INIT));
                } else processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                        messages.get("certWithoutDeviceVSInfoErrorMsg"), null));
                break;
            case WEB_SOCKET_BAN_SESSION:
                //talks
                break;
            default: throw new ExceptionVS("unknownSocketOperationErrorMsg: " + request.getOperation().toString());
        }
    }

    public void broadcast(Map messageMap) throws JsonProcessingException {
        SessionVSManager.getInstance().broadcast(new ObjectMapper().writeValueAsString(messageMap));
    }

    public void broadcastToAuthenticatedUsers(Map messageMap) throws JsonProcessingException {
        SessionVSManager.getInstance().broadcastToAuthenticatedUsers(new ObjectMapper().writeValueAsString(messageMap));
    }

    public ResponseVS broadcastList(Map messageMap, Set<String> listeners) throws JsonProcessingException {
        return SessionVSManager.getInstance().broadcastList(new ObjectMapper().writeValueAsString(messageMap), listeners);
    }

    public ResponseVS broadcastAuthenticatedList(Map messageMap, Set<String> listeners) throws JsonProcessingException {
        return SessionVSManager.getInstance().broadcastAuthenticatedList(
                new ObjectMapper().writeValueAsString(messageMap), listeners);
    }

    public void processResponse(Map messageMap) throws JsonProcessingException, ExceptionVS {
        log.info("processResponse - messageJSON:" + messageMap);
        SessionVSManager.getInstance().sendMessage(((String)messageMap.get("sessionId")),  
                new ObjectMapper().writeValueAsString(messageMap));
    }

}