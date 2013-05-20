/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class UrlMappings {

	static mappings = {		
		
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}
		
		"/"(view:"/index")
		"500"(view:'/error500')
		
		"/android/SistemaVotacion.apk" {
			controller = "android"
			action = "app"
		}
	
		"/anuladorVoto" {
			controller = "anuladorVoto"
			action = [POST:"post"]
		}
		
		"/anuladorVoto/$hashHex" {
			controller = "anuladorVoto"
			action = "index"
		}
		
		
		"/anuladorVoto/voto/${id}"{
			controller = "anuladorVoto"
			action = "get"
			constraints {
				id(matches:/\d*/)
			}
		}		
		
		"/certificado/usuario/$userId" {
			controller = "certificado"
			action = "usuario"
			constraints {
				userId(matches:/\d*/)
			}
		}
		
		"/certificado/eventCA/$idEvento" {
			controller = "certificado"
			action = "eventCA"
			constraints {
				idEvento(matches:/\d*/)
			}
		}
		
		"/certificado/voto/hashHex/$hashHex" {
			controller = "certificado"
			action = "voto"
		}
		
		"/evento/$id?" {
			controller = "evento"
			action = "index"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/evento/$id/comprobarFechas" {
			controller = "evento"
			action = "comprobarFechas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/evento/$id/estadisticas" {
			controller = "evento"
			action = "estadisticas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoFirma" {
			controller = "eventoFirma"
			action = [POST:"post"]
		}
		
		"/eventoFirma/$id" {
			controller = "eventoFirma"
			action = [GET:"index"]
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoFirma/$id/estadisticas" {
			controller = "eventoFirma"
			action = "estadisticas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoFirma/$id/informacionFirmas" {
			controller = "eventoFirma"
			action = "informacionFirmas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoFirma/firmado/$id" {
			controller = "eventoFirma"
			action = "firmado"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		
		"/eventoReclamacion" {
			controller = "eventoReclamacion"
			action = [POST:"post"]
		}

		"/eventoReclamacion/$id?" {
			controller = "eventoReclamacion"
			action = [GET:"index"]
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoReclamacion/$id/estadisticas" {
			controller = "eventoReclamacion"
			action = "estadisticas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoReclamacion/${id}/firmado" {
			controller = "eventoReclamacion"
			action = "firmado"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoReclamacion/${id}/validado" {
			controller = "eventoReclamacion"
			action = "validado"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoReclamacion/${id}/informacionFirmas" {
			controller = "eventoReclamacion"
			action = "informacionFirmas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoVotacion" {
			controller = "eventoVotacion"
			action = [POST:"post"]
		}
		
		"/eventoVotacion/$id?" {
			controller = "eventoVotacion"
			action = "index"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoVotacion/$id/estadisticas" {
			controller = "eventoVotacion"
			action = "estadisticas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoVotacion/$id/informacionVotos" {
			controller = "eventoVotacion"
			action = "estadisticas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoVotacion/${id}/validado" {
			controller = "eventoVotacion"
			action = "validado"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoVotacion/${id}/firmado" {
			controller = "eventoVotacion"
			action = "firmado"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/mensajeSMIME/$id" {
			controller = "mensajeSMIME"
			action = "index"
		}
		
		"/mensajeSMIME/recibo/$requestMessageId" {
			controller = "mensajeSMIME"
			action = "recibo"
		}
		
		"/recolectorFirma/$id?" {
			controller = "recolectorFirma"
			action = "index"
			constraints {
				id(matches:/\d*/)
		    }
		}

		"/representative" {
			controller = "representative"
			action = [POST:"processFileMap"]
		}
		
		"/representative/$id?" {
			controller = "representative"
			action = "index"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/representative/nif/$nif" {
			controller = "representative"
			action = "getByNif"
		}
		
		"/representative/image/$id" {
			controller = "representative"
			action = "image"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/representative/$representativeId/image" {
			controller = "representative"
			action = [GET:"image"]
			constraints {
				representativeId(matches:/\d*/)
			}
		}
		
		"/solicitudAcceso" {
			controller = "solicitudAcceso"
			action = [POST:"processFileMap"]
		}
		
		"/solicitudAcceso/$id" {
			controller = "solicitudAcceso"
			action = [GET:"index"]
		}
		
		"/solicitudAcceso/evento/$eventoId/nif/$nif" {
			controller = "solicitudAcceso"
			action = [GET:"encontrarPorNif"]
		}
		
		"/solicitudAcceso/hashHex/$hashHex" {
			controller = "solicitudAcceso"
			action = [GET:"hashHex"]
		}
		
		"/solicitudCopia/download/$id" {
			controller = "solicitudCopia"
			action = [GET:"download"]
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/solicitudCopia/$id" {
			controller = "solicitudCopia"
			action = [GET:"get"]
			constraints {
				id(matches:/\d*/)
			}
		}

		"/subscripcion/reclamaciones/$feedType?"{
			controller = "subscripcion"
			action = "reclamaciones"
		}
		
		"/subscripcion/votaciones/$feedType?"{
			controller = "subscripcion"
			action = "votaciones"
		}
		
		"/subscripcion/manifiestos/$feedType?"{
			controller = "subscripcion"
			action = "manifiestos"
		}
		
		"/timeStamp/$serialNumber" {
			controller = "timeStamp"
			action = [GET:"getBySerialNumber"]
			constraints {
				serialNumber(matches:/\d*/)
			}
		}
		
		"/user/$nif/representative" {
			controller = "user"
			action = [GET:"representative"]
		}
				
		"/voto" {
			controller = "voto"
			action = [POST:"post"]
		}
		
		
		"/voto/${id}" {
			controller = "voto"
			action = "get"
			constraints {
				id(matches:/\d*/)
			}
		}
	}
}
