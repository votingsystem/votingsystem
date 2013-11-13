package org.votingsystem.accesscontrol.service

import java.util.Date;
import java.util.List;

import org.codehaus.groovy.grails.web.json.JSONArray
import org.votingsystem.accesscontrol.model.*;

import java.security.cert.X509Certificate;

import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.*

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class UsuarioService {
	
	static transactional = true
	
	List<String> administradoresSistema
	def grailsApplication

	public Map getUsuario(Date fromDate){
		def usuarios
		Usuario.withTransaction {
			def criteria = Usuario.createCriteria()
			usuarios = criteria.list {
				gt("dateCreated", fromDate)
			}
		}
		int numUsu = Usuario.countByDateCreatedGreaterThan(fromDate)

		Map datosRespuesta = [totalNumUsu:numUsu]
		return datosRespuesta
	}
	
	public Map getControlCenterMap(CentroControl controlCenter) {
		Certificado certificado = Certificado.findWhere(actorConIP:controlCenter, estado:Certificado.Estado.OK)
		String cadenaCertificacionPEM
		if(certificado) {
			ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
			X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
			byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
			cadenaCertificacionPEM = new String(pemCert)
		}
		return [cadenaCertificacionPEM:cadenaCertificacionPEM, nombre:controlCenter.nombre,
			serverURL:controlCenter.serverURL, id:controlCenter.id, 
			serverType:TypeVS.CONTROL_CENTER.toString()]
	}
	
	boolean isUserAdmin(String nif) {
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList(
			"${grailsApplication.config.VotingSystem.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}
}

