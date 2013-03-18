

<u><h3 class="controllerInfoHeader">Subscripciones</h3></u>


	 Servicios relacionados con los feeds generados por la aplicación y
  				   con la asociación de Controles de Acceso.
  
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/subscripcion/index">/subscripcion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/subscripcion'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/subscripcion/votaciones">/subscripcion/votaciones</a><br/>
				
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>feedType</u>:  Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92, 
	  			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información en el formato solicitado sobre las votaciones publicadas.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/subscripcion/guardarAsociacionConControlAcceso">/subscripcion/guardarAsociacionConControlAcceso</a><br/>
				
	  Servicio que da de alta Controles de Acceso.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Obligatorio. Archivo con los datos del control de acceso que se desea dar de alta.<br/>
					
				</p>
			
			</p>
	
			
			</div>
		<HR>
	

</div>


