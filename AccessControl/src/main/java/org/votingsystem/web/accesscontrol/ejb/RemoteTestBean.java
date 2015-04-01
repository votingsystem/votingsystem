package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.model.EventVSElection;
import org.votingsystem.service.VotingSystemRemote;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Logger;

@Stateless
@Remote(VotingSystemRemote.class)
public class RemoteTestBean implements VotingSystemRemote {

    private static final Logger log = Logger.getLogger(RemoteTestBean.class.getSimpleName());

    @Inject EventVSElectionBean eventVSElectionBean;
    @Inject DAOBean dao;

    @Override
    public void generateBackup(Long eventId) throws Exception {
        log.info("generateBackup: " + eventId);
        EventVSElection eventVSElection = dao.find(EventVSElection.class, eventId);
        if(eventVSElection == null) throw new ValidationExceptionVS("ERROR - EventVSElection not found - eventId: " + eventId);
        eventVSElectionBean.generateBackup(eventVSElection);
    }

}
