package org.votingsystem.vicket.model;

import org.apache.log4j.Logger;
import org.votingsystem.model.BatchRequest;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("VicketBatchRequest")
public class VicketBatchRequest extends BatchRequest implements Serializable  {

    private static Logger log = Logger.getLogger(VicketBatchRequest.class);

    public static final long serialVersionUID = 1L;


}