package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class WebSocketBean {

    private static Logger log = Logger.getLogger(WebSocketBean.class.toString());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SignatureBean signatureBean;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    @Transactional
    public void processRequest(SocketMessageDto messageDto) throws Exception {
        switch(messageDto.getOperation()) {
            case MESSAGEVS_TO_DEVICE:
                if(SessionVSManager.getInstance().sendMessageToDevice(messageDto)) {//message send OK
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null)));
                } else messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                        messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND, null)));
                break;
            case MESSAGEVS_FROM_DEVICE:
                if(messageDto.getSession().getUserProperties().get("userVS") == null) {
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                            messages.get("userNotAuthenticatedErrorMsg"))));
                } else {
                    Session callerSession = SessionVSManager.getInstance().getAuthenticatedSession(messageDto.getSessionId());
                    if(callerSession == null) callerSession = SessionVSManager.getInstance()
                            .getNotAuthenticatedSession(messageDto.getSessionId());
                    if(callerSession == null) {
                        messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                                messageDto.getServerResponse( ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                                messages.get("messagevsSignRequestorNotFound"))));
                    } else {
                        callerSession.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(messageDto));
                        messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                                messageDto.getServerResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null)));
                    }
                }
                break;
            case INIT_SIGNED_SESSION:
                MessageSMIME messageSMIME = signatureBean.validateSMIME(messageDto.getSMIME(), null).getMessageSMIME();
                UserVS signer = messageSMIME.getUserVS();
                if(signer.getDeviceVS() != null) {
                    //check if accessing from one device and signing from another
                    if(!signer.getDeviceVS().getDeviceId().equals(messageDto.getDeviceId())) signer.setDeviceVS(null);
                }
                if(signer.getDeviceVS() == null && messageDto.getDeviceId() != null){
                    Query query = dao.getEM().createNamedQuery("findDeviceByUserAndDeviceId")
                            .setParameter("deviceId", messageDto.getDeviceId()).setParameter("userVS", signer);
                    DeviceVS deviceVS = dao.getSingleResult(DeviceVS.class, query);
                    signer.setDeviceVS(deviceVS);
                }
                if(signer.getDeviceVS() != null) {
                    SessionVSManager.getInstance().putAuthenticatedDevice(messageDto.getSession(), signer);
                    SocketMessageDto responseDto = messageDto.getServerResponse(
                            ResponseVS.SC_WS_CONNECTION_INIT_OK, null);
                    responseDto.setConnectedDevice(new DeviceVSDto(signer.getDeviceVS()));
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(responseDto));
                    dao.getEM().merge(messageSMIME.setType(TypeVS.WEB_SOCKET_INIT));
                } else {
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                            messages.get("certWithoutDeviceVSInfoErrorMsg"))));
                }
                break;
            case WEB_SOCKET_BAN_SESSION:
                //talks
                break;
            default: throw new ExceptionVS("unknownSocketOperationErrorMsg: " + messageDto.getOperation());
        }
    }

}