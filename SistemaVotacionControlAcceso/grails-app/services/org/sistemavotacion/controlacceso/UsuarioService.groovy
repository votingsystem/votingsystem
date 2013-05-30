package org.sistemavotacion.controlacceso

import java.util.Date;

import org.codehaus.groovy.grails.web.json.JSONArray
import org.sistemavotacion.controlacceso.modelo.*;
import java.security.cert.X509Certificate;
import org.sistemavotacion.seguridad.*

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class UsuarioService {
	
	static transactional = true

	public Respuesta getUsuario(Date fromDate){
		def usuarios
		Usuario.withTransaction {
			def criteria = Usuario.createCriteria()
			usuarios = criteria.list {
				gt("dateCreated", fromDate)
			}
		}
		int numUsu = Usuario.countByDateCreatedGreaterThan(fromDate)

		Map datosRespuesta = [totalNumUsu:numUsu]
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
			tipoServidor:Tipo.CENTRO_CONTROL.toString()]
	}
}

