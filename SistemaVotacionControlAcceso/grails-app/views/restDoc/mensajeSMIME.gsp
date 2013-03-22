

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

<u><h3 class="controllerInfoHeader">Mensajes firmados</h3></u>

 Servicios relacionados con los mensajes firmados manejados por la
                  aplicación.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/mensajeSMIME/index">/mensajeSMIME/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/mensajeSMIME'</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/mensajeSMIME/obtenerReciboFirma">/mensajeSMIME/obtenerReciboFirma</a><br/>
				
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
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/mensajeSMIME/obtenerReciboReclamacion">/mensajeSMIME/obtenerReciboReclamacion</a><br/>
				
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
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/mensajeSMIME/obtener">/mensajeSMIME/obtener</a><br/>
				
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