package org.votingsystem.util;

import org.votingsystem.vicket.model.AlertVS;
import org.votingsystem.vicket.model.TransactionVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface ApplicationVS {

    public void updateBalances(TransactionVS transactionVS);
    public void alert(AlertVS alertVS);

}
