package org.votingsystem.client.service;

import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventBusService {

    private static Logger log = Logger.getLogger(EventBusService.class.getName());

    private final Subject<Object, Object> eventBus = new SerializedSubject<>(PublishSubject.create());
    private static final EventBusService INSTANCE = new EventBusService();


    public static EventBusService getInstance() {return INSTANCE;}

    private EventBusService() {}


    public void register(Action1 observer) {
        eventBus.subscribe(observer);
    }

    public void post(Object eventData) {
        eventBus.onNext(eventData);
    }

}