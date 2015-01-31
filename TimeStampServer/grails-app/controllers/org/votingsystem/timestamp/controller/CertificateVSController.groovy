package org.votingsystem.timestamp.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.util.CertUtils

import java.security.cert.X509Certificate

/**
* @infoController Servicio de Certificados
* @descController Servicios relacionados con los certificates manejados por la aplicación
* 
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class CertificateVSController {

    def testService
    /**
     * (Disponible sólo para pruebas)
     *
     * Servicio que añade Autoridades de Confianza.<br/>
     *
     * @httpMethod [POST]
     * @param pemCertificate certificado en formato PEM de la Autoridad de Confianza que se desea añadir.
     * @return Si todo va bien devuelve un código de estado HTTP 200.
     */
    def addCertificateAuthority () {
        if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            response.status = ResponseVS.SC_ERROR
            render (message(code: "serviceDevelopmentModeMsg"))
            return false
        }
        if("POST".equals(request.method)) {
            def messageJSON = request.JSON
            //  Map requestMap = [operation: TypeVS.CERT_CA_NEW.toString(), certChainPEM: new String(rootCACertPEMBytes, "UTF-8"), info: "Autority from Test Web App '${Calendar.getInstance().getTime()}'"]
            if (TypeVS.CERT_CA_NEW != TypeVS.valueOf(messageJSON.operation)) {
                log.error "Needed type 'CERT_CA_NEW'  found '${messageJSON.operation}'"
                response.status = ResponseVS.SC_ERROR
                render ("Needed type 'CERT_CA_NEW'  found '${messageJSON.operation}'")
                return false
            }
            Collection<X509Certificate> certX509CertCollection = CertUtils.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes())
            if(certX509CertCollection.isEmpty()) {
                log.error("Cert not found")
                response.status = ResponseVS.SC_ERROR
                render ("Cert not found")
                return false
            }
            X509Certificate x509NewCACert = certX509CertCollection.iterator()?.next()
            testService.addTrustedCert(x509NewCACert)
            response.status = ResponseVS.SC_OK
            render "Cert '${x509NewCACert.getSubjectDN()}' added to trusted list"
        }
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}