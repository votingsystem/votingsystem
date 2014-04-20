package org.votingsystem.model.vicket;

import org.apache.log4j.Logger;
import org.votingsystem.model.ActorVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="VicketProvider")
@DiscriminatorValue("VicketProvider")
public class VicketProvider extends ActorVS implements Serializable {

    private static Logger log = Logger.getLogger(VicketProvider.class);

    public static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public static final String TAG = "VicketProvider";



}
