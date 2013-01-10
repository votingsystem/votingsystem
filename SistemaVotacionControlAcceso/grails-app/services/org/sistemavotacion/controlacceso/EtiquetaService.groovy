package org.sistemavotacion.controlacceso

import org.codehaus.groovy.grails.web.json.JSONArray
import org.sistemavotacion.controlacceso.modelo.*;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class EtiquetaService {
	
	static transactional = true

	Set<Etiqueta> guardarEtiquetas(JSONArray etiquetas) {
		log.debug("guardarEtiquetas - etiquetas: ${etiquetas}")
		def etiquetaSet = etiquetas.collect { etiquetaItem ->
			if ("".equals(etiquetaItem)) return null
			etiquetaItem = etiquetaItem.toLowerCase().trim()
			def etiqueta = Etiqueta.findByNombre(etiquetaItem)
			if (etiqueta) {
				etiqueta.frecuencia +=1
				etiqueta.save()
			} else {
				etiqueta = new Etiqueta(nombre:etiquetaItem, frecuencia:1)
				etiqueta.save()
			}
			return etiqueta
		}
		return etiquetaSet
	}
	
}

