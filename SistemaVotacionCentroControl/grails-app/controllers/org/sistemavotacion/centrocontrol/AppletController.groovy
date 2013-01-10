package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*
import org.springframework.web.servlet.support.RequestContextUtils as RCU



class AppletController {
	
	def index() { }
	
	def cliente = { }
	
	def herramientaValidacion = { }
	
	def prueba = {
		render RCU.getLocale(request)
		return false;
	}
	
	def jnlpCliente = {
		render (view:"jnlpCliente", contentType: "application/x-java-jnlp-file")
	}
	
	def jnlpHerramienta = {
		render (view:"jnlpHerramienta", contentType: "application/x-java-jnlp-file")
	}
	
}