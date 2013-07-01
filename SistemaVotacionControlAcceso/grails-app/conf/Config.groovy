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

//grails.gorm.failOnError=true
 
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

//To test production environments, on of DEVELOPMENT, PRODUCTION, TEST
VotingSystemEnvironment="DEVELOPMENT"

// set per-environment serverURL stem for creating absolute links
environments {
    development {
        grails.logging.jul.usebridge = true
		String localIP = getDevelopmentServerIP();
        grails.serverURL = "http://${localIP}:8080/${appName}"
    }
	production {
		grails.logging.jul.usebridge = false
		grails.serverURL = "http://192.168.1.4:8080/SistemaVotacionControlAcceso"
	}
    test {
		grails.logging.jul.usebridge = true	
		String localIP = getDevelopmentServerIP();
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

log4j = {

	System.setProperty 'mail.smtp.port', mail.error.port.toString()
	System.setProperty 'mail.smtp.starttls.enable', mail.error.starttls.toString()
  
    appenders {
		file name:'ControlAccesoERRORES', threshold:Level.ERROR, 
			file:"./logs/ControlAccesoERRORES.log", datePattern: '\'_\'yyyy-MM-dd'
		
		rollingFile name:"ControlAcceso", threshold:org.apache.log4j.Level.DEBUG, 
			layout:pattern(conversionPattern: '%d{[dd.MM.yy HH:mm:ss.SSS]} [%t] %p %c %x - %m%n'),
            file:"./logs/ControlAcceso.log", datePattern: '\'_\'yyyy-MM-dd'
			
		appender new SMTPAppender(name: 'smtp', to: mail.error.to, from: mail.error.from,
			subject: mail.error.subject, threshold: Level.ERROR,
			SMTPHost: mail.error.server, SMTPUsername: mail.error.username,
			SMTPDebug: mail.error.debug.toString(), SMTPPassword: mail.error.password,
			layout: pattern(conversionPattern:
			   '%d{[ dd.MM.yyyy HH:mm:ss.SSS]} [%t] %n%-5p %n%c %n%C %n %x %n %m%n'))
		 
	}
		
	
    root {
            debug  'stdout', 'ControlAcceso'
			error 'ControlAccesoERRORES', 'smtp'
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

grails.war.copyToWebApp = { args ->
	fileset(dir:"WEB-INF/cms") {
	}
}

SistemaVotacion.baseRutaCopiaRespaldo='./VotingSystem/copiaRespaldo'
SistemaVotacion.eventsMetaInfBaseDir='./VotingSystem/Eventos_MetaInf'
SistemaVotacion.errorsBaseDir='./VotingSystem/errors'
SistemaVotacion.sufijoURLCadenaCertificacion='/certificado/cadenaCertificacion'
SistemaVotacion.sufijoURLInfoServidor='/infoServidor'
SistemaVotacion.sufijoURLEventoVotacionValidado='/eventoVotacion/validado/'
SistemaVotacion.sufijoURLEventoVotacion='/eventoVotacion/'
SistemaVotacion.sufijoURLEventoFirmaValidado='/eventoFirma/firmado/'
SistemaVotacion.sufijoURLInicializacionEvento='/eventoVotacion'
SistemaVotacion.sufijoURLGuardarVotos='/voto'
SistemaVotacion.sufijoURLFirmarManifiesto='/app/home#FIRMAR_MANIFIESTO&eventoId='
SistemaVotacion.sufijoURLFirmarReclamacion='/app/home#FIRMAR_RECLAMACION&eventoId='
SistemaVotacion.sufijoURLVotar='/app/home#VOTAR&eventoId='
SistemaVotacion.accessRequestFileName='accessRequest'
SistemaVotacion.nombreSolicitudCSR='csr'
SistemaVotacion.imageFileName='image'
SistemaVotacion.representativeDataFileName='representativeData'
SistemaVotacion.rutaAlmacenClaves='WEB-INF/cms/ControlAcceso.jks'
SistemaVotacion.aliasClavesFirma='ClavesControlAcceso'
SistemaVotacion.passwordClavesFirma='PemPass'
SistemaVotacion.rutaDirectorioArchivosCA='WEB-INF/cms/'
SistemaVotacion.rutaCadenaCertificacion='WEB-INF/cms/cadenaCertificacion.pem'
SistemaVotacion.cancelSufix='_CANCELLED'
SistemaVotacion.hashCertificadoVotoBase64='hashCertificadoVotoBase64'

SistemaVotacion.timeOutConsulta = 500
SistemaVotacion.serverName='Primer ControlAcceso'
SistemaVotacion.votingHashAlgorithm='SHA256'
SistemaVotacion.urlBlog = 'http://www.gruposp2p.org'
SistemaVotacion.emailAdmin='jgzornoza@gmail.com'
//TODO En un principio asi para no complicar mucho
SistemaVotacion.adminsDNI='07553172H'

pkcs7SignedContentType='application/x-pkcs7-signature'
pkcs7EncryptedContentType='application/x-pkcs7-mime'
