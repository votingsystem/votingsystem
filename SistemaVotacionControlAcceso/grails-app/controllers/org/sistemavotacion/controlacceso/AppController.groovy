package org.sistemavotacion.controlacceso

import grails.converters.JSON

class AppController {

    def index() { }
		
	def clienteAndroid() {
		log.debug("*** Si llega aqui mostrar mensaje app market")
		if(params.boolean('clienteAndroidLoaded'))
			render(view:"index")
		else redirect(uri: "/app/index?clienteAndroidLoaded=false")
		return
	}
}
