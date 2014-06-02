grails.server.port.http = 8086

grails.tomcat.nio = true
grails.tomcat.scan.enabled = true

grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.8
grails.project.source.level = 1.8
grails.project.war.file = "target/${appName}.war"

grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    // configure settings for the run-app JVM
    //run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    run:false,
    // configure settings for the run-war JVM
    war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolver = "ivy"
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
        // runtime 'mysql:mysql-connector-java:5.1.29'

        compile('org.votingsystem:votingsystemlibrary:0.2.0',
                'org.apache.httpcomponents:httpmime:4.2.4',
                'org.apache.httpcomponents:httpclient:4.2.4',
                'org.bouncycastle:bcprov-jdk16:1.46',
                'org.bouncycastle:bcmail-jdk16:1.46',
                'org.bouncycastle:bcpg-jdk16:1.46',
                'org.iban4j:iban4j:2.1.1',
                'javax.mail:mail:1.4.7',
                'org.bouncycastle:bctsp-jdk16:1.46') {
            excludes "slf4j-api", "log4j", "commons-logging", "xalan", "xml-apis", "groovy","commons-io",
                    'bcprov-jdk15', 'bcpg-jdk15', 'bcprov-jdk14', 'bcmail-jdk14'
        }

        runtime 'org.postgresql:postgresql:9.3-1101-jdbc41'

        compile ("org.springframework:spring-expression:$springVersion", "org.springframework:spring-aop:$springVersion")

    }

    plugins {
        // plugins for the build system only
        build ":tomcat8:8.0.5"

        // plugins for the compile step
        compile ":scaffolding:2.1.0"
        compile ':cache:1.1.6'
        compile ":asset-pipeline:1.8.7"
        compile ":quartz:1.0.1"
        compile ":executor:0.3"

        // plugins needed at runtime but not for compilation
        runtime ":hibernate4:4.3.5.3" // or ":hibernate:3.6.10.15"
        runtime ":database-migration:1.4.0"
        runtime ":jquery:1.11.1"

        // Uncomment these to enable additional asset-pipeline capabilities
        //compile ":sass-asset-pipeline:1.7.4"
        //compile ":less-asset-pipeline:1.7.0"
        //compile ":coffee-asset-pipeline:1.7.0"
        //compile ":handlebars-asset-pipeline:1.3.0.3"
        if (Environment.current == Environment.DEVELOPMENT) {}
    }
}

grails.tomcat.jvmArgs = [ '-Xmx512m', '-XX:MaxPermSize=256m' ]
