grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.source.level = 1.6
grails.project.target.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
		excludes 'bcprov-jdk15', 'bcpg-jdk15'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve

    repositories {
        inherits true // Whether to inherit repository definitions from plugins
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()

        // uncomment these to enable remote dependency resolution from public Maven repositories
        //mavenCentral()
        //mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
		// specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
		compile('org.codehaus.groovy.modules.http-builder:http-builder:0.5.1',
			'org.apache.httpcomponents:httpmime:4.1',
			'org.bouncycastle:bcprov-jdk16:1.46',
			'org.bouncycastle:bcmail-jdk16:1.46',
			'org.bouncycastle:bcpg-jdk16:1.46',
			'joda-time:joda-time:2.1',
			'org.rometools:rome-modules:1.0',
			'com.google.gwt.google-apis:gwt-visualization:1.1.2',
			'javax.mail:mail:1.4.1',
			'javax.activation:activation:1.1.1',
			'org.hibernate:hibernate-search:3.4.2.Final'
			) {
			excludes 'xalan'
			excludes 'xml-apis'
			excludes 'groovy'
			excludes 'commons-io'
			}
			runtime 'postgresql:postgresql:9.1-901.jdbc4'
    }

    plugins {
        compile (":hibernate:$grailsVersion", ":cloud-foundry:1.2.3", ":gwt:0.8")
        build ":tomcat:$grailsVersion"
    }
}
