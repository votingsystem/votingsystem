class UrlMappings {


	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"500"(view:'/error500')
		
		
		"/anuladorVoto/voto/${id}"{
			controller = "anuladorVoto"
			action = "get"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/certificado/voto/hashHex/$hashHex"{
			controller = "certificado"
			action = "voto"
		}
		
		"/certificado/usuario/$userId" {
			controller = "certificado"
			action = "usuario"
		}
		
		"/eventoVotacion"{
			controller = "eventoVotacion"
			action = [POST:"save"]
		}
		
		"/eventoVotacion/$id?"{
			controller = "eventoVotacion"
			action = "index"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		
		"/eventoVotacion/$id?/estadisticas"{
			controller = "eventoVotacion"
			action = "estadisticas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoVotacion/$id/comprobarFechas"{
			controller = "eventoVotacion"
			action = "comprobarFechas"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/messageSMIME/$id"{
			controller = "messageSMIME"
			action = "index"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/subscripcion/votaciones/$feedType"{
			controller = "subscripcion"
			action = "votaciones"
		}
		
		"/voto/hashHex/$hashHex" {
			controller = "voto"
			action = [GET:"hashCertificadoVotoHex"]
		}
		
		"/voto/${id}" {
			controller = "voto"
			action = "get"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/errors/event/${id}" {
			controller = "voto"
			action = "errors"
			constraints {
				id(matches:/\d*/)
			}
		}
		
		"/eventoVotacion/$id/votingErrors" {
			controller = "eventoVotacion"
			action = "votingErrors"
			constraints {
				id(matches:/\d*/)
			}
		}
	}
}
