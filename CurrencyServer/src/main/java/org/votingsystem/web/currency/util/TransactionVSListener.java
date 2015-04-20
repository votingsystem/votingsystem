package org.votingsystem.web.currency.util;

import javax.persistence.PostPersist;
import java.util.logging.Logger;


public class TransactionVSListener {

    private static Logger log = Logger.getLogger(TransactionVSListener.class.getSimpleName());

    @PostPersist
    void onPostPersist(Object o) {
        log.info("============= class: " + o.getClass());
    }

}
