package org.sistemavotacion.centrocontrol

import org.codehaus.groovy.grails.web.json.JSONArray
import org.sistemavotacion.centrocontrol.modelo.*;
import org.codehaus.groovy.grails.web.json.JSONObject

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EtiquetaService {
	
	static transactional = true

	Set<Etiqueta> guardarEtiquetas(JSONArray etiquetas) {
		log.debug("guardarEtiquetas - etiquetas: ${etiquetas}")
		def etiquetaSet = etiquetas.findAll {it != JSONObject.NULL && 
				!"".equals(it)}.collect { etiquetaItem ->
			etiquetaItem = etiquetaItem.toLowerCase().trim()
			def etiqueta
			Etiqueta.withTransaction {
				etiqueta = Etiqueta.findByNombre(etiquetaItem)
				if (etiqueta) {
					etiqueta.frecuencia +=1
					etiqueta.save(flush: true)
				} else {
					etiqueta = new Etiqueta(nombre:etiquetaItem, frecuencia:1)
					etiqueta.save(flush: true)
				}
			}
			return etiqueta;
		}
		return etiquetaSet
	}
	
}

