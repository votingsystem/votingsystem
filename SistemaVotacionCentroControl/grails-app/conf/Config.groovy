/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

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
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
//grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']


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

// enable query caching by default
grails.hibernate.cache.queries = true

// set per-environment serverURL stem for creating absolute links
environments {
	
    development {
        grails.logging.jul.usebridge = true
		grails.serverURL = "http://192.168.1.4:8081/${appName}"
    }
    production {
        grails.logging.jul.usebridge = false
		//grails.serverURL = "http://gruposp2p.dyndns.org/${appName}"
		grails.serverURL = "http://sistemavotacioncentrocontrol.cloudfoundry.com"
    }
	test {
		//grails.serverURL = "http://sistemavotacioncontrolacceso.cloudfoundry.com"
		grails.serverURL = "http://localhost:8081/${appName}"
	}
}

// log4j configuration
log4j = {

    appenders{
        appender new org.apache.log4j.DailyRollingFileAppender(
                name:"CentroControl",
                layout:pattern(conversionPattern: '%d{[dd.MM.yy HH:mm:ss.SSS]} [%t] %p %c %x - %m%n'),
                file:"./logs/CentroControl.log",
                datePattern: '\'_\'yyyy-MM-dd')

    }
	
    root {
            info  'stdout', 'CentroControl'
    }

    debug  'org.sistemavotacion','filtros',
           'grails.app',
           'grails.plugins',
           'org.springframework.security'
		   
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

grails.plugin.cloudfoundry.username = 'jgzornoza@gmail.com'
grails.plugin.cloudfoundry.password = 'cloudfoundrysotopo'
grails.plugin.cloudfoundry.appname = 'SistemaVotacionCentroControl'

grails.war.copyToWebApp = { args ->
	fileset(dir:"WEB-INF/cms") {
		include(name: "**")
	}
}