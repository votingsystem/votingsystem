package org.votingsystem.client.service;

import com.google.common.eventbus.EventBus;
import org.apache.log4j.Logger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventBusService {

    private static Logger log = Logger.getLogger(EventBusService.class);

    private static final String EVENT_BUS_IDENTIFIER = "NotificationManager_EVENT_BUS";
    private static final EventBus eventBus = new EventBus(EVENT_BUS_IDENTIFIER);
    private static final EventBusService INSTANCE = new EventBusService();

    public void registerToEventBus(Object eventBusListener) {
        eventBus.register(eventBusListener);
    }

    public void postToEventBus(Object eventData) {
        eventBus.post(eventData);
    }

    public static EventBusService getInstance() {return INSTANCE;}

    private EventBusService() {}

}