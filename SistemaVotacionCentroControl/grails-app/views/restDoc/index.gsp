
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





<h3 class="controllerInfoHeader"><u>Anulación de Votos</u></h3>

 Servicios que permiten anular los votos de una votación
 
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/anuladorVoto/guardar">/anuladorVoto/guardar</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">El anulador de voto</a><br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Recibo firmado con el certificado del servidor</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/anuladorVoto/index">/anuladorVoto/index</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/anuladorVoto'</p>
			
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
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Parámetros de configuración de Hibernate</p>
			
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
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/app'</p>
			
			</div>
		<HR >
	
		
		
			<p>
				
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/app/prueba">/app/prueba</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
				<p>
					<b>Tipos de contenido:</b><br/>
					
						<u>pdf</u>: blim blim vlim<br/>
					
				</p>
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/app'</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>applet</u></h3>



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
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/applet/herramientaValidacion">/applet/herramientaValidacion</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Página HTML que sirve para cargar el Applet principal de la herramienta de validación
	  		   de archivos firmados y de copias de seguridad.</p>
			
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
					
						<u>consulta</u>: Obligatorio. Documento JSON con los parámetros de la consulta:<br/><code>
	  		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code><br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con la lista de votaciones que cumplen el criterio de la búsqueda.</p>
			
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
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/certificado/cadenaCertificacion">/certificado/cadenaCertificacion</a><br/>
				
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>La cadena de certificación del servidor</p>
			
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
					
						<u>controlAccesoId</u>: el identificador en la base de datos del control de acceso en el 
	  		  que se publicó la votación.<br/>
					
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
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Votaciones</u></h3>

 Servicios relacionados con las votaciones publicadas en el servidor.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/comprobarFechas">/eventoVotacion/comprobarFechas</a><br/>
				
	  Servicio que comprueba las fechas de una votación
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Obligatorio. El identificador de la votación en la base de datos.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/estadisticas">/eventoVotacion/estadisticas</a><br/>
				
	  Servicio que ofrece datos de recuento de una votación.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>controlAccesoServerURL</u>: Obligatorio. URL del Control de Acceso en el que se publicó el documento<br/>
					
						<u>eventoVotacionId</u>: Obligatorio. Identificador de la votación en la base de datos
	                           del Control de Acceso<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con estadísticas de la votación solicitada.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/guardarCancelacion">/eventoVotacion/guardarCancelacion</a><br/>
				
	  Servicio de cancelación de votaciones 
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Archivo con los datos de la votación que se desea cancelar 
	  			firmado por el Control de Acceso que publicó la votación y por
	  			el usuario que la publicó o un administrador de sistema.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/guardarEvento">/eventoVotacion/guardarEvento</a><br/>
				
	  Servicio que da de alta las votaciones.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Archivo con los datos de la votación firmado
	  		  por el usuario que la publica y el Control de Acceso en la que se publica.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
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
				
	  Servicio de consulta de las votaciones publicadas.
	  
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>id</u>: Opcional. El identificador en la base de datos del documento que se
	  			  desee consultar.<br/>
					
						<u>max</u>: Opcional (por defecto 20). Número máximo de documentos que
	  		  devuelve la consulta (tamaño de la página).<br/>
					
						<u>offset</u>: Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con las votaciones que cumplen el criterio de búsqueda.</p>
			
			</div>
		<HR >
	
		
		
			<p>
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/eventoVotacion/obtenerVotos">/eventoVotacion/obtenerVotos</a><br/>
				
	  Servicio de consulta de los votos
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>controlAccesoServerURL</u>: Obligatorio. URL del Control de Acceso en el que se publicó el documento.<br/>
					
						<u>eventoVotacionId</u>: Obligatorio. Identificador de la votación en la base de datos 
	                           del Control de Acceso.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con la lista de votos recibidos por la votación solicitada.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Información de la aplicación</u></h3>

 Servicios que ofrecen datos sobre la aplicación
 
  

<div>

	<HR>
	
		
		
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
		<HR color='#4c4c4c'>
	

</div>




<h3 class="controllerInfoHeader"><u>Subscripciones</u></h3>

 Servicios relacionados con los feeds generados por la aplicación y
  				   con la asociación de Controles de Acceso.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/subscripcion/guardarAsociacionConControlAcceso">/subscripcion/guardarAsociacionConControlAcceso</a><br/>
				
	  Servicio que da de alta Controles de Acceso.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. Archivo con los datos del control de acceso que se desea dar de alta.<br/>
					
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




<h3 class="controllerInfoHeader"><u>Servicio de Votos</u></h3>

 Servicio que procesa los votos recibidos.
  
  

<div>

	<HR>
	
		
		
			<p>
				- <u>POST</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/voto/guardarAdjuntandoValidacion">/voto/guardarAdjuntandoValidacion</a><br/>
				
	  Servicio que recoge los votos enviados por los usuarios.
	 
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>archivoFirmado</u>: Obligatorio. El voto firmado por el 
	         <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a><br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a></p>
			
			</div>
		<HR >
	
		
		
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
				- <u>GET</u> - 
				<a href="${request.scheme}://${request.serverName}:${request.serverPort}${request.getContextPath()}/voto/obtener">/voto/obtener</a><br/>
				
	  Servicio que devuelve la información de un voto a partir del  
	  hash asociado al mismo
	  <br/>
			</p>
			<div class="params_result_div">
			
			
			
			
			
			
				<p>
					<b>Parámetros:</b><br/>
					
						<u>hashCertificadoVotoHex</u>: Obligatorio. Hash en hexadecimal asociado al voto.<br/>
					
				</p>
			
			
			
			
			
			
			 
			
			
				<p><b>Respuesta:</b><br/>Documento JSON con la información del voto solicitado.</p>
			
			</div>
		<HR color='#4c4c4c'>
	

</div>

    
    </body>
</html>