package org.votingsystem.websocket;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.votingsystem.ticket.service.WebSocketService;
import org.votingsystem.util.ApplicationContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @deprecated
 *
 * Spring 3.x doesn't support @ServerEndpoint
 */
@Deprecated
public class SocketServletVS extends WebSocketServlet {

    private static Logger logger = Logger.getLogger(SocketServletVS.class);

    private static final long serialVersionUID = 1L;

    private static final String BROWSER_LISTENER_PREFIX = "WEB_BROWSER_CLIENT_";

    private final AtomicInteger connectionIds = new AtomicInteger(0);

    @Override protected StreamInbound createWebSocketInbound(
            String subProtocol, HttpServletRequest request) {
        //String[] beanNames = GrailsWebUtil.lookupApplication(getServletContext()).
        //		getMainContext().getBeanDefinitionNames();        
        return new SocketMessageInbound(connectionIds.incrementAndGet());
    }

    public final class SocketMessageInbound extends MessageInbound {

        private String browserId;
        private GrailsApplication grailsApplication;

        private SocketMessageInbound(int id) {
            //grailsApplication = GrailsWebUtil.lookupApplication(getServletContext());
            grailsApplication = (GrailsApplication) ApplicationContextHolder.getBean("grailsApplication");
        	/*String[] beanNames = grailsApplication.getMainContext().getBeanDefinitionNames(); 
        	for(String name : beanNames) {
        		logger.debug("beanName: " + name);
        	}*/
            this.browserId = BROWSER_LISTENER_PREFIX + id;
        }

        @Override protected void onOpen(WsOutbound outbound) {
            logger.debug(" - onOpen - client: " + browserId);
            ((WebSocketService)grailsApplication.getMainContext().
                    getBean("webSocketService")).onOpen(this);
        }

        @Override protected void onClose(int status) {
            ((WebSocketService)grailsApplication.getMainContext().
                    getBean("webSocketService")).onClose(this, status);
        }

        @Override protected void onBinaryMessage(ByteBuffer message) throws IOException {
            ((WebSocketService)grailsApplication.getMainContext().
                    getBean("webSocketService")).onBinaryMessage(this, message);
        }

        @Override protected void onTextMessage(CharBuffer message) throws IOException {
            ((WebSocketService)grailsApplication.getMainContext().
                    getBean("webSocketService")).onTextMessage(this, message);
        }

        public String getBrowserId() {
            return browserId;
        }

    }

}