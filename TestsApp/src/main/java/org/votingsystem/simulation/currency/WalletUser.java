package org.votingsystem.simulation.currency;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;

import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletUser extends SimProcess {

    private UserToUserModel model;
    private BigDecimal walletAmount;

    public WalletUser(Model owner, String name, boolean repeating, boolean showInTrace) {
        super(owner, name, repeating, showInTrace);
        model = (UserToUserModel) getModel();
    }

    @Override
    public void lifeCycle() {
        while (true) {
            if(walletAmount.compareTo(BigDecimal.ZERO) > 0) {

            } else {
                model.getIdleWalletUserQueue().insert(this);
                passivate();
            }
        }
        

    }
}
