<u><h3>Eventos para recoger votos</h3></u>
<div>
  	<p>Servicios relacionados con los <b>Eventos para recoger voto</b>.</p>
  	<h3>Métodos soportados</h3>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/obtener">/eventoVotacion/obtener</a><br/>
         <b>Respuesta:</b><br/>
         Lista en formato JSON con los <b>Eventos</b> dados de alta en el sistema.
    </p>  	
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/guardarAdjuntandoValidacion">/eventoVotacion/guardarAdjuntandoValidacion</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: El <b>Evento de Votación</b> que se desea dar de alta en el sistema.<br/>
      <b>Respuesta:</b><br/>
         Si todo es correcto devolverá la solicitud firmada con el certificado del servidor.
    </p>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/estadisticas">/eventoVotacion/estadisticas</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador del evento.<br/>
    <b>Respuesta:</b><br/>
         Documento en formato JSON con los datos de una votación.
    </p> 
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/guardarSolicitudCopiaRespaldo">/eventoVotacion/guardarSolicitudCopiaRespaldo</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: Solicitud firmada.<br/>
      <b>Respuesta:</b><br/>
         Un fichero zip con la copia de respaldo de los archivos generados para un determinado evento.
    </p>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/validado">/eventoVotacion/validado</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador del evento.<br/>
    <b>Respuesta:</b><br/>
         Mensaje <b>S/MIME</b> con los datos del evento firmado por el servidor y el usuario.
    </p> 
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/firmado">/eventoVotacion/firmado</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador del evento.<br/>
    <b>Respuesta:</b><br/>
         Mensaje <b>S/MIME</b> con los datos del evento firmado por el usuario.
    </p>     
  	<HR>
</div>