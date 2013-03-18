

<u><h3 class="controllerInfoHeader">Servicio de Certificados</h3></u>


	 Servicios relacionados con los certificados manejados por la aplicación
 
 


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/certificado/index">/certificado/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/certificado'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/certificado/certificadoUsuario">/certificado/certificadoUsuario</a><br/>
				
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
				<a href="${grailsApplication.config.grails.serverURL}/certificado/certificadoCA_DeEvento">/certificado/certificadoCA_DeEvento</a><br/>
				
	  Servicio de consulta de los certificados emisores de certificados
	  de voto para una votación.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>idEvento</u>:  el identificador de la votación que se desea consultar.<br/>
					
						<u>controlAccesoId</u>:  el identificador en la base de datos del control de acceso en el 
	  		  que se publicó la votación.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Devuelve la cadena de certificación, en formato PEM, con la que se generan los 
	  			certificados de los votos.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/certificado/cadenaCertificacion">/certificado/cadenaCertificacion</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>La cadena de certificación del servidor</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/certificado/certificadoDeVoto">/certificado/certificadoDeVoto</a><br/>
				
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
	

</div>


