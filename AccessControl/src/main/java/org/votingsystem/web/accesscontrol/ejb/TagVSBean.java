package org.votingsystem.web.accesscontrol.ejb;

import org.votingsystem.model.TagVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class TagVSBean {

    private static final Logger log = Logger.getLogger(TagVSBean.class.getSimpleName());

    @Inject DAOBean dao;

    public Set<TagVS> save(List<String> tags) {
        Query query = dao.getEM().createQuery("select t from TagVS t where t.name =:name");
        Set<TagVS> result = new HashSet<>();
        for(String tag : tags) {
            query.setParameter("name", tag);
            TagVS tagVS = dao.getSingleResult(TagVS.class, query);
            if(tagVS != null) {
                tagVS.setFrequency(tagVS.getFrequency() + 1);
                dao.merge(tagVS);
            } else {
                tagVS = dao.persist(new TagVS(tag, 1L));
            }
            result.add(tagVS);
        }
        return result;
    }

}
