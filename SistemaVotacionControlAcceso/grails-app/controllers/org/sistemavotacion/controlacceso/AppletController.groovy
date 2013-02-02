package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*
import org.springframework.web.servlet.support.RequestContextUtils as RCU



class AppletController {
	
	def index() { }
	
	def cliente = { }
	
	
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