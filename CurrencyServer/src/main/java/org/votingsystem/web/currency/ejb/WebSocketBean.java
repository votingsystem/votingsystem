package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
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
    @Inject CMSBean cmsBean;

    @Transactional
    public void processRequest(SocketMessageDto messageDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        MessageCMS messageCMS = null;
        SocketMessageDto signedMessageDto = null;
        UserVS signer = null;
        SocketMessageDto responseDto = null;
        switch(messageDto.getOperation()) {
            //Device (authenticated or not) sends message knowing target device id. Target device must be authenticated.
            case MSG_TO_DEVICE_BY_TARGET_DEVICE_ID:
                if(SessionVSManager.getInstance().sendMessageByTargetDeviceId(messageDto)) {//message send OK
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null)));
                } else messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                        messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                                messages.get("webSocketDeviceSessionNotFoundErrorMsg", messageDto.getDeviceToId()))));
                break;
            //Authenticated device sends message knowing target device session id. Target device can be authenticated or not.
            case MSG_TO_DEVICE_BY_TARGET_SESSION_ID:
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
                        messageDto.setSessionId(messageDto.getSession().getId());
                        callerSession.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(messageDto));
                        messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                                messageDto.getServerResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null)));
                    }
                }
                break;
            case INIT_BROWSER_SESSION:
                DeviceVS browserDevice = new DeviceVS(SessionVSManager.getInstance().getAndIncrementBrowserDeviceId())
                        .setType(DeviceVS.Type.BROWSER);
                messageDto.getSession().getUserProperties().put("deviceVS", browserDevice);
                SessionVSManager.getInstance().putBrowserDevice(messageDto.getSession());
                SocketMessageDto response = messageDto.getServerResponse(ResponseVS.SC_OK, null)
                        .setDeviceId(browserDevice.getId().toString()).setMessageType(TypeVS.INIT_BROWSER_SESSION);
                messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(response));
                break;
            case INIT_REMOTE_SIGNED_BROWSER_SESSION:
                break;
            case INIT_REMOTE_SIGNED_SESSION:
                messageCMS = cmsBean.validateCMS(messageDto.getCMS(), null).getMessageCMS();
                signer = messageCMS.getUserVS();
                signedMessageDto = messageCMS.getSignedContent(SocketMessageDto.class);
                Session remoteSession = SessionVSManager.getInstance().getNotAuthenticatedSession(signedMessageDto.getSessionId());
                SessionVSManager.getInstance().putAuthenticatedDevice(remoteSession, signer);
                remoteSession.getUserProperties().put("remote", true);
                responseDto = messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_INIT_OK, null)
                        .setSessionId(remoteSession.getId()).setMessageType(TypeVS.INIT_REMOTE_SIGNED_SESSION);
                responseDto.setConnectedDevice(DeviceVSDto.INIT_AUTHENTICATED_SESSION(signer));
                remoteSession.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(responseDto));
                dao.getEM().merge(messageCMS.setType(TypeVS.WEB_SOCKET_INIT));
                break;
            case INIT_SIGNED_SESSION:
                messageCMS = cmsBean.validateCMS(messageDto.getCMS(), null).getMessageCMS();
                signer = messageCMS.getUserVS();
                if(CertificateVS.Type.USER_ID_CARD != signer.getCertificateVS().getType())
                    throw new ExceptionVS("ERROR - ID_CARD signature required");
                SocketMessageDto dto = messageCMS.getSignedContent(SocketMessageDto.class);
                Query query = dao.getEM().createQuery("select d from DeviceVS d where d.userVS.nif =:nif and d.deviceId =:deviceId")
                        .setParameter("nif", signer.getNif()).setParameter("deviceId", dto.getDeviceId());
                DeviceVS deviceVS = dao.getSingleResult(DeviceVS.class, query);
                if(deviceVS != null) {
                    signer.setDeviceVS(deviceVS);
                    messageDto.getSession().getUserProperties().put("remote", false);
                    SessionVSManager.getInstance().putAuthenticatedDevice(messageDto.getSession(), signer);
                    responseDto = messageDto.getServerResponse(
                            ResponseVS.SC_WS_CONNECTION_INIT_OK, null).setMessageType(TypeVS.INIT_SIGNED_SESSION);
                    query = dao.getEM().createQuery("select t from UserVSToken t where t.userVS =:userVS and t.state =:state")
                            .setParameter("userVS", signer).setParameter("state", UserVSToken.State.OK);
                    UserVSToken token = dao.getSingleResult(UserVSToken.class, query);
                    if(token != null) {
                        byte[] userToken = cmsBean.decryptCMS(token.getToken());
                        responseDto.setMessage(new String(userToken));
                    }
                    responseDto.setConnectedDevice(DeviceVSDto.INIT_AUTHENTICATED_SESSION(signer));
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(responseDto));
                    dao.getEM().merge(messageCMS.setType(TypeVS.WEB_SOCKET_INIT));
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