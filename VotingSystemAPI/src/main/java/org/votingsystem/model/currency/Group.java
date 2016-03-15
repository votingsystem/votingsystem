package org.votingsystem.model.currency;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("Group")
public class Group extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    public Group() {
        setType(Type.GROUP);
    }
    
    public Group(String name, User.State state, User representative, String description, Set<TagVS> tagVSSet) {
        setType(Type.GROUP);
        setName(name);
        setState(state);
        setRepresentative(representative);
        setDescription(description);
        setTagVSSet(tagVSSet);
    }

}
