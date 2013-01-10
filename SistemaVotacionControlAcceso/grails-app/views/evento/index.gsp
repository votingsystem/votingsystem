<u><h3>Eventos</h3></u>
<div>
  	<p>Servicios relacionados con los <b>Eventos</b>.</p>
  	<h3>Métodos soportados</h3>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/evento/obtener">/evento/obtener</a><br/>
         <b>Respuesta:</b><br/>
         Lista en formato JSON con los <b>Eventos</b> dados de alta en el sistema.
    </p>
     	<HR>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/evento/guardarAdjuntandoValidacion">/evento/guardarAdjuntandoValidacion</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: Documento en formato <b>S/MIME</b> firmado por un <b>DNI electrónico</b> con 
       el <b>Evento de Votación</b> que se desea dar de alta en el sistema.<br/>
      <b>Respuesta:</b><br/>
         Si todo es correcto devolverá la solicitud firmada con el certificado del servidor.
    </p>
       <HR>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/evento/guardarCancelacion">/evento/guardarCancelacion</a><br/>
    Método que sirve para cancelar un evento.<br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: Documento en formato <b>S/MIME</b> firmado por el <b>DNI electrónico</b> de un administrador del sistema o del usuario
       	que publicó el documento.<br/>
      <b>Respuesta:</b><br/>
         Si todo es correcto devolverá un código de estado 200.
    </p>
      	<HR>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/evento/estadisticas">/evento/estadisticas</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador del evento.<br/>
    <b>Respuesta:</b><br/>
         Lista en formato JSON de información de interés sobre el <b>eventos</b> consultado.
    </p>
      	<HR>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/obtenerVotos">/eventoVotacion/obtenerVotos</a><br/>
         Método que devuelve la información sobre los votos recogidos por un evento<br/>
         <b>Parámetros:</b><br/>
         - <u>controlAccesoServerURL</u>: La URL del <b>Control de Acceso</b> en el que el evento fue dado de alta.<br/>
         - <u>eventoVotacionId</u>: El identificador del Evento de Votación.<br/>         
         <b>Respuesta:</b><br/>
         Lista en formato JSON con los <b>votos</b> asociados a la consulta.
    </p>  
      	<HR>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/evento/guardarSolicitudCopiaRespaldo">/evento/guardarSolicitudCopiaRespaldo</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: Solicitud firmada.<br/>
      <b>Respuesta:</b><br/>
         Un fichero zip con la copia de respaldo de los archivos generados para un determinado evento.
    </p>
      	<HR>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/evento/informacionVotos">/evento/informacionVotos</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador del evento.<br/>
    <b>Respuesta:</b><br/>
         Lista en formato JSON con los votos que ha recibido el evento.
    </p>    
      	<HR>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/evento/informacionFirmas">/evento/informacionFirmas</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador del evento.<br/>
    <b>Respuesta:</b><br/>
         Lista en formato JSON con las firmas que ha recibido el evento.
    </p>   
      	<HR>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/evento/informacionFirmasReclamacion">/evento/informacionFirmasReclamacion</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador del evento.<br/>
    <b>Respuesta:</b><br/>
         Lista en formato JSON con las firmas que ha recibido una determinada reclamación.
    </p>    
  	<HR>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/comprobarFechas">/eventoVotacion/comprobarFechas</a><br/>
         Método que devuelve el estado de un evento<br/>
         <b>Parámetros:</b><br/>
         - <u>id</u>: El id del evento que se desea comprobar.<br/>     
         <b>Respuesta:</b><br/>
         Cadena que representa el estado del evento
    </p>  
    <HR/> 	  	
</div>