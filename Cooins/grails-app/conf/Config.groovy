import grails.util.Metadata

// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.converters.default.pretty.print=true
grails.gorm.failOnError=true
grails.reload.enabled = true

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination

// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['Gecko', 'WebKit', 'Presto', 'Trident']
grails.mime.types = [ // the first one is the default format
    all:           '*/*', // 'all' maps to '*' or the first available format in withFormat
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    hal:           ['application/hal+json','application/hal+xml'],
    xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        // filteringCodecForContentType.'text/html' = 'html'
    }
}

grails.converters.encoding = "UTF-8"
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

// configure passing transaction's read-only attribute to Hibernate session, queries and criterias
// set "singleSession = false" OSIV mode in hibernate configuration after enabling
grails.hibernate.pass.readonly = false
// configure passing read-only to OSIV session by default, requires "singleSession = false" OSIV mode
grails.hibernate.osiv.readonly = false

environments {

    development {
        if(Metadata.current.isWarDeployed()) {
            grails.logging.jul.usebridge = false
            grails.serverURL = "https://www.sistemavotacion.org/Cooins"
            webSocketURL="wss://cooins:8443/Cooins/websocket/service"
        } else {
            grails.resources.debug = true
            grails.serverURL = "http://cooins:8086/Cooins"
            webSocketURL="ws://cooins:8086/Cooins/websocket/service"
        }
    }

    production {
        grails.logging.jul.usebridge = false
        grails.serverURL = "https://www.sistemavotacion.org/Cooins"
        webSocketURL="wss://cooins:8443/Cooins/websocket/service"
    }

}


mail.error.server = 'localhost'
mail.error.port = 25
//mail.error.username = 'AccessControl@sistemavotacion.org'
//mail.error.password = '*****'
mail.error.to = 'admin@sistemavotacion.org'
mail.error.from = 'AccessControl@sistemavotacion.org'
mail.error.subject = '[Access Control Application Error]'
mail.error.starttls = false
mail.error.debug = false

// log4j configuration
log4j = {

    //System.setProperty 'mail.smtp.port', mail.error.port.toString()
    //System.setProperty 'mail.smtp.starttls.enable', mail.error.starttls.toString()

    appenders {
        file name:'CooinServerERRORS', threshold:org.apache.log4j.Level.ERROR,
                file:"${System.getProperty("user.home")}/VotingSystem/logs/CooinServerERRORS.log", datePattern: '\'_\'yyyy-MM-dd'

        rollingFile name:"CooinServer", threshold:org.apache.log4j.Level.DEBUG,
                layout:pattern(conversionPattern: '%d{[dd.MM.yy HH:mm:ss.SSS]} [%t] %p %c %x - %m%n'),
                file:"${System.getProperty("user.home")}/VotingSystem/logs/CooinServer.log", datePattern: '\'_\'yyyy-MM-dd'

        rollingFile name:"CooinServerReports", threshold:org.apache.log4j.Level.INFO,
                layout:pattern(conversionPattern: '%m%n'), maxFileSize:"1024MB", maxBackupIndex: 100,
                file:"./Cooins/ServerReports.log", datePattern: '\'_\'yyyy-MM-dd'

        rollingFile name:"CooinTransactionsReports", threshold:org.apache.log4j.Level.INFO,
                layout:pattern(conversionPattern: '%m%n'), maxFileSize:"1024MB", maxBackupIndex: 100,
                file:"./Cooins/TransactionsVS.log", datePattern: '\'_\'yyyy-MM-dd'

        rollingFile name:"CooinsIssued", threshold:org.apache.log4j.Level.INFO,
                layout:pattern(conversionPattern: '%m%n'), maxFileSize:"1024MB", maxBackupIndex: 100,
                file:"./Cooins/CooinsIssued.log", datePattern: '\'_\'yyyy-MM-dd'


        /*appender new SMTPAppender(name: 'smtp', to: mail.error.to, from: mail.error.from,
            subject: mail.error.subject, threshold: Level.ERROR,
            SMTPHost: mail.error.server, SMTPUsername: mail.error.username,
            SMTPDebug: mail.error.debug.toString(), SMTPPassword: mail.error.password,
            layout: pattern(conversionPattern:
               '%d{[ dd.MM.yyyy HH:mm:ss.SSS]} [%t] %n%-5p %n%c %n%C %n %x %n %m%n'))*/
    }


    root {
        debug 'stdout', 'CooinServer'
        error 'CooinServerERRORS', 'smtp'
    }

    environments {

        development{
            debug   'org.votingsystem','filters', 'grails.app'
            //debug   'org.springframework.security'
            //debug   'org.hibernate'
            //debug   'org.apache'

            info  additivity: true, CooinServerReports: 'reportsLog'
            info  additivity: true, CooinTransactionsReports: 'transactionsLog'
            info  additivity: true, CooinsIssued: 'cooinsIssuedLog'


            error   'org.codehaus.groovy.grails.web.servlet',  //  controllers
                    'org.codehaus.groovy.grails.web.pages', //  GSP
                    'org.codehaus.groovy.grails.web.sitemesh', //  layouts
                    'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
                    'org.codehaus.groovy.grails.web.mapping', // URL mapping
                    'org.codehaus.groovy.grails.commons', // core / classloading
                    'org.codehaus.groovy.grails.plugins', // plugins
                    'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
                    'org.springframework',
                    'grails.plugins',
                    'grails.app.services.org.grails.plugin.resource',
                    'grails.app.taglib.org.grails.plugin.resource',
                    'grails.app.resourceMappers.org.grails.plugin.resource'
            error   'org.hibernate'

        }

        production {
            info  additivity: false, CooinServerReports: 'reportsLog'
            info  additivity: false, CooinTransactionsReports: 'transactionsLog'

            debug   'org.votingsystem','filters', 'grails.app'
            //debug   'org.springframework.security'
            //debug   'org.hibernate'
            //debug   'org.apache'


            error   'org.codehaus.groovy.grails.web.servlet',  //  controllers
                    'org.codehaus.groovy.grails.web.pages', //  GSP
                    'org.codehaus.groovy.grails.web.sitemesh', //  layouts
                    'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
                    'org.codehaus.groovy.grails.web.mapping', // URL mapping
                    'org.codehaus.groovy.grails.commons', // core / classloading
                    'org.codehaus.groovy.grails.plugins', // plugins
                    'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
                    'org.springframework',
                    'grails.plugins',
                    'grails.app.services.org.grails.plugin.resource',
                    'grails.app.taglib.org.grails.plugin.resource',
                    'grails.app.resourceMappers.org.grails.plugin.resource'
            error   'org.hibernate'
            error   'org.hibernate'
        }

        test{ }

    }

}

grails.war.copyToWebApp = { args -> fileset(dir:"WEB-INF/votingsystem") { }}
vs.dev.accessControlURL='http://sistemavotacion.org/AccessControl'
vs.prod.accessControlURL='http://www.sistemavotacion.org/AccessControl'
vs.IBAN_bankCode = "7878"
vs.IBAN_branchCode = "8989"
vs.systemNIF = "COOIN_SYSTEM_NIF"
vs.serverName='Voting System Cooin Server'
vs.urlTimeStampServer='http://www.sistemavotacion.org/TimeStampServer'
vs.backupCopyPath='./Cooins/backups'
vs.weekReportsPath = vs.backupCopyPath + '/weekReports'
vs.errorsBaseDir='./Cooins/errors'
vs.keyStorePath='WEB-INF/votingsystem/CooinServer.jks'
vs.signKeyAlias='CooinServerKeys'
vs.signKeyPassword='PemPass'
vs.certAuthoritiesDirPath='WEB-INF/votingsystem/certs'
vs.defaulTagsFilePath='WEB-INF/docs/defaultTags.txt'
vs.certChainPath='WEB-INF/votingsystem/certChain.pem'
vs.requestTimeOut = 500 //milliseconds