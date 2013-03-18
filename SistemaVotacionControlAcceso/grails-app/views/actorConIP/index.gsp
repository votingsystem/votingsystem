

<u><h3 class="controllerInfoHeader">Actores de la aplicación</h3></u>


	 Servicios relacionados con la gestión de los actores que 
  intervienen en la aplicación.
 
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/actorConIP/index">/actorConIP/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/actorConIP'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/actorConIP/guardarSolicitudDesactivacion">/actorConIP/guardarSolicitudDesactivacion</a><br/>
				
	  Servicio que da de baja un actor.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Archivo firmado en formato SMIME con los datos del
	  		  actor que se desea dar de baja.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Si todo es correcto devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/actorConIP/guardarSolicitudActivacion">/actorConIP/guardarSolicitudActivacion</a><br/>
				
	  Servicio que da de alta un actor.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Archivo firmado en formato SMIME con los datos del
	  		  actor que se desea dar de baja.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Si todo es correcto devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	

</div>


