package org.votingsystem.web.currency.ejb;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.dto.*;
import org.votingsystem.model.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.currency.util.HTTPSessionManager;
import org.votingsystem.web.currency.util.PrincipalVS;
import org.votingsystem.web.currency.websocket.SessionManager;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.http.HttpSession;
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
        User signer = null;
        SocketMessageDto responseDto = null;
        Device browserDevice = null;
        switch(messageDto.getOperation()) {
            //Device (authenticated or not) sends message knowing target device id. Target device must be authenticated.
            case MSG_TO_DEVICE:
                if(SessionManager.getInstance().sendMessageByTargetDeviceId(messageDto)) {//message send OK
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null)));
                } else messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                        messageDto.getServerResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                                messages.get("webSocketDeviceSessionNotFoundErrorMsg"))));
                break;
            case CLOSE_SESSION:
                cmsMessage = cmsBean.validateCMS(messageDto.getCMS(), null).getCmsMessage();
                MessageDto msgDto = cmsMessage.getSignedContent(MessageDto.class);
                if(TypeVS.CLOSE_SESSION == msgDto.getOperation()) {
                    messageDto.getSession().close();
                }
                break;
            case INIT_REMOTE_SIGNED_SESSION:
                cmsMessage = cmsBean.validateCMS(messageDto.getCMS(), null).getCmsMessage();
                signer = cmsMessage.getUser();
                RemoteSignedSessionDto remoteSignedSessionDto = cmsMessage.getSignedContent(RemoteSignedSessionDto.class);
                Session remoteSession = SessionManager.getInstance().getDeviceSession(
                        remoteSignedSessionDto.getDeviceId());
                if(remoteSession == null) {
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_ERROR, messages.get(
                            "webSocketDeviceSessionNotFoundErrorMsg"))));
                    return;
                }
                HttpSession remoteHttpSession = (HttpSession)remoteSession.getUserProperties().get(HttpSession.class.getName());
                if(remoteHttpSession == null) {
                    messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                            messageDto.getServerResponse(ResponseVS.SC_ERROR, messages.get(
                                    "webSocketDeviceSessionNotFoundErrorMsg"))));
                    return;
                }
                PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(
                        remoteSignedSessionDto.getCsr().getBytes());
                User userFromCSR = User.getUser(csr.getSubject());
                if(!userFromCSR.checkUserFromCSR(signer.getX509Certificate())) {
                        messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                        messageDto.getServerResponse(ResponseVS.SC_ERROR, messages.get("remoteCSRErrorMsg",
                        signer.getNameAndId(), userFromCSR.getNameAndId()))));
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
                            .setMessageType(TypeVS.INIT_REMOTE_SIGNED_SESSION);
                    responseDto.setConnectedDevice(DeviceDto.INIT_BROWSER_SESSION(signer, browserDevice));
                    remoteSession.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(responseDto));
                    dao.getEM().merge(cmsMessage.setType(TypeVS.WEB_SOCKET_INIT));
                    remoteHttpSession.setAttribute(PrincipalVS.USER_KEY, signer.setDevice(browserDevice));
                    remoteHttpSession.setAttribute(HTTPSessionManager.WEBSOCKET_SESSION_KEY, remoteSession);
                }
                break;
            case INIT_SIGNED_SESSION:
                cmsMessage = cmsBean.validateCMS(messageDto.getCMS(), null).getCmsMessage();
                signer = cmsMessage.getUser();
                if(Certificate.Type.USER_ID_CARD != signer.getCertificate().getType())
                    throw new ExceptionVS("ERROR - ID_CARD signature required");
                MessageDto dto = cmsMessage.getSignedContent(MessageDto.class);
                Query query = dao.getEM().createQuery("select d from Device d where d.user.nif =:nif and d.deviceId =:deviceId")
                        .setParameter("nif", signer.getNif()).setParameter("deviceId", dto.getDeviceId());
                Device device = dao.getSingleResult(Device.class, query);
                responseDto = messageDto.getServerResponse(
                        ResponseVS.SC_WS_CONNECTION_INIT_OK, null).setMessageType(TypeVS.INIT_SIGNED_SESSION);
                if(device != null) {
                    signer.setDevice(device);
                    query = dao.getEM().createQuery("select t from UserToken t where t.user =:user and t.state =:state")
                            .setParameter("user", signer).setParameter("state", UserToken.State.OK);
                    UserToken token = dao.getSingleResult(UserToken.class, query);
                    if(token != null) {
                        byte[] userToken = cmsBean.decryptCMS(token.getToken());
                        responseDto.setMessage(new String(userToken));
                    }
                } else {
                    device = (Device) messageDto.getSession().getUserProperties().get("device");
                    device.setX509Certificate(signer.getX509Certificate());
                    signer.setDevice(device);
                }
                responseDto.setConnectedDevice(DeviceDto.INIT_SIGNED_SESSION(signer));
                dao.getEM().merge(cmsMessage.setType(TypeVS.WEB_SOCKET_INIT));
                SessionManager.getInstance().putAuthenticatedDevice(messageDto.getSession(), signer, device);
                HttpSession httpSession = HTTPSessionManager.getInstance().getHttpSession(dto.getHttpSessionId());
                if(httpSession != null) {
                    log.log(Level.INFO, "authenticated HTTP session: " + dto.getHttpSessionId() + " - user: " + signer.getId());
                    httpSession.setAttribute(PrincipalVS.USER_KEY, signer.setDevice(browserDevice));
                }
                messageDto.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(responseDto));
                break;
            case WEB_SOCKET_BAN_SESSION:
                //TODO
                break;
            default: throw new ExceptionVS("unknownSocketOperationErrorMsg: " + messageDto.getOperation());
        }
    }

}