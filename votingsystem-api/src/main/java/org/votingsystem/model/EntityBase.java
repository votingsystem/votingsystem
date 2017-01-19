package org.votingsystem.model;

import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@MappedSuperclass
public abstract class EntityBase {

    public abstract void setDateCreated(LocalDateTime date);
    public abstract LocalDateTime getDateCreated();
    public abstract void setLastUpdated(LocalDateTime lastUpdated);
    public abstract LocalDateTime getLastUpdated();

    @PrePersist
    public void prePersist() {
        LocalDateTime date = LocalDateTime.now();
        setDateCreated(date);
        setLastUpdated(date);
    }

    @PreUpdate
    public void preUpdate() {
        setLastUpdated(LocalDateTime.now());
    }

}