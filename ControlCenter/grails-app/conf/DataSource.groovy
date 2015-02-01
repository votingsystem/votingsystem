import grails.util.Metadata

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
dataSource {
    pooled = true
    driverClassName = "org.postgresql.Driver"
    username = "userVS"
    password = "userVS"
    //dataSource.logSql = true
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
//    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    singleSession = true // configure OSIV singleSession mode
    //hibernate.show_sql=true
}

// environment specific settings
environments {
    development {
        dataSource {
            if(Metadata.current.isWarDeployed()) {
                pooled = true
                dbCreate = "update"
                jndiName = "java:comp/env/jdbc/controlcenter"
            } else {
                dbCreate = "update" // one of 'create', 'create-drop','update'
                url="jdbc:postgresql://localhost:5432/ControlCenter"
                maxActive = 100
                maxIdle=30
                maxWait=10000
                validationQuery = "SELECT 1"
            }
        }
    }
    test {
        dataSource {
            dbCreate = "create-drop"
            url="jdbc:postgresql://localhost:5432/ControlCenter"
        }
    }

    production {
        dataSource {
            pooled = true
            dbCreate = "update"
            jndiName = "java:comp/env/jdbc/controlcenter"
        }
    }

}
