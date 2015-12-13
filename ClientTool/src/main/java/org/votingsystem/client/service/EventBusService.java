package org.votingsystem.client.service;

import com.google.common.eventbus.EventBus;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventBusService {


    private static final EventBusService INSTANCE = new EventBusService();
    private EventBus eventBus;


    public static EventBusService getInstance() {return INSTANCE;}

    private EventBusService() {
        eventBus = new EventBus();
    }


    public void register(Object observer) {
        eventBus.register(observer);
    }

    public void post(Object eventData) {
        eventBus.post(eventData);
    }

}