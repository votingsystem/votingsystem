

<u><h3 class="controllerInfoHeader">Votaciones</h3></u>


	 Servicios relacionados con la publicación de votaciones.
  
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/index">/eventoVotacion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/eventoVotacion'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/guardarAdjuntandoValidacion">/eventoVotacion/guardarAdjuntandoValidacion</a><br/>
				
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
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/validado">/eventoVotacion/validado</a><br/>
				
	  Servicio que proporciona una copia de la votación publicada con la firma
	  añadida del servidor.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Archivo SMIME de la publicación de la votación firmada por el usuario que
	          la publicó y el servidor.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/firmado">/eventoVotacion/firmado</a><br/>
				
	  Servicio que proporciona una copia de la votación publicada.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Archivo SMIME de la publicación de la votación firmada por el usuario
	          que la publicó.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/estadisticas">/eventoVotacion/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Obligatorio. Identificador en la base de datos de la votación que se desea consultar.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Estadísticas asociadas a la votación que se desea consultar en formato JSON.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/obtener">/eventoVotacion/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Opcional. El identificador de la votación en la base de datos. Si no se pasa ningún id
	         la consulta se hará entre todos las votaciones.<br/>
					
						<u>max</u>:  Opcional (por defecto 20). Número máximo de votaciones que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>:  Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de inicio.<br/>
					
						<u>offset</u>:  Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Página con votaciones en formato JSON que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/guardarSolicitudCopiaRespaldo">/eventoVotacion/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve los votos y las solicitudes de acceso recibidas en una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Archivo firmado en formato SMIME con los datos de la
	  		  votación origen de la copia de seguridad.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Archivo zip con los archivos relacionados con la votación.</p>
			
			</div>
		<HR>
	

</div>


