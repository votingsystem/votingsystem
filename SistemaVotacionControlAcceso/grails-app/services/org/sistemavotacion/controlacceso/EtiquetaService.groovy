package org.sistemavotacion.controlacceso

import org.codehaus.groovy.grails.web.json.JSONArray
import org.sistemavotacion.controlacceso.modelo.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EtiquetaService {
	
	static transactional = true

	Set<Etiqueta> guardarEtiquetas(JSONArray etiquetas) {
		log.debug("guardarEtiquetas - etiquetas: ${etiquetas}")
		def etiquetaSet  
		etiquetas.collect { etiquetaItem ->
			if (!etiquetaItem || "".equals(etiquetaItem)) return null
			if(!etiquetaSet) etiquetaSet = new HashSet<Etiqueta>()
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
			etiquetaSet.add(etiqueta);
		}
		return etiquetaSet
	}
	
}

