package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.service.EJBRemoteAdminCurrencyServer;
import org.votingsystem.util.crypto.KeyStoreUtil;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@Stateless
@Remote(EJBRemoteAdminCurrencyServer.class)
public class RemoteAdminBean implements EJBRemoteAdminCurrencyServer {

    private static final Logger log = Logger.getLogger(RemoteAdminBean.class.getName());

    @Inject AuditBean auditBean;
    @Inject CMSBean cmsBean;
    @Inject ConfigVS config;

    @Asynchronous
    @Override
    public Future<ResponseVS> initWeekPeriod(Calendar requestDate) throws IOException {
        log.info("initWeekPeriod - requestDate: " + requestDate.getTime());
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        try {
            auditBean.initWeekPeriod(requestDate);
            return new AsyncResult<>(ResponseVS.OK());
        } catch (Exception e) {
            return new AsyncResult<>(ResponseVS.ERROR(e.getMessage()));
        }
    }

    @Override
    public byte[] generateUserKeyStore(String givenName, String surname, String nif, char[] password) throws Exception {
        MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
        KeyStore keyStore = cmsBean.generateKeysStore(givenName, surname, nif, password);
        return KeyStoreUtil.getBytes(keyStore, password);
    }

}
