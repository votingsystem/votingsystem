

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

<h3 class="controllerInfoHeader"><u>Recogida de firmas</u></h3>

 Servicios relacionados con la recogida de firmas.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/recolectorFirma/guardarAdjuntandoValidacion">/recolectorFirma/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio que valida firmas recibidas en documentos SMIME
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Documento SMIME firmado.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El archivo SMIME recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/recolectorFirma/index">/recolectorFirma/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/recolectorFirma'.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/recolectorFirma/validarPDF">/recolectorFirma/validarPDF</a><br/>
				
	  Servicio que valida firmas recibidas en documentos PDF
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador en la base de datos del manifiesto que se está firmando.<br/>
					
						<u>signedPDF</u>: Obligatorio. PDF con el documento firmado.<br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR>
	

</div>