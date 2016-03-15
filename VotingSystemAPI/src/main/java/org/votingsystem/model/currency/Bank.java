package org.votingsystem.model.currency;

import org.votingsystem.model.User;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("Bank")
public class Bank extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    public Bank() {
        setType(Type.BANK);
    }

    public static Bank getUser(X509Certificate certificate) {
        Bank user = new Bank();
        user.setCertificate(certificate);
        String subjectDN = certificate.getSubjectDN().getName();
        if (subjectDN.contains("C=")) user.setCountry(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER=")) user.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.contains("SURNAME=")) user.setLastName(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME=")) {
            String givenname = subjectDN.split("GIVENNAME=")[1].split(",")[0];
            user.setName(givenname);
            user.setFirstName(givenname);
        }
        if (subjectDN.contains("CN=")) user.setCn(subjectDN.split("CN=")[1]);
        return user;
    }
}
