

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

<u><h3 class="controllerInfoHeader">Servicio de Certificados</h3></u>

 Servicios relacionados con los certificados manejados por la aplicación
 
 

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/trustedCerts">/certificado/trustedCerts</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Los certificados en formato PEM de las Autoridades Certificadoras en las que
	          confía la aplicación.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/index">/certificado/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/certificado'.</p>
			
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
					
						<u>usuarioId</u>:  Obligatorio. El identificador en la base de datos del usuario.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>El certificado en formato PEM.</p>
			
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
					
						<u>idEvento</u>:  el identificador de la votación que se desea consultar.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Devuelve la cadena de certificación, en formato PEM, con la que se generan los
	  			certificados de los votos.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/cadenaCertificacion">/certificado/cadenaCertificacion</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>La cadena de certificación en formato PEM del servidor</p>
			
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
					
						<u>hashCertificadoVotoHex</u>:  Obligatorio. Hash en hexadecimal asociado al
	           certificado de voto que se desea consultar.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>El certificado en formato PEM.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/addCertificateAuthority">/certificado/addCertificateAuthority</a><br/>
				
	  (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). Servicio que añade Autoridades de Confianza.<br/>
	  Sirve para poder validar los certificados enviados en las simulaciones.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>pemCertificate</u>:  certificado en formato PEM de la Autoridad de Confianza que se desea añadir.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	

</div>