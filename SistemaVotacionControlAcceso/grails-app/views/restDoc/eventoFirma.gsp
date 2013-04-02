

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

<h3 class="controllerInfoHeader"><u>Manifiestos</u></h3>

 Servicios relacionados con la publicación de manifiestos.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/estadisticas">/eventoFirma/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a un manifiesto.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Identificador en la base de datos del manifiesto que se desea consultar.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con las estadísticas asociadas al manifiesto solicitado.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/guardarAdjuntandoValidacion">/eventoFirma/guardarAdjuntandoValidacion</a><br/>
				
	  (EN DESUSO)
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME en cuyo contenido se 
	         encuentra el manifiesto que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/guardarSolicitudCopiaRespaldo">/eventoFirma/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve todas las firmas recibidas por un manifiesto.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos del
	  		  manifiesto origen de la copia de seguridad.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo zip con todas las firmas que ha recibido el manifiesto consultado.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/index">/eventoFirma/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/eventoFirma'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/obtener">/eventoFirma/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con información del manifiesto solicitado.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/obtenerHtml">/eventoFirma/obtenerHtml</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: el identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El manifiesto en formato HTML.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/obtenerManifiestos">/eventoFirma/obtenerManifiestos</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de documentos que 
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>: Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de inicio.<br/>
					
						<u>estadoEvento</u>: Opcional, posibles valores 'ACTIVO','CANCELADO', 'FINALIZADO', 'PENDIENTE_COMIENZO'.
	  		               El estado de los eventos que se desea consultar.<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/obtenerPDF">/eventoFirma/obtenerPDF</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: El identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El manifiesto en formato PDF.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/publicarPDF">/eventoFirma/publicarPDF</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>htmlManifest</u>: Manifiesto que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200 con el identificador
	  del nuevo manifiesto en la base de datos en el cuerpo del mensaje.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/validarPDF">/eventoFirma/validarPDF</a><br/>
				
	  Servicio que valida los manifiestos que se desean publicar. <br/>
	  La publicación de manifiestos se produce en dos fases. En la primera
	  se envía a '/eventoFirma/publicarPDF' el manifiesto en formato HTML, el servidor 
	  lo valida y si todo es correcto genera el PDF y envía al programa cliente el identificador 
	  del manifiesto en la base de datos. El programa cliente puede descargarse con ese
	  identificador el PDF firmarlo y enviarlo a este servicio.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador en la base de datos del manifiesto.<br/>
					
						<u>signedPDF</u>: Obligatorio. PDF con el manifiesto que se desea publicar firmado
	         por el autor.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	

</div>