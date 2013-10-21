

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

<h3 class="controllerInfoHeader"><u>Anulación de votos</u></h3>

 Servicios relacionados con la anulación de votos.
 
  

<div>

	<HR>
	
		
		
			<p>
				
	  Servicio que devuelve la información de la anulación de un voto a partir del
	  identifiacador del voto en la base de datos
	  <br/>
				
					- <u>GET</u> - 
					<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/anuladorVoto/voto/${id}">/anuladorVoto/voto/${id}</a><br/>
				
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. Identificador del voto en la base de datos<br/>
					
				</p>
			
			
						
			
			
			
				<p>
					<b>Tipo de contenido en las respuestas:</b><br/>
					
						<u>application/json</u>: <br/>
					
				</p>
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con la información del voto solicitado.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				
	  Servicio de consulta de votos anulados.
	 
	  <br/>
				
					- <u>GET</u> - 
					<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/anuladorVoto/$hashHex">/anuladorVoto/$hashHex</a><br/>
				
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>hashHex</u>: El hash en Hexadecimal del certificado del voto anulado.<br/>
					
				</p>
			
			
						
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La anulación de voto firmada por el usuario.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				
	  Servicio que anula votos.
	  
      <br/>
				
					- <u>POST</u> - 
					<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/anuladorVoto">/anuladorVoto</a><br/>
				
			</p>
			<div class="params_result_div">
			
			
			
			
			
						
			
				<p>
					<b>Tipo de contenido en las peticiones:</b><br/>
					
						<u>application/x-pkcs7-signature,application/x-pkcs7-mime</u>: <br/>
					
				</p>
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor. La respuesta viaja cifrada.</p>
			
			</div>
		<HR>
	

</div>