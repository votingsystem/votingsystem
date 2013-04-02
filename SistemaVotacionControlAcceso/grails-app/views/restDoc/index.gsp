
<!DOCTYPE html>
<html>
    <head>
        <title>API REST</title>
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
    </head>
    <body>





<h3 class="controllerInfoHeader"><u>Actores de la aplicación</u></h3>

 Servicios relacionados con la gestión de los actores que 
  intervienen en la aplicación.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/actorConIP/guardarSolicitudActivacion">/actorConIP/guardarSolicitudActivacion</a><br/>
				
	  Servicio que da de alta un actor.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos del
	  		  actor que se desea dar de baja.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo es correcto devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/actorConIP/guardarSolicitudDesactivacion">/actorConIP/guardarSolicitudDesactivacion</a><br/>
				
	  Servicio que da de baja un actor.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos del
	  		  actor que se desea dar de baja.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo es correcto devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/actorConIP/index">/actorConIP/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/actorConIP'.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Android app Controller</u></h3>

 Controlador que sirve la aplicación para clientes Android.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/android/app">/android/app</a><br/>
				
	  Este servicio surgió para resolver un problema que surgió en un servicio de hosting.
	  No era posible acceder al archivo de la aplicación diréctamente.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La aplicación de votación para clientes Android.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/android/index">/android/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/actorConIP'.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Anulación de votos</u></h3>

 Servicios relacionados con la anulación de votos.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/anuladorVoto/guardarAdjuntandoValidacion">/anuladorVoto/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio que anula votos.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: El <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">anulador de voto</a>.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/anuladorVoto/index">/anuladorVoto/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/anuladorVoto'.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Aplicación</u></h3>

 Servicios de acceso a la aplicación web principal
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/app/clienteAndroid">/app/clienteAndroid</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La página principal de la aplicación web de votación con parámetros de utilidad
	  		   para una sesión con cliente Android.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/app/editor">/app/editor</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Página que sirve el editor de documentos que se emplea en las aplicaciones Android.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/app/home">/app/home</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La página principal de la aplicación web de votación.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/app/index">/app/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/app'.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Applet</u></h3>

 Servicios relacionados con los applets de la aplicación.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/applet/cliente">/applet/cliente</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Página HTML que sirve para cargar el Applet principal de firma.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/applet/index">/applet/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/applet'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/applet/jnlpCliente">/applet/jnlpCliente</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo JNLP con los datos del Applet principal de firma.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/applet/jnlpHerramienta">/applet/jnlpHerramienta</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo JNLP con los datos del Applet de la herramienta de validación de archivos firmados
	          y copias de seguridad.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Búsquedas</u></h3>

 Servicios de búsqueda sobre los datos generados por la aplicación
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/buscador/consultaJSON">/buscador/consultaJSON</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>consulta</u>: Documento JSON con los parámetros de la consulta:<br/><code>
	  		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code><br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con la lista de eventos que cumplen el criterio de la búsqueda.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/buscador/evento">/buscador/evento</a><br/>
				
	  Servicio que busca la cadena de texto recibida entre las votaciones publicadas.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de documentos que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>consultaTexto</u>: Obligatorio. Texto de la búsqueda.<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/buscador/eventoPorEtiqueta">/buscador/eventoPorEtiqueta</a><br/>
				
	  Servicio que busca los eventos que tienen la etiqueta que se
	  pasa como parámetro.
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de documentos que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
						<u>etiqueta</u>: Obligatorio. Texto de la etiqueta.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/buscador/guardarReindex">/buscador/guardarReindex</a><br/>
				
	 Servicio que reindexa los datos del motor de búsqueda
	 <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Solicitud firmada por un administrador de sistema.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/buscador/index">/buscador/index</a><br/>
				
	 <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/buscador'</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/buscador/reindex">/buscador/reindex</a><br/>
				
	 (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	 Servicio que reindexa el motor de búsqueda
	 <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Servicio de Certificados</u></h3>

 Servicios relacionados con los certificados manejados por la aplicación
 
 

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/addCertificateAuthority">/certificado/addCertificateAuthority</a><br/>
				
	  (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). Servicio que añade Autoridades de Confianza.<br/>
	  Sirve para poder validar los certificados enviados en las simulaciones.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>pemCertificate</u>: certificado en formato PEM de la Autoridad de Confianza que se desea añadir.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/cadenaCertificacion">/certificado/cadenaCertificacion</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La cadena de certificación en formato PEM del servidor</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/certificadoCA_DeEvento">/certificado/certificadoCA_DeEvento</a><br/>
				
	  Servicio de consulta de los certificados emisores de certificados
	  de voto para una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>idEvento</u>: el identificador de la votación que se desea consultar.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Devuelve la cadena de certificación, en formato PEM, con la que se generan los
	  			certificados de los votos.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/certificadoDeVoto">/certificado/certificadoDeVoto</a><br/>
				
	  Servicio de consulta de certificados de voto.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>hashCertificadoVotoHex</u>: Obligatorio. Hash en hexadecimal asociado al
	           certificado de voto que se desea consultar.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El certificado en formato PEM.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/certificadoUsuario">/certificado/certificadoUsuario</a><br/>
				
	  Servicio de consulta de certificados de usuario.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>usuarioId</u>: Obligatorio. El identificador en la base de datos del usuario.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El certificado en formato PEM.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/index">/certificado/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/certificado'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/trustedCerts">/certificado/trustedCerts</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Los certificados en formato PEM de las Autoridades Certificadoras en las que
	          confía la aplicación.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Validación de solicitudes de certificación</u></h3>

 Servicios relacionados con validación y firma de certificados.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/csr/anular">/csr/anular</a><br/>
				
	  (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS).
	  
	  Servicio que anula solicitudes de certificación de usuario.<br/>
	  
	  TODO - IMPLEMETAR. Hacer las validaciones sólo sobre solicitudes 
	  firmadas electrónicamente por personal dado de alta en la base de datos.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>userIfo</u>: Documento JSON con los datos del usuario 
	  <code>{deviceId:"000000000000000", telefono:"15555215554", nif:"1R" }</code><br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo es correcto devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/csr/guardarValidacion">/csr/guardarValidacion</a><br/>
				
	  (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS).
	 
	  Servicio que firma solicitudes de certificación de usuario.<br/>
	 
	  TODO - Hacer las validaciones sólo sobre solicitudes firmadas electrónicamente
	  por personal dado de alta en la base de datos.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME en cuyo contenido 
	  se encuentran los datos de la solicitud que se desea validar.
	  <code>{deviceId:"000000000000000", telefono:"15555215554", nif:"1R" }</code><br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo es correcto devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/csr/index">/csr/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/csr'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/csr/obtener">/csr/obtener</a><br/>
				
	  Servicio que devuelve las solicitudes de certificados firmadas una vez que
	  se ha validado la identidad del usuario.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>idSolicitudCSR</u>: Identificador de la solicitud de certificación enviada previamente por el usuario.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si el sistema ha validado al usuario devuelve la solicitud de certificación firmada.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/csr/solicitar">/csr/solicitar</a><br/>
				
	  (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS)<br/>
	  Servicio para la creación de certificados de usuario.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>csr</u>: Solicitud de certificación con los datos de usuario.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo es correcto devuelve un código de estado HTTP 200 y el identificador 
	          de la solicitud en la base de datos.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/csr/validar">/csr/validar</a><br/>
				
	  (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS).
	  
	  Servicio que firma solicitudes de certificación de usuario.<br/>
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>userIfo</u>: Documento JSON con los datos del usuario 
	  <code>{deviceId:"000000000000000", telefono:"15555215554", nif:"1R" }</code><br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo es correcto devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Documentos</u></h3>

 Servicios relacionados con PDFs.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/documento/index">/documento/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/documento'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/documento/obtenerFirmaManifiesto">/documento/obtenerFirmaManifiesto</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: El identificador del documento en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El documento PDF asociado al identificador.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/documento/obtenerManifiesto">/documento/obtenerManifiesto</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: El identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El manifiesto en formato PDF.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Eventos</u></h3>

 Servicios relacionados con los eventos del sistema.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/comprobarFechas">/evento/comprobarFechas</a><br/>
				
	 Servicio que comprueba las fechas de un evento
	
	 <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador del evento en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/estadisticas">/evento/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a un evento.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Identificador en la base de datos del evento que se desea consultar.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con estadísticas asociadas al evento consultado.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/guardarAdjuntandoValidacion">/evento/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio para publicar votaciones.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME en cuyo contenido se
	         encuentra la votación que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/guardarCancelacion">/evento/guardarCancelacion</a><br/>
				
	  Servicio que cancela eventos
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Archivo con los datos del evento que se desea 
	  cancelar firmado por el usuario que publicó o un administrador de sistema.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/guardarSolicitudCopiaRespaldo">/evento/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve los archivos relacionados con un evento.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos del
	  		  evento origen de la copia de seguridad.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo zip con los archivos relacionados con un evento.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/index">/evento/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/evento'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/informacionFirmas">/evento/informacionFirmas</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una acción de recogida de firmas
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con información sobre las firmas recibidas por el manifiesto solicitado.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/informacionFirmasReclamacion">/evento/informacionFirmasReclamacion</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una acción de reclamación
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la reclamación la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con información sobre las firmas recibidas por la reclamación solicitada.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/informacionVotos">/evento/informacionVotos</a><br/>
				
	  Servicio que devuelve información sobre la actividad de una votación
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con información sobre los votos y solicitudes de acceso de una votación.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/evento/obtener">/evento/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Opcional. El identificador del evento en la base de datos. Si no se pasa ningún id 
	         la consulta se hará entre todos los eventos.<br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de documentos que 
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>: Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de creación.<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Manifiestos</u></h3>

 Servicios relacionados con la publicación de manifiestos.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/estadisticas">/eventoFirma/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a un manifiesto.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Identificador en la base de datos del manifiesto que se desea consultar.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con las estadísticas asociadas al manifiesto solicitado.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/guardarAdjuntandoValidacion">/eventoFirma/guardarAdjuntandoValidacion</a><br/>
				
	  (EN DESUSO)
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME en cuyo contenido se 
	         encuentra el manifiesto que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/guardarSolicitudCopiaRespaldo">/eventoFirma/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve todas las firmas recibidas por un manifiesto.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos del
	  		  manifiesto origen de la copia de seguridad.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo zip con todas las firmas que ha recibido el manifiesto consultado.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/index">/eventoFirma/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/eventoFirma'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/obtener">/eventoFirma/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con información del manifiesto solicitado.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/obtenerHtml">/eventoFirma/obtenerHtml</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: el identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El manifiesto en formato HTML.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/obtenerManifiestos">/eventoFirma/obtenerManifiestos</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de documentos que 
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>: Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de inicio.<br/>
					
						<u>estadoEvento</u>: Opcional, posibles valores 'ACTIVO','CANCELADO', 'FINALIZADO', 'PENDIENTE_COMIENZO'.
	  		               El estado de los eventos que se desea consultar.<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/obtenerPDF">/eventoFirma/obtenerPDF</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: El identificador del manifiesto en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El manifiesto en formato PDF.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/publicarPDF">/eventoFirma/publicarPDF</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>htmlManifest</u>: Manifiesto que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200 con el identificador
	  del nuevo manifiesto en la base de datos en el cuerpo del mensaje.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoFirma/validarPDF">/eventoFirma/validarPDF</a><br/>
				
	  Servicio que valida los manifiestos que se desean publicar. <br/>
	  La publicación de manifiestos se produce en dos fases. En la primera
	  se envía a '/eventoFirma/publicarPDF' el manifiesto en formato HTML, el servidor 
	  lo valida y si todo es correcto genera el PDF y envía al programa cliente el identificador 
	  del manifiesto en la base de datos. El programa cliente puede descargarse con ese
	  identificador el PDF firmarlo y enviarlo a este servicio.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador en la base de datos del manifiesto.<br/>
					
						<u>signedPDF</u>: Obligatorio. PDF con el manifiesto que se desea publicar firmado
	         por el autor.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Reclamaciones</u></h3>

 Servicios relacionados con la publicación de reclamaciones.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/estadisticas">/eventoReclamacion/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a una reclamción.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. Identificador en la base de datos de la reclamación que se desea consultar.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con las estadísticas asociadas a la reclamación solicitada.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/firmado">/eventoReclamacion/firmado</a><br/>
				
	  Servicio que proporciona una copia de la reclamación publicada.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la reclamación en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo SMIME de la publicación de la reclamación firmada por el usuario 
	          que la publicó.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/guardarAdjuntandoValidacion">/eventoReclamacion/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio para publicar votaciones.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME en cuyo contenido se
	         encuentra la reclamación que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/guardarSolicitudCopiaRespaldo">/eventoReclamacion/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve las firmas recibidas por una reclamación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos de la
	  		  reclamación origen de la copia de seguridad.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo zip con los archivos relacionados con la reclamación.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/index">/eventoReclamacion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/eventoReclamacion'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/obtener">/eventoReclamacion/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Opcional. El identificador de la reclamación en la base de datos. Si no se pasa ningún 
	         id la consulta se hará entre todos las reclamaciones.<br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de reclamaciones que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>: Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de inicio.<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoReclamacion/validado">/eventoReclamacion/validado</a><br/>
				
	  Servicio que proporciona una copia de la reclamación publicada con la firma
	  añadida del servidor.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la reclamación en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo SMIME de la publicación de la reclamación firmada por el usuario que
	          la publicó y el servidor.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Votaciones</u></h3>

 Servicios relacionados con la publicación de votaciones.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/estadisticas">/eventoVotacion/estadisticas</a><br/>
				
	  Servicio que devuelve estadísticas asociadas a una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. Identificador en la base de datos de la votación que se desea consultar.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con las estadísticas asociadas a la votación solicitada.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/firmado">/eventoVotacion/firmado</a><br/>
				
	  Servicio que proporciona una copia de la votación publicada.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo SMIME de la publicación de la votación firmada por el usuario
	          que la publicó.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/guardarAdjuntandoValidacion">/eventoVotacion/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio para publicar votaciones.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME en cuyo contenido se
	         encuentra la votación que se desea publicar en formato HTML.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/guardarSolicitudCopiaRespaldo">/eventoVotacion/guardarSolicitudCopiaRespaldo</a><br/>
				
	  Servicio que devuelve los votos y las solicitudes de acceso recibidas en una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Archivo firmado en formato SMIME con los datos de la
	  		  votación origen de la copia de seguridad.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo zip con los archivos relacionados con la votación.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/index">/eventoVotacion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/eventoVotacion'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/obtener">/eventoVotacion/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Opcional. El identificador de la votación en la base de datos. Si no se pasa ningún id
	         la consulta se hará entre todos las votaciones.<br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de votaciones que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>order</u>: Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	         resultados según la fecha de inicio.<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con las votaciones que cumplen con el criterio de búsqueda.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/validado">/eventoVotacion/validado</a><br/>
				
	  Servicio que proporciona una copia de la votación publicada con la firma
	  añadida del servidor.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo SMIME de la publicación de la votación firmada por el usuario que
	          la publicó y el servidor.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Información de la aplicación</u></h3>

 Servicios que ofrecen datos sobre la aplicación
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/infoServidor/centrosCertificacion">/infoServidor/centrosCertificacion</a><br/>
				
	  <br/><u>SERVICIO DE PRUEBAS - DATOS FICTICIOS</u>. El esquema actual de certificación en plataformas
	  Android pasa por que el usuario tenga que identificarse en un centro autorizado
	  para poder instalar en su dispositivo el certificado de identificación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Direcciones a las que tendrían que ir los usuarios para poder obtener un certificado
	  		   de identificación.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/infoServidor/datosAplicacion">/infoServidor/datosAplicacion</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Datos de las versiones de algunos componentes de la aplicación</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/infoServidor/index">/infoServidor/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/infoServidor'</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/infoServidor/informacion">/infoServidor/informacion</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información general de la aplicación</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/infoServidor/listaServicios">/infoServidor/listaServicios</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La lista de servicios de la aplicación</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/infoServidor/obtener">/infoServidor/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con datos de la aplicación</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Mensajes firmados</u></h3>

 Servicios relacionados con los mensajes firmados manejados por la
                  aplicación.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/mensajeSMIME/index">/mensajeSMIME/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/mensajeSMIME'</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/mensajeSMIME/obtener">/mensajeSMIME/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. Identificador del mensaje en la base de datos<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El mensaje solicitado.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/mensajeSMIME/obtenerReciboFirma">/mensajeSMIME/obtenerReciboFirma</a><br/>
				
	  Servicio que devuelve el recibo con el que respondió el servidor al publicar un manifiesto.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. Identificador del mensaje de publicación en la base de datos<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El recibo.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/mensajeSMIME/obtenerReciboReclamacion">/mensajeSMIME/obtenerReciboReclamacion</a><br/>
				
	  Servicio que devuelve el recibo con el que respondió el servidor al publicar una reclamación.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. Identificador del mensaje de publicación en la base de datos<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El recibo.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




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
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/recolectorFirma/index">/recolectorFirma/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/recolectorFirma'.</p>
			
			</div>
		<HR >
	
		
		
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
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Recogida de reclamaciones</u></h3>

 Servicios relacionados con la recogida de reclamaciones.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/recolectorReclamacion/guardarAdjuntandoValidacion">/recolectorReclamacion/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio que valida reclamaciones recibidas en documentos SMIME
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Documento SMIME firmado con la reclamación.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El archivo SMIME recibido con la firma añadida del servidor.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/recolectorReclamacion/index">/recolectorReclamacion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/recolectorReclamacion'.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Solicitudes de acceso</u></h3>

 Servicios relacionados con las solicitudes de acceso recibidas en una votación.
  
 

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudAcceso/encontrar">/solicitudAcceso/encontrar</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>hashSolicitudAccesoHex</u>: Obligatorio. Hash en formato hexadecimal asociado
	         a la solicitud de acceso.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La solicitud de acceso asociada al hash.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudAcceso/encontrarPorNif">/solicitudAcceso/encontrarPorNif</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>nif</u>: Obligatorio. El nif del solicitante.<br/>
					
						<u>eventoId</u>: Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La solicitud de acceso asociada al nif y el evento.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudAcceso/index">/solicitudAcceso/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/solicitudAcceso'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudAcceso/obtener">/solicitudAcceso/obtener</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Opcional. El identificador de la solicitud de acceso en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	  			La solicitud de acceso</a> solicitada.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudAcceso/procesar">/solicitudAcceso/procesar</a><br/>
				
	  Servicio que valida las <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	  solicitudes de acceso</a> recibidas en una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. La solicitud de acceso.<br/>
					
						<u>csr</u>: Obligatorio. La solicitud de certificado de voto.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La solicitud de certificado de voto firmada.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Solicitud de copias de seguridad</u></h3>

 Servicios que gestiona solicitudes de copias de seguridad.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudCopia/index">/solicitudCopia/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/solicitudCopia'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudCopia/obtener">/solicitudCopia/obtener</a><br/>
				
	  Servicio que proporciona la copia de seguridad a partir de la URL que se envía
	  al solicitante en el mail de confirmación que recibe al enviar la solicitud.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la solicitud de copia de seguridad la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Archivo zip con la copia de seguridad.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudCopia/obtenerSolicitud">/solicitudCopia/obtenerSolicitud</a><br/>
				
	  Servicio que proporciona copias de las solicitudes de copias de seguridad recibidas.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la solicitud de copia de seguridad la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El PDF en el que se solicita la copia de seguridad.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/solicitudCopia/validarSolicitud">/solicitudCopia/validarSolicitud</a><br/>
				
	  Servicio que recibe solicitudes de copias de seguridad
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>signedPDF</u>: Archivo PDF con los datos de la copia de seguridad.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo va bien devuelve un código de estado HTTP 200. Y el solicitante recibirá un
	          email con información para poder obtener la copia de seguridad.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Subscripciones</u></h3>

 Servicios relacionados con los feeds generados por la aplicación y
  				   con la asociación de Centros de Control.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/subscripcion/guardarAsociacionConCentroControl">/subscripcion/guardarAsociacionConCentroControl</a><br/>
				
	  Servicio que da de alta Centros de Control.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Archivo con los datos del Centro de Control que se desea dar de alta.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/subscripcion/index">/subscripcion/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/subscripcion'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/subscripcion/manifiestos">/subscripcion/manifiestos</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>feedType</u>: Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92,
	  			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información en el formato solicitado sobre los manifiestos publicados.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/subscripcion/reclamaciones">/subscripcion/reclamaciones</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>feedType</u>: Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92, 
	  			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información en el formato solicitado sobre las reclamaciones publicadas.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/subscripcion/votaciones">/subscripcion/votaciones</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>feedType</u>: Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92,
	  			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información en el formato solicitado sobre las votaciones publicadas.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>timeStamp</u></h3>



<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/timeStamp/getBySerialNumber">/timeStamp/getBySerialNumber</a><br/>
				
	  Servicio que devuelve un sello de tiempo previamente generado y guardado en la base de datos.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>serialNumber</u>: Número de serie del sello de tiempo.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>El sello de tiempo en formato RFC 3161.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/timeStamp/index">/timeStamp/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/timeStamp'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/timeStamp/obtener">/timeStamp/obtener</a><br/>
				
	  Servicio de generación de sellos de tiempo.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>timeStampRequest</u>: Solicitud de sellado de tiempo en formato RFC 3161.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Si todo es correcto un sello de tiempo en formato RFC 3161.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Servicio de Votos</u></h3>

 Servicio que procesa los votos recibidos.
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/voto/index">/voto/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/voto'.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/voto/procesar">/voto/procesar</a><br/>
				
	  Servicio que recoge los votos enviados por lo Centrols de Control
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. El voto firmado por el
	         <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a>
	         y el certificado del Centro de Control.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a></p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>

    
    </body>
</html>