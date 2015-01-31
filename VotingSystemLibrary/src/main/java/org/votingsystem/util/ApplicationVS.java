package org.votingsystem.util;

import org.votingsystem.cooin.model.TransactionVS;
import org.votingsystem.model.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface ApplicationVS {

    public void updateBalances(TransactionVS transactionVS);
    public void alert(ResponseVS responseVS);

}
