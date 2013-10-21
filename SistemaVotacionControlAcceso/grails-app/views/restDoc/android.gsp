

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

<h3 class="controllerInfoHeader"><u>Android app Controller</u></h3>

 Controlador que sirve la aplicación para clientes Android.
 
  

<div>

	<HR>
	
		
		
			<p>
				
	  Este servicio surgió para resolver un problema que surgió en un servicio de hosting.
	  No era posible acceder al archivo de la aplicación diréctamente.
	  
	  <br/>
				
					- <u>GET</u> - 
					<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/android/app">/android/app</a><br/>
				
			</p>
			<div class="params_result_div">
			
			
			
			
			
						
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La aplicación de votación para clientes Android.</p>
			
			</div>
		<HR>
	
		
		
			<p>
				
	  <br/>
				
					- <u>GET</u> - 
					<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/android/index">/android/index</a><br/>
				
			</p>
			<div class="params_result_div">
			
			
			
			
			
						
			
			
			
			
			
				
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/actorConIP'.</p>
			
			</div>
		<HR>
	

</div>