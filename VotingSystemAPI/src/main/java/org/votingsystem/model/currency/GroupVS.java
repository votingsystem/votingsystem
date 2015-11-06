package org.votingsystem.model.currency;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("GroupVS")
public class GroupVS extends UserVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public GroupVS() {
        setType(Type.GROUP);
    }
    
    public GroupVS(String name, UserVS.State state, UserVS representative, String description, Set<TagVS> tagVSSet) {
        setType(Type.GROUP);
        setName(name);
        setState(state);
        setRepresentative(representative);
        setDescription(description);
        setTagVSSet(tagVSSet);
    }

}
