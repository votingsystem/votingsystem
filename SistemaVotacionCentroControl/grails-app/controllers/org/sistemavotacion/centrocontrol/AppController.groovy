package org.sistemavotacion.centrocontrol

class AppController {

    def prueba() { 
		render (view: "prueba")
	}
	
	def index() {}
	
	def clienteAndroid() {
		log.debug("*** Si llega aqui mostrar mensaje app market browserToken: ${params.browserToken}" )
		if(params.boolean('androidClientLoaded'))
			render(view:"index")
		String uri = "/app/index?androidClientLoaded=false"
		if(params.browserToken) uri = "${uri}#${params.browserToken}"
		if(params.eventoId) uri = "${uri}&eventoId=${params.eventoId}"
		if(params.msg) uri = "${uri}&msg=${params.msg}"
		redirect(uri:uri)
		return
	}
	
}
