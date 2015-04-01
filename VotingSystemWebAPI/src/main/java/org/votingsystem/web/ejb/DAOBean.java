package org.votingsystem.web.ejb;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Stateless
public class DAOBean {

    @PersistenceContext
    private EntityManager em;

    public <T> T persist(T t) {
        em.persist(t);
        return t;
    }

    public Query createNamedQuery(String namedQuery) {
        return em.createNamedQuery(namedQuery);
    }

    public EntityManager getEM() {
        return em;
    }

    public <T> List<T> persist(List<T> entities) {
        List<T> result = new ArrayList<>();
        for (T e : entities) {
            result.add(persist(e));
        }
        return result;
    }

    public <T, PK extends Serializable> T find(Class<T> type, PK id) {
        return (T) em.find(type, id);
    }


    public <T> T getSingleResult(Class<T> type, Query query){
        List results = query.getResultList();
        if(!results.isEmpty()){
            return (T) results.get(0);
        } else return null;
    }

    public <T> List<T> findAll(Class<T> type) {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(type));
        return em.createQuery(cq).getResultList();
    }

    public <T> List<T> findAll(Class<T> type, int offset, int pageSize) {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(type));
        return em.createQuery(cq).setFirstResult(offset).setMaxResults(pageSize).getResultList();
    }

    public <T> T merge(T type) {
        em.merge(type);
        return type;
    }

    public <T, PK extends Serializable> void delete(Class<T> type, PK id) {
        T ref = (T) em.find(type, id);
        em.remove(ref);
    }

    public <T> List<T> findWithNamedQuery(String queryName)
    {
        return em.createNamedQuery(queryName).getResultList();
    }

    public <T> List<T> findWithNamedQuery(String queryName, Map<String, Object> params) {
        Set<Entry<String, Object>> rawParameters = params.entrySet();
        Query query = em.createNamedQuery(queryName);
        for (Entry<String, Object> entry : rawParameters) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query.getResultList();
    }

    public <T> T findUniqueWithNamedQuery(String queryName) {
        return (T) em.createNamedQuery(queryName).getSingleResult();
    }

    public <T> T findUniqueWithNamedQuery(String queryName, Map<String, Object> params) {
        Set<Entry<String, Object>> rawParameters = params.entrySet();
        Query query = em.createNamedQuery(queryName);
        for (Entry<String, Object> entry : rawParameters) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return (T) query.getSingleResult();
    }


    public CriteriaBuilder getCriteriaBuilder() {
        return em.getCriteriaBuilder();
    }

    public <T> Long count(Class<T> type) {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<T> rt = cq.from(type);
        cq.select(em.getCriteriaBuilder().count(rt));
        Query q = em.createQuery(cq);
        return (Long) q.getSingleResult();
    }

}
