package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.Device;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.web.accesscontrol.websocket.SessionManager;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class WebSocketBean {

    private static Logger log = Logger.getLogger(WebSocketBean.class.toString());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;

    @Transactional
    public void processRequest(SocketMessageDto messageDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
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
            case WEB_SOCKET_BAN_SESSION:
                //talks
                break;
            default: throw new ExceptionVS("unknownSocketOperationErrorMsg: " + messageDto.getOperation());
        }
    }

}