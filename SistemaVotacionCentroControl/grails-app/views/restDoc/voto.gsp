

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

<h3 class="controllerInfoHeader"><u>Servicio de Votos</u></h3>

 Servicio que procesa los votos recibidos.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/voto/guardarAdjuntandoValidacion">/voto/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio que recoge los votos enviados por los usuarios.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. El voto firmado por el 
	         <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a><br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a></p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/voto/index">/voto/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/voto'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/voto/obtener">/voto/obtener</a><br/>
				
	  Servicio que devuelve la información de un voto a partir del  
	  hash asociado al mismo
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>hashCertificadoVotoHex</u>: Obligatorio. Hash en hexadecimal asociado al voto.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con la información del voto solicitado.</p>
			
			</div>
		<HR>
	

</div>