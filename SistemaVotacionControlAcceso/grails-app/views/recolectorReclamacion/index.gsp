

<u><h3 class="controllerInfoHeader">Recogida de reclamaciones</h3></u>


	 Servicios relacionados con la recogida de reclamaciones.
 
  


<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/recolectorReclamacion/index">/recolectorReclamacion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/recolectorReclamacion'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${grailsApplication.config.grails.serverURL}/recolectorReclamacion/guardarAdjuntandoValidacion">/recolectorReclamacion/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio que valida reclamaciones recibidas en documentos SMIME
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>:  Obligatorio. Documento SMIME firmado con la reclamación.<br/>
					
				</p>
			
			</p>
	
			
				<p><b>Respuesta:</b><br/>El archivo SMIME recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR>
	

</div>


