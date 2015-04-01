package org.votingsystem.util;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Date;

public class EntityInterceptor extends EmptyInterceptor {


    public EntityInterceptor() { }

    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if(entity instanceof EntityVS) {
            EntityVS entityVS = (EntityVS)entity;
            Date dateModified = new Date();
            if(entityVS.getDateCreated() == null) entityVS.setDateCreated(dateModified);
            entityVS.setLastUpdated(dateModified);
        }
        //we are modifying property directly in the entity not through Object[] state, so result is false
        return false;
    }

}