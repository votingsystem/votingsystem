package org.votingsystem.model;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="BankVSInfo")
public class BankVSInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne  private BankVS bankVS;
    @Column(name="bankCode" ) private String bankCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankVS getBankVS() {
        return bankVS;
    }

    public BankVSInfo setBankVS(BankVS bankVS) {
        this.bankVS = bankVS;
        return this;
    }

    public String getBankCode() {
        return bankCode;
    }

    public BankVSInfo setBankCode(String bankCode) {
        this.bankCode = bankCode;
        return this;
    }
}
