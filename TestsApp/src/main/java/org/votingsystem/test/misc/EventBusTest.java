package org.votingsystem.test.misc;

import com.google.common.eventbus.Subscribe;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.service.EventBusService;

import java.util.logging.Logger;

/**
 * Created by jgzornoza on 16/04/15.
 */
public class EventBusTest {

    private static Logger log = Logger.getLogger(EventBusService.class.getName());


    static class EventBusConsumer {
        @Subscribe public void responseVSChange(ResponseVS responseVS) {
            log.info("EventBusConsumer - response received: " + responseVS.toString());
        }
    }


    public static void main(String[] args) {
        EventBusService.getInstance().register(new EventBusConsumer());
        for(int i= 0; i < 10 ; i++) {
            log.info("sended message: " + i);
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK, "message: " + i);
            EventBusService.getInstance().post(responseVS);
        }

    }

}
