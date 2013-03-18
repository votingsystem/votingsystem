

<u><h3 class="controllerInfoHeader">Solicitudes de acceso</h3></u>


	 Servicios relacionados con las solicitudes de acceso recibidas en una votación.
  
 


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/solicitudAcceso/index">/solicitudAcceso/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/solicitudAcceso'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/solicitudAcceso/procesar">/solicitudAcceso/procesar</a><br/>
				
	  Servicio que valida las <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	  solicitudes de acceso</a> recibidas en una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Obligatorio. La solicitud de acceso.<br/>
					
						<u>csr</u>:  Obligatorio. La solicitud de certificado de voto.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>La solicitud de certificado de voto firmada.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/solicitudAcceso/obtener">/solicitudAcceso/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>:  Opcional. El identificador de la solicitud de acceso en la base de datos.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	  			La solicitud de acceso</a> solicitada.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/solicitudAcceso/encontrarPorNif">/solicitudAcceso/encontrarPorNif</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>nif</u>:  Obligatorio. El nif del solicitante.<br/>
					
						<u>eventoId</u>:  Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>La solicitud de acceso asociada al nif y el evento.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/solicitudAcceso/encontrar">/solicitudAcceso/encontrar</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>hashSolicitudAccesoHex</u>:  Obligatorio. Hash en formato hexadecimal asociado
	         a la solicitud de acceso.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>La solicitud de acceso asociada al hash.</p>
			
			</div>
		<HR>
	

</div>


