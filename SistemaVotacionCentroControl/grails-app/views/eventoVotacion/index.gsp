<u><h3>Eventos Votación</h3></u>
<div>
  	<p>Servicios relacionados con los <b>Eventos de votación</b>.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/inicializarEvento">/eventoVotacion/inicializarEvento</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: El <b>Evento de Votación</b> que se desea dar de alta en el sistema.<br/>
         <b>Respuesta:</b><br/>
         Si todo es correcto devolverá una respuesta HTTP con código de estado 200.
    </p>
   <HR/>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/obtener">/eventoVotacion/obtener</a><br/>
         <b>Respuesta:</b><br/>
         Lista en formato JSON con los <b>Eventos de Votación</b> dados de alta en el sistema.
    </p>
    <HR/>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/obtenerVotos">/eventoVotacion/obtenerVotos</a><br/>
         Método que devuelve la información sobre los votos recogidos por un evento<br/>
         <b>Parámetros:</b><br/>
         - <u>controlAccesoServerURL</u>: La URL del <b>Control de Acceso</b> en el que el evento fue dado de alta.<br/>
         - <u>eventoVotacionId</u>: El identificador del Evento de Votación.<br/>         
         <b>Respuesta:</b><br/>
         Lista en formato JSON con los <b>votos</b> asociados a la consulta.
    </p> 
    <HR/>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/estadisticas">/eventoVotacion/estadisticas</a><br/>
	<b>Parámetros:</b><br/>
	Para identificar el evento consultado debe proporcionar el id del mismo en la base de datos del <b>Centro de Control</b>, o <br/>
	la url del <b>Control de Acceso</b> y el id en la base de datos del <b>Control de Acceso</b>. <br/>
    - <u>id</u>: El identificador del evento en la base de datos del centro de control.<br/>
    - <u>eventoVotacionId</u>: El identificador del evento en la base de datos del <b>Centro de Control</b>.<br/>
    - <u>controlAccesoServerURL</u>:La url del <b>Control de Acceso</b>.<br/>
    <b>Respuesta:</b><br/>
         Documento en formato JSON con las estadísitcas de una votación.
    </p>     
  	<HR/>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/eventoVotacion/comprobarFechas">/eventoVotacion/comprobarFechas</a><br/>
         Método que devuelve el estado de un evento<br/>
         <b>Parámetros:</b><br/>
         - <u>id</u>: El id del evento que se desea comprobar.<br/>     
         <b>Respuesta:</b><br/>
         Cadena que representa el estado del evento
    </p>  
    <HR/> 	
</div>