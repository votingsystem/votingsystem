

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

<h3 class="controllerInfoHeader"><u>Servicio de Certificados</u></h3>

 Servicios relacionados con los certificados manejados por la aplicación
 
 

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/cadenaCertificacion">/certificado/cadenaCertificacion</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La cadena de certificación del servidor</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/certificadoCA_DeEvento">/certificado/certificadoCA_DeEvento</a><br/>
				
	  Servicio de consulta de los certificados emisores de certificados
	  de voto para una votación.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>idEvento</u>: el identificador de la votación que se desea consultar.<br/>
					
						<u>controlAccesoId</u>: el identificador en la base de datos del control de acceso en el 
	  		  que se publicó la votación.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Devuelve la cadena de certificación, en formato PEM, con la que se generan los 
	  			certificados de los votos.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/certificadoDeVoto">/certificado/certificadoDeVoto</a><br/>
				
	  Servicio de consulta de certificados de voto.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>hashCertificadoVotoHex</u>: Obligatorio. Hash en hexadecimal asociado al
	           certificado de voto que se desea consultar.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El certificado en formato PEM.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/certificadoUsuario">/certificado/certificadoUsuario</a><br/>
				
	  Servicio de consulta de certificados de usuario.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>usuarioId</u>: Obligatorio. El identificador en la base de datos del usuario.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El certificado en formato PEM.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/index">/certificado/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/certificado'.</p>
			
			</div>
		<HR>
	

</div>