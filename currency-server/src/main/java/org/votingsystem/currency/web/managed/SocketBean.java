package org.votingsystem.currency.web.managed;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.votingsystem.util.Constants;

import javax.enterprise.event.Observes;
import javax.faces.bean.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Collection;
import java.util.logging.Logger;

@Named("socketBean")
@ApplicationScoped
public class SocketBean implements Serializable {

    private static final Logger log = Logger.getLogger(SocketBean.class.getName());

    @Inject @Push
    private PushContext testChannel;

    public void broadcast(String message) {
        testChannel.send(message);
    }

    public void sendMessageToUser(String message, String userUUID) {
        testChannel.send(message, userUUID);
    }

    public void sendMessageToGroup(String message, Collection<String> userUUIDList) {
        testChannel.send(message, userUUIDList);
    }

    public String getUserUUID() {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return (String) req.getSession().getAttribute(Constants.USER_UUID);
    }

    public void onPushEvent(@Observes SocketPushEvent event) {
        log.info("event type: " + event.getType());
        switch (event.getType()) {
            case ALL_USERS:
                broadcast(event.getMessage());
                break;
            case TO_USER:
                sendMessageToUser(event.getMessage(), event.getUserUUID());
                break;
            case TO_GROUP:
                sendMessageToGroup(event.getMessage(), event.getUserSet());
                break;
        }
    }
}