package org.votingsystem.web.currency.ejb;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.RemoteSignedSessionDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.currency.websocket.SessionManager;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class WebSocketBean {

    private static Logger log = Logger.getLogger(WebSocketBean.class.toString());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionBean transactionBean;
    @Inject CMSBean cmsBean;

    @Transactional
    public void processRequest(SocketMessageDto messageDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSMessage cmsMessage = null;
        SocketMessageDto signedMessageDto = null;
        User signer = null;
        SocketMessageDto responseDto = null;
        Device browserDevice = null;
        switch(messageDto.getOperation()) {
            //Device (authenticated or not) sends message knowing target device id. Target device must be authenticated.
            case MSG_TO_DEVICE_BY_TARGET_DEVICE_ID:
                if(SessionManager.getInstance().sendMessageByTargetDeviceId(messageDto)) {//message send OK
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null)));
                } else messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                        messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                                messages.get("webSocketDeviceSessionNotFoundErrorMsg"))));
                break;
            //Authenticated device sends message knowing target device session id. Target device can be authenticated or not.
            case MSG_TO_DEVICE_BY_TARGET_SESSION_ID:
                if(messageDto.getSession().getUserProperties().get("user") == null) {
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                            messages.get("userNotAuthenticatedErrorMsg"))));
                } else {
                    Session callerSession = SessionManager.getInstance().getAuthenticatedSession(messageDto.getSessionId());
                    if(callerSession == null) callerSession = SessionManager.getInstance()
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
                browserDevice = new Device(SessionManager.getInstance().getAndIncrementBrowserDeviceId())
                        .setType(Device.Type.BROWSER).setDeviceId(UUID.randomUUID().toString());
                messageDto.getSession().getUserProperties().put("device", browserDevice);
                SessionManager.getInstance().putBrowserDevice(messageDto.getSession(), browserDevice);
                SocketMessageDto response = messageDto.getServerResponse(ResponseVS.SC_OK, null)
                        .setMessage(browserDevice.getId().toString()).setMessageType(TypeVS.INIT_BROWSER_SESSION);
                messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(response));
                break;
            case CLOSE_SESSION:
                cmsMessage = cmsBean.validateCMS(messageDto.getCMS(), null).getCmsMessage();
                MessageDto msgDto = cmsMessage.getSignedContent(MessageDto.class);
                if(TypeVS.CLOSE_SESSION == TypeVS.valueOf(msgDto.getOperation())) {
                    messageDto.getSession().close();
                }
                break;
            case INIT_REMOTE_SIGNED_SESSION:
                cmsMessage = cmsBean.validateCMS(messageDto.getCMS(), null).getCmsMessage();
                signer = cmsMessage.getUser();
                RemoteSignedSessionDto remoteSignedSessionDto = cmsMessage.getSignedContent(RemoteSignedSessionDto.class);
                PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(
                        remoteSignedSessionDto.getCsr().getBytes());
                User userFromCSR = User.getUser(csr.getSubject());
                if(!userFromCSR.checkUserFromCSR(signer.getX509Certificate())) {
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_ERROR, messages.get("remoteCSRErrorMsg",
                            signer.getNameAndId(), userFromCSR.getNameAndId()))));
                } else {
                    Session remoteSession = SessionManager.getInstance().getSession(
                            remoteSignedSessionDto.getSessionId());
                    if(remoteSession == null) {
                        messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                                messageDto.getServerResponse(ResponseVS.SC_ERROR, messages.get(
                                "webSocketDeviceSessionNotFoundErrorMsg",  signer.getNameAndId(),
                                userFromCSR.getNameAndId()))));
                    } else {
                        browserDevice = (Device) remoteSession.getUserProperties().get("device");
                        try {
                            Certificate certificate = cmsBean.signCSRForBrowserSession(csr, cmsMessage, browserDevice);
                            browserDevice.setX509Certificate(certificate.getX509Certificate());
                            if(browserDevice.getUser() != null) {
                                //device already in db
                                browserDevice.setCertificate(certificate).setUser(signer);
                                browserDevice = dao.merge(browserDevice);
                            } else {
                                browserDevice.setId(null);
                                browserDevice.setDeviceId(UUID.randomUUID().toString());
                                browserDevice.setCertificate(certificate).setUser(signer);
                                browserDevice = dao.persist(browserDevice);
                            }
                            SessionManager.getInstance().putAuthenticatedDevice(remoteSession, signer, browserDevice);
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, ex.getMessage(), ex);
                        }
                        DeviceDto mobileDevice = new DeviceDto(signer.getDevice());
                        responseDto = messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_INIT_OK,
                                JSON.getMapper().writeValueAsString(mobileDevice))
                                .setSessionId(remoteSession.getId())
                                .setMessageType(TypeVS.INIT_REMOTE_SIGNED_SESSION);
                        responseDto.setConnectedDevice(DeviceDto.INIT_BROWSER_SESSION(signer, browserDevice));
                        remoteSession.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(responseDto));
                        dao.getEM().merge(cmsMessage.setType(TypeVS.WEB_SOCKET_INIT));
                    }
                }
                break;
            case INIT_SIGNED_SESSION:
                cmsMessage = cmsBean.validateCMS(messageDto.getCMS(), null).getCmsMessage();
                signer = cmsMessage.getUser();
                if(Certificate.Type.USER_ID_CARD != signer.getCertificate().getType())
                    throw new ExceptionVS("ERROR - ID_CARD signature required");
                SocketMessageDto dto = cmsMessage.getSignedContent(SocketMessageDto.class);
                Query query = dao.getEM().createQuery("select d from Device d where d.user.nif =:nif and d.deviceId =:deviceId")
                        .setParameter("nif", signer.getNif()).setParameter("deviceId", dto.getDeviceId());
                Device device = dao.getSingleResult(Device.class, query);
                if(device != null) {
                    signer.setDevice(device);
                    responseDto = messageDto.getServerResponse(
                            ResponseVS.SC_WS_CONNECTION_INIT_OK, null).setMessageType(TypeVS.INIT_SIGNED_SESSION);
                    query = dao.getEM().createQuery("select t from UserToken t where t.user =:user and t.state =:state")
                            .setParameter("user", signer).setParameter("state", UserToken.State.OK);
                    UserToken token = dao.getSingleResult(UserToken.class, query);
                    if(token != null) {
                        byte[] userToken = cmsBean.decryptCMS(token.getToken());
                        responseDto.setMessage(new String(userToken));
                    }
                    responseDto.setConnectedDevice(DeviceDto.INIT_SIGNED_SESSION(signer));
                    dao.getEM().merge(cmsMessage.setType(TypeVS.WEB_SOCKET_INIT));
                    SessionManager.getInstance().putAuthenticatedDevice(messageDto.getSession(), signer, device);
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(responseDto));
                } else {
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                            messages.get("certWithoutDeviceInfoErrorMsg"))));
                }
                break;
            case WEB_SOCKET_BAN_SESSION:
                //talks
                break;
            default: throw new ExceptionVS("unknownSocketOperationErrorMsg: " + messageDto.getOperation());
        }
    }

}