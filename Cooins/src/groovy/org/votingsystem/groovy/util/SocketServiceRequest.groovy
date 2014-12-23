package org.votingsystem.groovy.util

import org.apache.coyote.http11.upgrade.NioServletOutputStream
import org.apache.log4j.Logger
import org.apache.tomcat.websocket.WsRemoteEndpointAsync
import org.apache.tomcat.websocket.WsSession
import org.apache.tomcat.websocket.server.WsRemoteEndpointImplServer
import net.sf.json.JSONNull
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.votingsystem.cooin.websocket.SessionVS
import org.votingsystem.cooin.websocket.SessionVSManager
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.throwable.ExceptionVS
import javax.websocket.Session
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class SocketServiceRequest {

    private static Logger log = Logger.getLogger(SocketServiceRequest.class);

    Session session;
    JSONObject messageJSON;
    SessionVS sessionVS
    Locale locale
    TypeVS operation
    SMIMEMessage smimeMessage;
    InetSocketAddress remoteAddress

    public SocketServiceRequest(Session session, String msg, boolean last) {
        this.remoteAddress = ((InetSocketAddress)((NioServletOutputStream)((WsRemoteEndpointImplServer)(
                (WsRemoteEndpointAsync) ((WsSession)session).remoteEndpointAsync).base).sos).
                socketWrapper.socket.sc.remoteAddress);
        this.session = session;
        messageJSON = (JSONObject)JSONSerializer.toJSON(msg);
        if(!messageJSON.sessionId) messageJSON.sessionId = session.getId()
        if(!messageJSON.locale) throw new ExceptionVS("missing message 'locale'")
        locale = Locale.forLanguageTag(messageJSON.locale)
        if(!messageJSON.operation || JSONNull.getInstance().equals(messageJSON.operation))
            throw new ExceptionVS("missing message 'operation'")
        operation = TypeVS.valueOf(messageJSON.operation)
        if(TypeVS.MESSAGEVS_SIGN == operation) {
            if(!messageJSON.deviceId) throw new ExceptionVS("missing message 'deviceId'")
        }
        sessionVS = SessionVSManager.getInstance().getAuthenticatedSession(session)
        log.debug("session id: ${session.getId()} - operation : ${messageJSON?.operation} - " +
                "remoteIp: ${remoteAddress.address} - last: ${last}")
    }
    JSONObject getResponse(Integer statusCode, String message){
        return JSONSerializer.toJSON([statusCode:statusCode, message:message,
                  sessionId:session.getId(), operation:TypeVS.MESSAGEVS_FROM_VS, UUID:messageJSON.UUID])
    }

    JSONObject getResponse(Integer statusCode, String message, Long userId){
        return JSONSerializer.toJSON([statusCode:statusCode, message:message, userId:userId,
                  sessionId:messageJSON.sessionId, operation:TypeVS.MESSAGEVS_FROM_VS, UUID:messageJSON.UUID])
    }

    SMIMEMessage getSMIME() {
        if(!smimeMessage) smimeMessage = new SMIMEMessage(new ByteArrayInputStream(messageJSON.smimeMessage.decodeBase64()))
        return smimeMessage
    }
}
