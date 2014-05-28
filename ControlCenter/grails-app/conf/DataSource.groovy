/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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
    hibernate.search.default.indexBase = new File("./VotingSystem/searchIndexControlCenter").absolutePath
    //hibernate.show_sql=true
}

// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop','update'
            url="jdbc:postgresql://localhost:5432/ControlCenter"
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
