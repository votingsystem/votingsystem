

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

<h3 class="controllerInfoHeader"><u>Votaciones</u></h3>

 Servicios relacionados con las votaciones publicadas en el servidor.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/index">/eventoVotacion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/eventoVotacion'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/guardarCancelacion">/eventoVotacion/guardarCancelacion</a><br/>
				
	  Servicio de cancelación de votaciones 
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Archivo con los datos de la votación que se desea cancelar 
	  			firmado por el Control de Acceso que publicó la votación y por
	  			el usuario que la publicó o un administrador de sistema.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/estadisticas">/eventoVotacion/estadisticas</a><br/>
				
	  Servicio que ofrece datos de recuento de una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>controlAccesoServerURL</u>: Obligatorio. URL del Control de Acceso en el que se publicó el documento<br/>
					
						<u>eventoVotacionId</u>: Obligatorio. Identificador de la votación en la base de datos
	                           del Control de Acceso<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con estadísticas de la votación solicitada.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/obtenerVotos">/eventoVotacion/obtenerVotos</a><br/>
				
	  Servicio de consulta de los votos
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>controlAccesoServerURL</u>: Obligatorio. URL del Control de Acceso en el que se publicó el documento.<br/>
					
						<u>eventoVotacionId</u>: Obligatorio. Identificador de la votación en la base de datos 
	                           del Control de Acceso.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con la lista de votos recibidos por la votación solicitada.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/obtener">/eventoVotacion/obtener</a><br/>
				
	  Servicio de consulta de las votaciones publicadas.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Opcional. El identificador en la base de datos del documento que se
	  			  desee consultar.<br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de documentos que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con las votaciones que cumplen el criterio de búsqueda.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/guardarEvento">/eventoVotacion/guardarEvento</a><br/>
				
	  Servicio que da de alta las votaciones.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Archivo con los datos de la votación firmado
	  		  por el usuario que la publica y el Control de Acceso en la que se publica.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/comprobarFechas">/eventoVotacion/comprobarFechas</a><br/>
				
	  Servicio que comprueba las fechas de una votación
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
			</div>
		<HR>
	

</div>