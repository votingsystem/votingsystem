package org.votingsystem.services.impl;

import com.google.common.eventbus.EventBus;
import org.votingsystem.services.EventBusService;

import java.util.logging.Logger;

/**
 * license: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventBusServiceImpl implements EventBusService {

    private static Logger log = Logger.getLogger(EventBusServiceImpl.class.getName());

    private static final String EVENT_BUS_IDENTIFIER = "EventBusCoreService";
    private static final EventBus eventBus = new EventBus(EVENT_BUS_IDENTIFIER);

    public void register(Object eventBusListener) {
        eventBus.register(eventBusListener);
    }

    public void post(Object eventData) {
        eventBus.post(eventData);
    }

    public void init() {
        log.info("init");
    }

    public void destroy() {
        log.info("destroy");
    }

}
