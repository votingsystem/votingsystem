package org.votingsystem.currency.web.ejb;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.currency.web.websocket.SessionManager;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.Device;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.socket.SocketRequest;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Messages;
import org.votingsystem.xml.XML;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class WebSocketEJB {

    private static Logger log = Logger.getLogger(WebSocketEJB.class.toString());
    
    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private TransactionEJB transactionBean;
    @Inject private SignatureService signatureService;

    @Transactional
    public void processRequest(SocketRequest socketRequest) throws Exception {
        User signer = null;
        MessageDto responseDto = null;
        Device browserDevice = null;
        switch(socketRequest.getDto().getSocketOperation()) {
            //Device (authenticated or not) sends message knowing target device UUID. Target device must be authenticated.
            case MSG_TO_DEVICE:
                if(SessionManager.getInstance().sendMessageByTargetDeviceId(socketRequest.getDto())) {//message send OK
                    socketRequest.getSession().getBasicRemote().sendText(XML.getMapper().writeValueAsString(
                            socketRequest.getDto().getServerResponse(ResponseDto.SC_WS_MESSAGE_SEND_OK, null)));
                } else socketRequest.getSession().getBasicRemote().sendText(XML.getMapper().writeValueAsString(
                        socketRequest.getDto().getServerResponse(ResponseDto.SC_WS_CONNECTION_NOT_FOUND,
                                Messages.currentInstance().get("webSocketDeviceSessionNotFoundErrorMsg"))));
                break;
            case CLOSE_SESSION: {
                SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                        SignedDocumentType.CLOSE_SESSION).setWithTimeStampValidation(true);
                SignedDocument signedDocument = signatureService.validateXAdES(
                        new InMemoryDocument(socketRequest.getBody().getBytes()), signatureParams);
                /*if(CurrencyOperation.CLOSE_SESSION == socketRequest.getDto().getOperation()) {
                    socketRequest.getSession().close();
                }*/
                break;
            }
            /*case GENERATE_BROWSER_CERTIFICATE: {
                SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                        SignedDocumentType.REMOTE_SIGNED_SESSION).setWithTimeStampValidation(true);
                SignedDocument signedDocument = signatureService.validateXAdES(
                        new InMemoryDocument(socketRequest.getBody().getBytes()), signatureParams);
                signer = signedDocument.getFirstSignatureByDate().getSigner();
                RemoteSignedSessionDto remoteSignedSessionDto = signedDocument.getSignedContent(RemoteSignedSessionDto.class);
                Session remoteSession = SessionManager.getInstance().getDeviceSession(
                        remoteSignedSessionDto.getUUID());
                if(remoteSession == null) {
                    socketRequest.getSession().getBasicRemote().sendText(XML.getMapper().writeValueAsString(
                            socketRequest.getDto().getServerResponse(ResponseDto.SC_ERROR, Messages.currentInstance().get(
                                    "webSocketDeviceSessionNotFoundErrorMsg"))));
                    return;
                }
                HttpSession remoteHttpSession = (HttpSession)remoteSession.getUserProperties().get(HttpSession.class.getName());
                if(remoteHttpSession == null) {
                    socketRequest.getSession().getBasicRemote().sendText(XML.getMapper().writeValueAsString(
                            socketRequest.getDto().getServerResponse(ResponseDto.SC_ERROR, Messages.currentInstance().get(
                                    "webSocketDeviceSessionNotFoundErrorMsg"))));
                    return;
                }
                PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(
                        remoteSignedSessionDto.getCsr().getBytes());
                User userFromCSR = User.getUser(csr.getSubject());
                if(!userFromCSR.checkUserFromCSR(signer.getX509Certificate())) {
                    socketRequest.getSession().getBasicRemote().sendText(XML.getMapper().writeValueAsString(
                            socketRequest.getDto().getServerResponse(ResponseDto.SC_ERROR,
                                    Messages.currentInstance().get("remoteCSRErrorMsg",
                                    signer.getFullName() + " " + signer.getNumIdAndType(),
                                    userFromCSR.getFullName() + " " + userFromCSR.getNumIdAndType()))));
                } else {
                    browserDevice = (Device) remoteSession.getUserProperties().get("device");
                    try {
                        Certificate certificate = cmsBean.signCSRForBrowserSession(csr, cmsMessage, browserDevice);
                        browserDevice.setX509Certificate(certificate.getX509Certificate());
                        if(browserDevice.getUser() != null) {
                            //device already in db
                            browserDevice.setCertificate(certificate).setUser(signer);
                            browserDevice = em.merge(browserDevice);
                        } else {
                            browserDevice.setUUID(UUID.randomUUID().toString()).setId(null);
                            browserDevice.setCertificate(certificate).setUser(signer);
                            em.persist(browserDevice);
                        }
                        SessionManager.getInstance().putAuthenticatedDevice(remoteSession, signer, browserDevice);
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                    DeviceDto mobileDevice = new DeviceDto(signer.getDevice());
                    responseDto = messageDto.getServerResponse(ResponseDto.SC_WS_CONNECTION_INIT_OK,
                            XML.getMapper().writeValueAsString(mobileDevice))
                            .setMessageType(TypeVS.GENERATE_BROWSER_CERTIFICATE);
                    responseDto.setConnectedDevice(DeviceDto.INIT_BROWSER_SESSION(signer, browserDevice));
                    remoteSession.getBasicRemote().sendText(XML.getMapper().writeValueAsString(responseDto));
                    em.merge(cmsMessage.setType(TypeVS.WEB_SOCKET_INIT));
                    remoteHttpSession.setAttribute(PrincipalVS.USER_KEY, signer.setDevice(browserDevice));
                    remoteHttpSession.setAttribute(HTTPSessionManager.WEBSOCKET_SESSION_KEY, remoteSession);
                }
                break;
            }
            case INIT_SIGNED_SESSION: {
                SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                        SignedDocumentType.INIT_SIGNED_SESSION).setWithTimeStampValidation(true);
                SignedDocument signedDocument = signatureService.validateXAdES(
                        new InMemoryDocument(socketRequest.getBody().getBytes()), signatureParams);

                if(Certificate.Type.USER_ID_CARD != signer.getCertificate().getType())
                    throw new ValidationException("ERROR - ID_CARD signature required");


                List<Device> devices = em.createQuery("select d from Device d where d.UUID =:UUID")
                        .setParameter("UUID", socketRequest.getDto().getDeviceFromUUID()).getResultList();


                responseDto = socketRequest.getDto().getServerResponse(
                        ResponseDto.SC_WS_CONNECTION_INIT_OK, null).setSocketOperation(SocketOperation.INIT_SIGNED_SESSION);
                if(device != null) {
                    signer.setDevice(device);
                    List<UserToken> userTokens = em.createQuery("select t from UserToken t where t.user =:user and t.state =:state")
                            .setParameter("user", signer).setParameter("state", UserToken.State.OK).getResultList();
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
                em.merge(cmsMessage.setType(TypeVS.WEB_SOCKET_INIT));
                SessionManager.getInstance().putAuthenticatedDevice(messageDto.getSession(), signer, device);
                HttpSession httpSession =  HTTPSessionManager.getInstance().getHttpSession(dto.getHttpSessionId());
                if(httpSession != null) {
                    log.log(Level.INFO, "authenticated HTTP session: " + dto.getHttpSessionId() + " - user: " + signer.getId());
                    httpSession.setAttribute(PrincipalVS.USER_KEY, signer.setDevice(browserDevice));
                }
                socketRequest.getSession().getBasicRemote().sendText(XML.getMapper().writeValueAsString(responseDto));
                break;
            }*/
            case MSG_TO_SERVER:
                //TODO
                break;
            default: throw new ValidationException("unknownSocketOperationErrorMsg: " + socketRequest.getDto().getOperation());
        }

    }

}