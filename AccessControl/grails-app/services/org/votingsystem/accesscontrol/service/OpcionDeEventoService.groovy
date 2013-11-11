package org.votingsystem.accesscontrol.service

import org.codehaus.groovy.grails.web.json.JSONArray
import org.votingsystem.accesscontrol.model.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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

