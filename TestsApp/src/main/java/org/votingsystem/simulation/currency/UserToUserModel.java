package org.votingsystem.simulation.currency;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.ProcessQueue;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserToUserModel  extends Model {

    private ProcessQueue<User> idleUserQueue;
    private List<User> userList;


    public UserToUserModel(Model owner, String name, boolean showInReport, boolean showInTrace) {
        super(owner, name, showInReport, showInTrace);
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public void doInitialSchedules() {

    }

    @Override
    public void init() {

    }

    public ProcessQueue getIdleWalletUserQueue() {
        return idleUserQueue;
    }

}
