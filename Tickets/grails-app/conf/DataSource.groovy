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
    //cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    //hibernate.show_sql=true
}

// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop','update'
            url="jdbc:postgresql://192.168.1.8:5432/TicketServer"
        }
    }
    test {
        dataSource {
            dbCreate = "create-drop"
            url="jdbc:postgresql://localhost:5432/TicketServerTest"
        }
    }
    /*production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            properties {
               maxActive = -1
               minEvictableIdleTimeMillis=1800000
               timeBetweenEvictionRunsMillis=1800000
               numTestsPerEvictionRun=3
               testOnBorrow=true
               testWhileIdle=true
               testOnReturn=false
               validationQuery="SELECT 1"
               jdbcInterceptors="ConnectionState"
            }
        }
    }*/

    production {
        dataSource {
            pooled = true
            dbCreate = "update"
            jndiName = "java:comp/env/jdbc/TicketServer"
        }
    }
    /*production {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop','update'
            url="jdbc:postgresql://localhost:5432/TicketServer"
        }
    }*/
}
