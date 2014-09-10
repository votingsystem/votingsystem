package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContextVS
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.FileUtils

import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * @infoController TestingController
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class TestingController {

    def signatureVSService
    def grailsApplication

    def index() {

        signatureVSService.initCertAuthorities()
        render "OK"
    }

}