<u><h3>Controlador gestor de servicios de copias de seguridad</h3></u>
<div>
  	<p>Servicio que gestiona el mecanismo de copias de seguridad de la aplicación.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/${params.controller}/obtener">
    					/${params.controller}/obtener</a><br/>
    	<b>Parámetros:</b><br/>
    	- <u>id</u>: El identificador de la solicitud de la copia que se está tramitando.<br/>
	    <b>Respuesta:</b><br/>
	    Devuelve el archivo zip con la copia de seguridad solicitada por el usuario.
    </p>   
    <hr/>  	
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/${params.controller}/validarSolicitud">
    					/${params.controller}/validarSolicitud</a><br/>
      <b>Parámetros:</b><br/>
      - <u>archivoFirmado</u>:PDF firmado electrónicamente con los datos de la solicitud.<br/>
      <b>Respuesta:</b><br/>
         Si todo es correcto devolverá un código de estado HTTP 200 y enviará 
         las instrucciones de descarga a la dirección de correo proporcionada por el usuario.
    </p>
    <hr/>  
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/${params.controller}/obtenerSolicitud">
    					/${params.controller}/obtenerSolicitud</a><br/>
    	<b>Parámetros:</b><br/>
    	- <u>id</u>: El identificador de la solicitud de la copia que se quiere consultar.<br/>
	    <b>Respuesta:</b><br/>
	    Devuelve el archivo PDF de la solicitud firmada por el usuario.
    </p>    
     <hr/>  
</div>

