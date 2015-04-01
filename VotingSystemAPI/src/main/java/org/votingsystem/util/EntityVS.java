package org.votingsystem.util;

import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@MappedSuperclass
public abstract class EntityVS {

    public abstract Date getDateCreated();
    public abstract void setDateCreated(Date date);
    public abstract void setLastUpdated(Date lastUpdated);

    @PrePersist
    public void prePersist() {
        Date date = new Date();
        setDateCreated(date);
        setLastUpdated(date);
    }

    @PreUpdate
    public void preUpdate() {
        setLastUpdated(new Date());
    }

}
