package org.votingsystem.service;

import com.google.common.eventbus.EventBus;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventBusService {

    private static Logger log = Logger.getLogger(EventBusService.class.getName());

    private static final BlockingQueue queue = new ArrayBlockingQueue(50);
    private static final AtomicBoolean isRunning = new AtomicBoolean(Boolean.TRUE);

    private static final String EVENT_BUS_IDENTIFIER = "VotingSystemAPIEventBusService";
    private static final EventBus eventBus = new EventBus(EVENT_BUS_IDENTIFIER);
    private static final EventBusService INSTANCE = new EventBusService();

    public void register(Object eventBusListener) {
        eventBus.register(eventBusListener);
    }

    public void unRegister(Object eventBusListener) {
        eventBus.unregister(eventBusListener);
    }

    public void stop() {
        isRunning.set(Boolean.FALSE);
    }

    public void post(Object eventData) {
        try {
            queue.put(eventData);
            log.info("--- queue.put - queue.size: " + queue.size());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static EventBusService getInstance() {return INSTANCE;}

    private EventBusService() {
        new Thread(() -> {
            log.info("--- EventBusService started ---");
            while(isRunning.get()) {
                try {
                    eventBus.post(queue.take());
                    log.info("--- queue.take - queue.size: " + queue.size());
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }).start();
    }

}