package org.sistemavotacion.controlacceso

import org.codehaus.groovy.grails.web.json.JSONArray
import org.sistemavotacion.controlacceso.modelo.*;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class OpcionDeEventoService {
	
    static transactional = true

    Set<OpcionDeEvento> guardarOpciones(EventoVotacion evento, JSONArray opciones) {
        log.debug("guardarOpciones - evento: ${evento.id} - opciones: ${opciones}")
        Set<OpcionDeEvento> opcionesSet = opciones.collect { opcionItem ->
			evento.refresh()
            OpcionDeEvento opcion = new OpcionDeEvento(eventoVotacion:evento, 
				contenido:opcionItem?.contenido)
            return opcion.save();
        }
        return opcionesSet
    }
	
}

