<u><h3>Solicitudes de acceso</h3></u>
<div>
  	<p>Servicio que valida solicitudes y proporciona los
  	<a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Certificado-de-voto">certificados de voto</a>.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/solicitudAcceso/procesar">/solicitudAcceso/procesar</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Solicitud-de-acceso">solicitud de acceso</a>.<br/>
       - <u>csr</u>: Solicitud de certificación a partir de la que se generará el <b>certificado de voto</b>.<br/>
      <b>Respuesta:</b><br/>
         Si todo es correcto el <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Certificado-de-voto">certificados de voto</a>.
    </p>
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/solicitudAcceso/obtener">/solicitudAcceso/obtener</a><br/>
	<b>Parámetros:</b><br/>
    - <u>id</u>: El identificador de la solicitud que se desea consultar.<br/>
    <b>Respuesta:</b><br/>
         La <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Solicitud-de-acceso">solicitud de acceso</a>.
    </p> 
   	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/solicitudAcceso/encontrar">/solicitudAcceso/encontrar</a><br/>
	<b>Parámetros:</b><br/>
    - <u>hashSolicitudAccesoHex</u>: Hash en hexadecimal de la solicitud que se desea consultar.<br/>
    <b>Respuesta:</b><br/>
         La <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Solicitud-de-acceso">solicitud de acceso</a>.
    </p>     
  	<HR>
</div>


