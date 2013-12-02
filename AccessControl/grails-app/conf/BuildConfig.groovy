grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    // configure settings for the run-app JVM
    run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the run-war JVM
    war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolver = "ivy" // or maven -> problems with excludes
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
		excludes 'bcprov-jdk15', 'bcpg-jdk15', 'bcprov-jdk14', 'bcmail-jdk14'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        mavenCentral()
        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.
		
		compile(
			'org.votingsystem:votingsystemlibrary:0.2.0',
			'org.codehaus.groovy.modules.http-builder:http-builder:0.5.1',
			'org.apache.httpcomponents:httpmime:4.2.4',
			'org.apache.httpcomponents:httpclient:4.2.4',
			'org.bouncycastle:bcprov-jdk16:1.46',
			'org.bouncycastle:bcmail-jdk16:1.46',
			'org.bouncycastle:bcpg-jdk16:1.46',
			'org.bouncycastle:bctsp-jdk16:1.46',
			//'gnu.mail:gnumail:1.1.2',
			//'gnu.mail:inetlib:1.1.1',
			//'org.apache.geronimo.specs:geronimo-javamail_1.4_spec:1.7.1',
			'javax.mail:mail:1.4.7',
			'org.hibernate:hibernate-search:4.2.0.Final',
			'com.itextpdf:itextpdf:5.1.3',
			'org.xhtmlrenderer:core-renderer:R8',
			'org.rometools:rome-modules:1.0',
			//for rendering plugin
			'org.springframework:spring-test:3.2.5.RELEASE',
			) {excludes "slf4j-api", "log4j", "commons-logging", "xalan",
					"xml-apis", "groovy","commons-io"}
			
			compile 'org.postgresql:postgresql:9.2-1003-jdbc4'
    }

    plugins {
        // plugins for the build system only
        build   ":tomcat:7.0.47"

        // plugins for the compile step
        compile ":scaffolding:2.0.1"
        compile ':cache:1.1.1'
		compile ':executor:0.3'
		compile ':rendering:0.4.4'
		//runtime 'org.votingsystem:rest-doc-plugin:0.5'
		

        // plugins needed at runtime but not for compilation
        runtime ":hibernate4:4.1.11.2" // or ":hibernate:3.6.10.3"
        runtime ":resources:1.2.1"
		runtime ":gsp-resources:0.4.4"
		//runtime ":database-migration:1.3.5"
        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0.1"
        //runtime ":cached-resources:1.1"
        if (Environment.current == Environment.DEVELOPMENT) {}
    }
}

grails.tomcat.jvmArgs = [ '-Xmx1024m', '-Xms512m', '-XX:MaxPermSize=512m' ]