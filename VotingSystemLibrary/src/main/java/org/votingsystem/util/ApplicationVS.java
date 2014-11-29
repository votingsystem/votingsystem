package org.votingsystem.util;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.cooin.model.TransactionVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface ApplicationVS {

    public void updateBalances(TransactionVS transactionVS);
    public void alert(ResponseVS responseVS);

}
