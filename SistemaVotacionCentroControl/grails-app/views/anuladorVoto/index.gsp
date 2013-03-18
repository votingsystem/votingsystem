

<u><h3 class="controllerInfoHeader">Anulación de Votos</h3></u>


	 Servicios que permiten anular los votos de una votación
 
  


<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/anuladorVoto/guardar">/anuladorVoto/guardar</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">El anulador de voto</a><br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Recibo firmado con el certificado del servidor</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/anuladorVoto/index">/anuladorVoto/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/anuladorVoto'</p>
			
			</div>
		<HR>
	

</div>


