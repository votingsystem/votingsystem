package org.votingsystem.simulation.currency;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.ProcessQueue;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserToUserModel  extends Model {

    private ProcessQueue idleWalletUserQueue;

    /**
     * Constructs a model, with the given name and parameters for report and
     * trace files.
     *
     * @param owner        Model : The main model this model is associated to
     * @param name         java.lang.String : The name of this model
     * @param showInReport
     * @param showInTrace  boolean : Flag for showing this model in trace-files. Set it
     *                     to <code>true</code> if model should show up in trace,
     */
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
        return idleWalletUserQueue;
    }

}
