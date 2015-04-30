package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.model.voting.UserRequestCsrVS;
import org.votingsystem.service.VotingSystemRemote;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@Stateless
@Remote(VotingSystemRemote.class)
public class RemoteTestBean implements VotingSystemRemote {

    private static final Logger log = Logger.getLogger(RemoteTestBean.class.getSimpleName());

    @Inject EventVSElectionBean eventVSElectionBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CSRBean csrBean;

    @Override
    public void generateBackup(Long eventId) throws Exception {
        log.info("generateBackup: " + eventId);
        EventVSElection eventVSElection = dao.find(EventVSElection.class, eventId);
        if(eventVSElection == null) throw new ValidationExceptionVS("ERROR - EventVSElection not found - eventId: " + eventId);
        eventVSElectionBean.generateBackup(eventVSElection);
    }

    @Asynchronous @Override
    public Future<String> testAsync(String message) {
        try {
            Thread.sleep(10000);
            return new AsyncResult<>("testAsync response - to message: " + message);
        } catch (InterruptedException e) {
            return new AsyncResult<>(e.getMessage());
        }
    }

    @Override
    public String validateCSR(String nif, String deviceId) throws Exception {
        log.info("validateCSR - nif: " + nif + " - deviceId: " + deviceId);
        if(config.getMode() != EnvironmentVS.DEVELOPMENT) {
            throw new ExceptionVS("service available only in mode DEVELOPMENT - actual mode: " + config.getMode());
        }
        Query query = dao.getEM().createQuery("select d from DeviceVS d where d.deviceId =:deviceId").setParameter("deviceId", deviceId);
        DeviceVS device = dao.getSingleResult(DeviceVS.class, query);
        if(device == null) throw new ExceptionVS("DeviceVS not found - deviceId: " + deviceId);
        String validatedNIF = NifUtils.validate(nif);
        query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif").setParameter("nif", validatedNIF);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        if(userVS == null) throw new ExceptionVS("UserVS not found - nif: " + validatedNIF);
        query = dao.getEM().createQuery("select csr from UserRequestCsrVS csr where csr.deviceVS=:deviceVS and " +
                "csr.userVS =:userVS and csr.state=:state").setParameter("deviceVS", device).setParameter("userVS", userVS)
                .setParameter("state", UserRequestCsrVS.State.OK);
        UserRequestCsrVS csrRequest = dao.getSingleResult(UserRequestCsrVS.class, query);
        if(csrRequest == null) throw new ExceptionVS("UserRequestCsrVS not found for nif: " + validatedNIF +
                " - and deviceId: " + deviceId);
        X509Certificate issuedCert = csrBean.signCertUserVS(csrRequest);
        return "issued cert:" + issuedCert.getSerialNumber().longValue() + "- subjectDN: " + issuedCert.getSubjectDN();
    }

    public void logTest(UserVS userVS) {
        log.info("========= logTest: " + userVS.getNif());

    }

}
