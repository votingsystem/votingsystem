package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.model.UserVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.Query;
import java.util.logging.Logger;

@Singleton
@Startup
public class StartupBean {

    private static Logger log = Logger.getLogger(StartupBean.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TimeStampBean timeStampBean;
    @Inject SignatureBean signatureBean;
    @Inject EventVSElectionBean eventVSElectionBean;


    @PostConstruct
    public void initialize() throws Exception {
        log.info("initialize");
        ContextVS.init();
        Query query = dao.getEM().createQuery("select u from UserVS u where u.type =:type")
                .setParameter("type", UserVS.Type.SYSTEM);
        UserVS systemUser = dao.getSingleResult(UserVS.class, query);
        if(systemUser == null) {
            dao.persist(new UserVS(config.getSystemNIF(), config.getServerName(), UserVS.Type.SYSTEM));
        }
        timeStampBean.init();
        signatureBean.init();
    }

    @PreDestroy private void shutdown() { log.info(" --------- shutdown ---------");}
}
