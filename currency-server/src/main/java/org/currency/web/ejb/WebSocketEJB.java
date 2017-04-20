package org.currency.web.ejb;

import eu.europa.esig.dss.InMemoryDocument;
import org.currency.web.websocket.SessionManager;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.Device;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.socket.SocketRequest;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;
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
        CurrencyOperation socketOperation = (CurrencyOperation)socketRequest.getDto().getOperation().getType();
        switch(socketOperation) {
            //Device (authenticated or not) sends message knowing target device UUID. Target device must be authenticated.
            case MSG_TO_DEVICE:
                String deviceToUUID = socketRequest.getDto().getDeviceToUUID();
                if(deviceToUUID != null) {
                    if(SessionManager.getInstance().sendMessageByTargetDeviceUUID(socketRequest.getDto())) {//message send OK
                        socketRequest.getSession().getBasicRemote().sendText(XML.getMapper().writeValueAsString(
                                socketRequest.getDto().getServerResponse(ResponseDto.SC_WS_MESSAGE_SEND_OK, null,
                                        config.getEntityId())));
                        return;
                    }
                }
                log.severe("Target device UUID: " + deviceToUUID + " not found");
                socketRequest.getSession().getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                        socketRequest.getDto().getServerResponse(ResponseDto.SC_WS_CONNECTION_NOT_FOUND,
                                Messages.currentInstance().get("webSocketDeviceSessionNotFoundErrorMsg"),
                                config.getEntityId())));
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
            default: throw new ValidationException("unknownSocketOperationErrorMsg: " + socketRequest.getDto().getOperation());
        }

    }

}