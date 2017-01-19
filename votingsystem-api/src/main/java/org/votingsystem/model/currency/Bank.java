package org.votingsystem.model.currency;

import org.votingsystem.model.User;
import org.votingsystem.util.IdDocument;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.io.Serializable;
import java.security.cert.X509Certificate;

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

    public static Bank getUser(X509Certificate certificate) {
        Bank user = new Bank();
        user.setX509Certificate(certificate);
        String subjectDN = certificate.getSubjectDN().getName();
        if (subjectDN.contains("C="))
            user.setCountry(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER="))
            user.setNumIdAndType(subjectDN.split("SERIALNUMBER=")[1].split(",")[0], IdDocument.NIF);
        if (subjectDN.contains("SURNAME="))
            user.setSurname(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME="))
            user.setName(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
        if (subjectDN.contains("CN="))
            user.setCn(subjectDN.split("CN=")[1]);
        return user;
    }

}
