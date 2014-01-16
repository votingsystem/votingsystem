package org.votingsystem.model.ticket;

import org.apache.log4j.Logger;
import org.springframework.format.annotation.NumberFormat;
import org.votingsystem.model.UserVS;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="UserVSAccount")
public class UserVSAccount implements Serializable {

    private static Logger log = Logger.getLogger(UserVSAccount.class);

    public static final long serialVersionUID = 1L;


    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;


    @NotNull @NumberFormat(style= NumberFormat.Style.CURRENCY)
    private BigDecimal balance = null;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;


    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;
}
