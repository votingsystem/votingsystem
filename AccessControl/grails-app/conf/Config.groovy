import java.net.*;
import org.apache.log4j.net.SMTPAppender
import org.apache.log4j.Level

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

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination

// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['Gecko', 'WebKit', 'Presto', 'Trident']
grails.mime.types = [
    all:           '*/*',
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

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

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
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
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

//To make tests on production environments. One of DEVELOPMENT, PRODUCTION, TEST
VotingSystemEnvironment="DEVELOPMENT"

environments {
    development {
        grails.logging.jul.usebridge = true
		grails.resources.debug = true// -> rendering problems
		String localIP = getLocalIP();
		println("Setting test development to: ${localIP}")
        grails.serverURL = "http://${localIP}:8080/${appName}"
    }
	production {
		grails.logging.jul.usebridge = false
		grails.serverURL = "http://192.168.1.4:8080/AccessControl"
	}
    test {
		grails.logging.jul.usebridge = true	
		String localIP = getLocalIP();
		println("Setting test address to: ${localIP}")
        grails.serverURL = "http://${localIP}:8080/${appName}"
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
		file name:'AccessControlERRORS', threshold:Level.ERROR,
			file:"./logs/AccessControlERRORS.log", datePattern: '\'_\'yyyy-MM-dd'
		
		rollingFile name:"AccessControl", threshold:org.apache.log4j.Level.DEBUG, 
			layout:pattern(conversionPattern: '%d{[dd.MM.yy HH:mm:ss.SSS]} [%t] %p %c %x - %m%n'),
            file:"./logs/AccessControl.log", datePattern: '\'_\'yyyy-MM-dd'
			
		/*appender new SMTPAppender(name: 'smtp', to: mail.error.to, from: mail.error.from,
			subject: mail.error.subject, threshold: Level.ERROR,
			SMTPHost: mail.error.server, SMTPUsername: mail.error.username,
			SMTPDebug: mail.error.debug.toString(), SMTPPassword: mail.error.password,
			layout: pattern(conversionPattern:
			   '%d{[ dd.MM.yyyy HH:mm:ss.SSS]} [%t] %n%-5p %n%c %n%C %n %x %n %m%n'))*/
		 
	}
		
	
    root {
        debug  'stdout', 'AccessControl'
        error 'AccessControlERRORES', 'smtp'
    }

    debug  'org.votingsystem','filtros', 'grails.app',//,'org.springframework.security',  'com.itextpdf.text.*'
           'org.springframework.*', 'org.apache.catalina.core', 'grails.*','org.hibernate.*' ,'org.codehaus.groovy.grails.*'
		   
    error  'org.hibernate',
            'grails.plugins',
            'grails.app.services.org.grails.plugin.resource',
            'grails.app.taglib.org.grails.plugin.resource',
            'grails.app.resourceMappers.org.grails.plugin.resource',
            'net.sf.ehcache.hibernate'
            /*'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',*/

}


def getLocalIP() {
	Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
	for (NetworkInterface netint : Collections.list(nets)){
		Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
		for (InetAddress inetAddress : Collections.list(inetAddresses)) {
			if(inetAddress.isSiteLocalAddress()) {
				String inetAddressStr = inetAddress.toString();
				while(inetAddressStr.startsWith("/"))
					inetAddressStr = inetAddressStr.substring(1)
				return inetAddressStr
			}
			
		}
	}
}

grails.war.copyToWebApp = { args ->
	fileset(dir:"WEB-INF/cms") {
	}
}

VotingSystem.backupCopyPath='./VotingSystem/backups'
VotingSystem.errorsBaseDir='./VotingSystem/errors'
SistemaVotacion.accessRequestFileName='accessRequest'
SistemaVotacion.csrRequestFileName='csr'
SistemaVotacion.imageFileName='image'
SistemaVotacion.representativeDataFileName='representativeData'
VotingSystem.keyStorePath='WEB-INF/cms/AccessControl.jks'
VotingSystem.signKeysAlias='AccessControlKeys'
VotingSystem.signKeysPassword='PemPass'
VotingSystem.certAuthoritiesDirPath='WEB-INF/cms/'
VotingSystem.certChainPath='WEB-INF/cms/certChain.pem'

VotingSystem.requestTimeOut = 500
VotingSystem.serverName='Primer AccessControlVS'
VotingSystem.votingHashAlgorithm='SHA256'
VotingSystem.blogURL = 'http://www.gruposp2p.org'
VotingSystem.emailAdmin='jgzornoza@gmail.com'
//_ TODO _
VotingSystem.adminsDNI='07553172H'