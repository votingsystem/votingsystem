/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
dataSource {
	pooled = true
	driverClassName = "org.postgresql.Driver"
	// dialect = org.hibernate.dialect.PostgreSQLDialect
	dialect = net.sf.hibernate.dialect.PostgreSQLDialect
	username = "UsuarioVotacion"
	password = "UsuarioVotacion"
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
	hibernate.search.default.indexBase = new File("./searchIndexCentroControl").absolutePath
}
// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop','update'
			url="jdbc:postgresql://localhost:5432/SistemaVotacionCentroControl"
        }
    }
    test {
        dataSource {
            dbCreate = "create-drop"
			url="jdbc:postgresql://localhost:5432/SistemaVotacionCentroControlTest"
        }
    }
    /*production {
		dataSource {
			pooled = true
			dbCreate = "update"
			url="jdbc:postgresql://localhost:5432/SistemaVotacionCentroControl"
			properties {
				maxActive = 100
				maxIdle = 30
				minIdle = 5
				initialSize = 5
				minEvictableIdleTimeMillis = 60000
				timeBetweenEvictionRunsMillis = 60000
				maxWait = 10000
				validationQuery = "/* ping *//*"
			}
		}
    }
	
	production {
		dataSource {
			pooled = true
			dbCreate = "update"
			jndiName = "java:comp/env/jdbc/centrocontrol"
		}
	}*/
	production {
		dataSource {
			dbCreate = "update" // one of 'create', 'create-drop','update'
			url="jdbc:postgresql://localhost:5432/SistemaVotacionCentroControl"
		}
	}
}