

<u><h3 class="controllerInfoHeader">Mensajes firmados</h3></u>


	 Servicios relacionados con los mensajes firmados manejados por la
                  aplicación.
  
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/mensajeSMIME/index">/mensajeSMIME/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/mensajeSMIME'</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtenerReciboFirma">/mensajeSMIME/obtenerReciboFirma</a><br/>
				
	  Servicio que devuelve el recibo con el que respondió el servidor al publicar un manifiesto.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: 	Obligatorio. Identificador del mensaje de publicación en la base de datos<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>El recibo.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtenerReciboReclamacion">/mensajeSMIME/obtenerReciboReclamacion</a><br/>
				
	  Servicio que devuelve el recibo con el que respondió el servidor al publicar una reclamación.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: 	Obligatorio. Identificador del mensaje de publicación en la base de datos<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>El recibo.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener">/mensajeSMIME/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: 	Obligatorio. Identificador del mensaje en la base de datos<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>El mensaje solicitado.</p>
			
			</div>
		<HR>
	

</div>


