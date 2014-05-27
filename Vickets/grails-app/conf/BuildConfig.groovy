grails.server.port.http = 8086

grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.7
grails.project.source.level = 1.7
grails.project.war.file = "target/${appName}.war"

grails.project.fork = [
        // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
        //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

        // configure settings for the test-app JVM, uses the daemon by default
        test: [maxMemory: 1024, minMemory: 64, debug: false, maxPerm: 256, daemon:true, source:1.7],
        // configure settings for the run-app JVM
        //if there are problems debugging -> run:false,
        run: [maxMemory: 1024, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false, source:1.7],

        // configure settings for the run-war JVM
        war: [maxMemory: 1024, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false, source:1.7],
        // configure settings for the Console UI JVM
        console: [maxMemory: 1024, minMemory: 64, debug: false, maxPerm: 256, source:1.7]
]

grails.project.dependency.resolver = "ivy" // or maven
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies

    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
        excludes 'bcprov-jdk15', 'bcpg-jdk15', 'bcprov-jdk14', 'bcmail-jdk14'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        mavenLocal()
        mavenCentral()
        grailsPlugins()
        grailsHome()

        grailsCentral()

        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }

    dependencies {
        compile('org.votingsystem:votingsystemlibrary:0.2.0',
                'org.apache.httpcomponents:httpmime:4.2.4',
                'org.apache.httpcomponents:httpclient:4.2.4',
                'org.bouncycastle:bcprov-jdk16:1.46',
                'org.bouncycastle:bcmail-jdk16:1.46',
                'org.bouncycastle:bcpg-jdk16:1.46',
                'javax.mail:mail:1.4.7',
                'org.bouncycastle:bctsp-jdk16:1.46') {
            excludes "slf4j-api", "log4j", "commons-logging", "xalan", "xml-apis", "groovy","commons-io",
                    'bcprov-jdk15', 'bcpg-jdk15', 'bcprov-jdk14', 'bcmail-jdk14'
        }
	    runtime 'org.postgresql:postgresql:9.2-1003-jdbc4'
    }



    plugins {
        // plugins for the build system only
        build   ":tomcat:7.0.47"

        // plugins for the compile step
        compile(':scaffolding:2.0.1', ':cache:1.1.1', ':quartz:1.0.1', ':executor:0.3') {
            //excludes "hibernate"
        }

        // plugins needed at runtime but not for compilation
        runtime(':hibernate4:4.1.11.2', ':resources:1.2.1', ':gsp-resources:0.4.4') {
            //excludes "hibernate"
        }

        //runtime ":database-migration:1.3.5"
        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0.1"
        //runtime ":cached-resources:1.1"
        //runtime ":yui-minify-resources:0.1.5"
        if (Environment.current == Environment.DEVELOPMENT) {}
    }
}
grails.tomcat.jvmArgs = [ '-Xmx512m', '-XX:MaxPermSize=256m' ]

