

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

<h3 class="controllerInfoHeader"><u>Reclamaciones</u></h3>

 Servicios relacionados con la publicación de reclamaciones.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/estadisticas">/eventoReclamacion/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a una reclamción.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. Identificador en la base de datos de la reclamación que se desea consultar.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con las estadísticas asociadas a la reclamación solicitada.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/firmado">/eventoReclamacion/firmado</a><br/>
				
	  Servicio que proporciona una copia de la reclamación publicada.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la reclamación en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo SMIME de la publicación de la reclamación firmada por el usuario 
	          que la publicó.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/guardarAdjuntandoValidacion">/eventoReclamacion/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio para publicar votaciones.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME en cuyo contenido se
	         encuentra la reclamación que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/guardarSolicitudCopiaRespaldo">/eventoReclamacion/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve las firmas recibidas por una reclamación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos de la
	  		  reclamación origen de la copia de seguridad.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo zip con los archivos relacionados con la reclamación.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/index">/eventoReclamacion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/eventoReclamacion'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/obtener">/eventoReclamacion/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Opcional. El identificador de la reclamación en la base de datos. Si no se pasa ningún 
	         id la consulta se hará entre todos las reclamaciones.<br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de reclamaciones que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>: Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de inicio.<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/validado">/eventoReclamacion/validado</a><br/>
				
	  Servicio que proporciona una copia de la reclamación publicada con la firma
	  añadida del servidor.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la reclamación en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo SMIME de la publicación de la reclamación firmada por el usuario que
	          la publicó y el servidor.</p>
			
			</div>
		<HR>
	

</div>