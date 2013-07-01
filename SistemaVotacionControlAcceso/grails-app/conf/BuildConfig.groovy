/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/

grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

// uncomment (and adjust settings) to fork the JVM to isolate classpaths
//grails.project.fork = [
//   run: [maxMemory:1024, minMemory:64, debug:false, maxPerm:256]
//]

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
		//excludes "grails-plugin-logging", "log4j"
		excludes 'bcprov-jdk15', 'bcpg-jdk15', 'bcprov-jdk14', 'bcmail-jdk14'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
		
		mavenRepo "https://repo.springsource.org/repo"
		mavenRepo "http://repo1.maven.org/maven2"

        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }

    dependencies {
		// specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
		test("org.apache.pdfbox:pdfbox:1.0.0") {
			exclude 'jempbox'
			exported = false
		}
		compile('org.codehaus.groovy.modules.http-builder:http-builder:0.5.1',
			'org.apache.httpcomponents:httpmime:4.2.4',
			'org.apache.httpcomponents:httpclient:4.2.4',
			'org.bouncycastle:bcprov-jdk16:1.46',
			'org.bouncycastle:bcmail-jdk16:1.46',
			'org.bouncycastle:bcpg-jdk16:1.46',
			//'gnu.mail:gnumail:1.1.2',
			//'gnu.mail:inetlib:1.1.1',
			//'org.apache.geronimo.specs:geronimo-javamail_1.4_spec:1.7.1',
			'javax.mail:mail:1.4.7',
			'org.hibernate:hibernate-search:3.4.2.Final',
			'com.itextpdf:itextpdf:5.1.3',
			'com.lowagie:itext:2.1.0',
			'org.xhtmlrenderer:core-renderer:R8',
			'org.bouncycastle:bctsp-jdk16:1.46',
			'joda-time:joda-time:2.1',
			'org.rometools:rome-modules:1.0',
			'com.google.gwt.google-apis:gwt-visualization:1.1.2',
			//para hacer funcionar AntBuilder en Cloudfoundry
			'org.apache.ant:ant:1.8.3',
			//for rendering plugin in production environments
			'org.springframework:spring-test:3.1.4.RELEASE',
			'org.apache.ant:ant-launcher:1.8.3',
			'javax.servlet:javax.servlet-api:3.1.0'
			) {
				excludes "slf4j-api", "log4j", "commons-logging", "xalan",
					"xml-apis", "groovy","commons-io"
			}
			
			runtime 'org.postgresql:postgresql:9.2-1002-jdbc4'
			
    }

    plugins {
        runtime ":hibernate:$grailsVersion", 
				":jquery:1.8.3",
				":resources:1.1.6",
				":database-migration:1.3.2"

        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0"
        //runtime ":cached-resources:1.0"
        //runtime ":yui-minify-resources:0.1.5"

        build ":tomcat:$grailsVersion"

		//":cache:1.0.1" 
        compile (":gwt:0.8", ":executor:0.3",
			":rendering:0.4.3", ":rest-doc-plugin:0.4")
    }
	
}
