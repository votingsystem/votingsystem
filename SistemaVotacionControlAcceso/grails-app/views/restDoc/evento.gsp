

        <style type="text/css" media="screen">
         .controllerInfoHeader {
	     	color: #4c4c4c;
	     }
	     .params_result_div {
	     	margin:0px 0px 0px 20px;
	     }
	     .serviceURLS {
	     	margin:10px 0px 20px 200px;
	     	color: #4c4c4c;
	     }
        </style>

<h3 class="controllerInfoHeader"><u>Eventos</u></h3>

 Servicios relacionados con los eventos del sistema.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/comprobarFechas">/evento/comprobarFechas</a><br/>
				
	 Servicio que comprueba las fechas de un evento
	
	 <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador del evento en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/estadisticas">/evento/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a un evento.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Identificador en la base de datos del evento que se desea consultar.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con estadísticas asociadas al evento consultado.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/guardarAdjuntandoValidacion">/evento/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio para publicar votaciones.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME en cuyo contenido se
	         encuentra la votación que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/guardarCancelacion">/evento/guardarCancelacion</a><br/>
				
	  Servicio que cancela eventos
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Archivo con los datos del evento que se desea 
	  cancelar firmado por el usuario que publicó o un administrador de sistema.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/guardarSolicitudCopiaRespaldo">/evento/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve los archivos relacionados con un evento.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos del
	  		  evento origen de la copia de seguridad.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo zip con los archivos relacionados con un evento.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/index">/evento/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/evento'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/informacionFirmas">/evento/informacionFirmas</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una acción de recogida de firmas
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con información sobre las firmas recibidas por el manifiesto solicitado.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/informacionFirmasReclamacion">/evento/informacionFirmasReclamacion</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una acción de reclamación
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la reclamación la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con información sobre las firmas recibidas por la reclamación solicitada.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/informacionVotos">/evento/informacionVotos</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una votación
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con información sobre los votos y solicitudes de acceso de una votación.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/obtener">/evento/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Opcional. El identificador del evento en la base de datos. Si no se pasa ningún id 
	         la consulta se hará entre todos los eventos.<br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de documentos que 
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>: Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de creación.<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR>
	

</div>