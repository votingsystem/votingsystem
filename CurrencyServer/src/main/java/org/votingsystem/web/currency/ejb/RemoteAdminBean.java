package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.service.EJBRemoteAdminCurrencyServer;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@Stateless
@Remote(EJBRemoteAdminCurrencyServer.class)
public class RemoteAdminBean implements EJBRemoteAdminCurrencyServer {

    private static final Logger log = Logger.getLogger(RemoteAdminBean.class.getSimpleName());

    @Inject AuditBean auditBean;
    @Inject ConfigVS config;

    @Asynchronous
    @Override
    public Future<ResponseVS> initWeekPeriod(Calendar requestDate) throws IOException {
        log.info("initWeekPeriod - requestDate: " + requestDate.getTime());
        prepareRequest();
        try {
            auditBean.initWeekPeriod(requestDate);
            return new AsyncResult<>(ResponseVS.OK());
        } catch (Exception e) {
            return new AsyncResult<>(ResponseVS.ERROR(e.getMessage()));
        }
    }

    private void prepareRequest() {
        String bundleBaseName = config.getProperty("vs.bundleBaseName");
        MessagesVS.setCurrentInstance(Locale.getDefault(), bundleBaseName);
    }

}
