package org.votingsystem.model.currency;

import org.votingsystem.model.User;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("BANK")
@NamedQueries({
        @NamedQuery(name = Bank.FIND_BANK_BY_NIF, query = "SELECT b FROM Bank b WHERE b.numId =:numId")
})
public class Bank extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FIND_BANK_BY_NIF = "Bank.findBankByNif";

    public Bank() {
        setType(Type.BANK);
    }
}
