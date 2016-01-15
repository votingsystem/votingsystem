package org.votingsystem.simulation.currency;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Bank extends SimProcess {

    private UserToUserModel model;

    public Bank(Model owner, String name, boolean repeating, boolean showInTrace) {
        super(owner, name, repeating, showInTrace);
        model = (UserToUserModel) getModel();
    }

    @Override
    public void lifeCycle() {

    }


}
