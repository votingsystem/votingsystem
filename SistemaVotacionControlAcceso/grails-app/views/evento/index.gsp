

<u><h3 class="controllerInfoHeader">Eventos</h3></u>


	 Servicios relacionados con los eventos del sistema.
  
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/index">/evento/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/evento'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/informacionVotos">/evento/informacionVotos</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una votación
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los votos y solicitudes de acceso de una votación en formato JSON.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/guardarAdjuntandoValidacion">/evento/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio para publicar votaciones.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Archivo firmado en formato SMIME en cuyo contenido se
	         encuentra la votación que se desea publicar en formato HTML.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/guardarCancelacion">/evento/guardarCancelacion</a><br/>
				
	  Servicio que cancela eventos
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Obligatorio. Archivo con los datos del evento que se desea 
	  cancelar firmado por el usuario que publicó o un administrador de sistema.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/estadisticas">/evento/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a un evento.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Identificador en la base de datos del evento que se desea consultar.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Estadísticas asociadas al evento que se desea consultar en formato JSON.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/obtener">/evento/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Opcional. El identificador del evento en la base de datos. Si no se pasa ningún id 
	         la consulta se hará entre todos los eventos.<br/>
					
						<u>max</u>:  Opcional (por defecto 20). Número máximo de documentos que 
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>:  Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de creación.<br/>
					
						<u>offset</u>:  Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Página con manifiestos en formato JSON que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/informacionFirmasReclamacion">/evento/informacionFirmasReclamacion</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una acción de reclamación
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Obligatorio. El identificador de la reclamación la base de datos.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre las reclamaciones recibidas en formato JSON.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/informacionFirmas">/evento/informacionFirmas</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una acción de recogida de firmas
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Obligatorio. El identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre las firmas recibidas en formato JSON.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/guardarSolicitudCopiaRespaldo">/evento/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve los archivos relacionados con un evento.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Archivo firmado en formato SMIME con los datos del
	  		  evento origen de la copia de seguridad.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Archivo zip con los archivos relacionados con un evento.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/evento/comprobarFechas">/evento/comprobarFechas</a><br/>
				
	 Servicio que comprueba las fechas de un evento
	
	 <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:   Obligatorio. El identificador del evento en la base de datos.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	

</div>


