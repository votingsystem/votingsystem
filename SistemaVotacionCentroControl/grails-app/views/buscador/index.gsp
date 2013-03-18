

<u><h3 class="controllerInfoHeader">Búsquedas</h3></u>


	 Servicios de búsqueda sobre los datos generados por la aplicación
 
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/buscador/index">/buscador/index</a><br/>
				
	 <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/buscador'</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/buscador/consultaJSON">/buscador/consultaJSON</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>consulta</u>:  Documento JSON con los parámetros de la consulta:<br/><code>
	  		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code><br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Una lista en formato JSON con los documentos que cumplen el criterio de la búsqueda.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/buscador/reindex">/buscador/reindex</a><br/>
				
     (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
     Servicio que reindexa el motor de búsqueda
	 <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/buscador/evento">/buscador/evento</a><br/>
				
	  Servicio que busca la cadena de texto recibida entre las votaciones publicadas.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>max</u>:  Opcional (por defecto 20). Número máximo de documentos que 
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>consultaTexto</u>:  Obligatorio. Texto de la búsqueda.<br/>
					
						<u>offset</u>:  Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			</p>
	
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/buscador/guardarReindex">/buscador/guardarReindex</a><br/>
				
	 Servicio que reindexa los datos del motor de búsqueda
	 <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Obligatorio. Solicitud firmada por un administrador de sistema.<br/>
					
				</p>
			
			</p>
	
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/buscador/eventoPorEtiqueta">/buscador/eventoPorEtiqueta</a><br/>
				
	  Servicio que busca los eventos que tienen la etiqueta que se
	  pasa como parámetro.
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>max</u>:  Opcional (por defecto 20). Número máximo de documentos que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>offset</u>:  Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
						<u>etiqueta</u>:  Obligatorio. Texto de la etiqueta.<br/>
					
				</p>
			
			</p>
	
			
			</div>
		<HR>
	

</div>


