package org.votingsystem.model.ticket;

import org.apache.log4j.Logger;
import org.votingsystem.model.BatchRequest;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@DiscriminatorValue("TicketVSBatchRequest")
public class TicketVSBatchRequest extends BatchRequest implements Serializable  {

    private static Logger log = Logger.getLogger(TicketVSBatchRequest.class);

    public static final long serialVersionUID = 1L;


}