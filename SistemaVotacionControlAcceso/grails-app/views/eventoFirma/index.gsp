<u><h3>Eventos para recoger firmas</h3></u>
<div>
  	<p>Servicios relacionados con los <b>Eventos para recoger firmas</b>.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoFirma/guardarAdjuntandoValidacion">/eventoFirma/guardarAdjuntandoValidacion</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: El <b>Evento de Votación</b> que se desea dar de alta en el sistema.<br/>
      <b>Respuesta:</b><br/>
         Si todo es correcto devolverá la solicitud firmada con el certificado del servidor.
    </p>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoFirma/estadisticas">/eventoFirma/estadisticas</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador del evento.<br/>
    <b>Respuesta:</b><br/>
         Lista en formato JSON de información de interés sobre el <b>eventos</b> consultado.
    </p> 
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoFirma/guardarSolicitudCopiaRespaldo">/eventoFirma/guardarSolicitudCopiaRespaldo</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: Solicitud firmada.<br/>
      <b>Respuesta:</b><br/>
         Un fichero zip con la copia de respaldo de los archivos generados para un determinado evento.
    </p>
  	<HR>
</div>