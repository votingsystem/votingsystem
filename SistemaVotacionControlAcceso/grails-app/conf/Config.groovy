import java.net.*;

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

grails.config.locations = [ "classpath:app-config.properties"]

 if(System.properties["${appName}.config.location"]) {
	grails.config.locations << "file:" + System.properties["${appName}.config.location"]
 }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
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
    xml:           ['text/xml', 'application/xml']
]

grails.resources.adhoc.excludes = ['**/gwt/**']

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
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

// set per-environment serverURL stem for creating absolute links
environments {
    production {
	//grails.serverURL = "http://sistemavotacioncontrolacceso.cloudfoundry.com"
	grails.logging.jul.usebridge = false
	String localIP = getDevelopmentServerIP();
	//=====
	//grails.serverURL = "http://${localIP}:8080/${appName}"
	grails.serverURL = "http://sistemavotacioncontrolacceso.cloudfoundry.com"
    }
    development {
	grails.logging.jul.usebridge = false
	String localIP = getDevelopmentServerIP();
        grails.serverURL = "http://${localIP}:8080/${appName}"
    }
    test {
	grails.logging.jul.usebridge = true	
	String localIP = getDevelopmentServerIP();
        grails.serverURL = "http://${localIP}:8080/${appName}"
    }

}

// log4j configuration
log4j = {

    appenders{
        appender new org.apache.log4j.DailyRollingFileAppender(
                name:"ControlAcceso",
                layout:pattern(conversionPattern: '%d{[dd.MM.yy HH:mm:ss.SSS]} [%t] %p %c %x - %m%n'),
                file:"./logs/ControlAcceso.log",
                datePattern: '\'_\'yyyy-MM-dd')

    }
	
    root {
            info  'stdout', 'ControlAcceso'
    }

    debug  'org.sistemavotacion','filtros',
           'grails.app',
           'grails.plugins',
           'org.springframework.security',
		   'org.apache.catalina.core',
		   'com.itextpdf.text.*'
		   
    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
		   

}

def getDevelopmentServerIP() {
	Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
	for (NetworkInterface netint : Collections.list(nets)){
		Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
		for (InetAddress inetAddress : Collections.list(inetAddresses)) {
			if(inetAddress.isSiteLocalAddress()) {
				String inetAddressStr = inetAddress.toString();
				while(inetAddressStr.startsWith("/"))
					inetAddressStr = inetAddressStr.substring(1)
				log.debug("Setting development address to: ${inetAddressStr}")
				return inetAddressStr
			}
			
		}
	}
}

grails {
	mail {
		host = "smtp.gmail.com"
		port = 465
		username = "SistemaVotacion"
		password = "*****"
		props = ["mail.smtp.auth":"true",
			 "mail.smtp.socketFactory.port":"465",
			 "mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
			 "mail.smtp.socketFactory.fallback":"false"]

	}
}


grails.war.copyToWebApp = { args ->
	fileset(dir:"WEB-INF/cms") {
		include(name: "**")
	}
}

