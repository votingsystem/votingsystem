

<u><h3 class="controllerInfoHeader">Anulación de votos</h3></u>


	 Servicios relacionados con la anulación de votos.
 
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/anuladorVoto/index">/anuladorVoto/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/anuladorVoto'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/anuladorVoto/guardarAdjuntandoValidacion">/anuladorVoto/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio que anula votos.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  El <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">anulador de voto</a>.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR>
	

</div>


