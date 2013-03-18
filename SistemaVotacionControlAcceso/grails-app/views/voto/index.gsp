

<u><h3 class="controllerInfoHeader">Servicio de Votos</h3></u>


	 Servicio que procesa los votos recibidos.
 
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/voto/index">/voto/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/voto'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/voto/procesar">/voto/procesar</a><br/>
				
	  Servicio que recoge los votos enviados por lo Centrols de Control
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: 	Obligatorio. El voto firmado por el
	         <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a>
	         y el certificado del Centro de Control.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a></p>
			
			</div>
		<HR>
	

</div>


