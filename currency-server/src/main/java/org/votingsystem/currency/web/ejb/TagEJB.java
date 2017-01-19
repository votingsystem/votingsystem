package org.votingsystem.currency.web.ejb;

import org.votingsystem.model.currency.Tag;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class TagEJB {

    private static final Logger log = Logger.getLogger(TagEJB.class.getName());

    @PersistenceContext
    private EntityManager em;

    public Set<Tag> save(Set<Tag> tags) {
        Set<Tag> result = new HashSet<>();
        for(Tag tag : tags) {
            List<Tag> tagList = em.createQuery("select t from Tag t where t.name =:name")
                    .setParameter("name", tag.getName().trim()).getResultList();
            Tag selectedTag = null;
            if(!tagList.isEmpty()) {
                selectedTag = tagList.iterator().next();
                selectedTag.setFrequency(selectedTag.getFrequency() + 1);
            } else {
                tag.setFrequency(1L).setName(tag.getName().trim());
                em.persist(tag);
                selectedTag = tag;
            }
            result.add(selectedTag);
        }
        return result;
    }

}