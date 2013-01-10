<u><h3>Activación/desactivación de actores</h3></u>
<div>
  	Servicio que permite activar/desactivar actores a usuarios con el DNI en la lista de administradores.
  	<h4>Métodos soportados</h4>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/actorConIP/guardarSolicitudDesactivacion">/actorConIP/guardarSolicitudDesactivacion</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: Documento en formato <b>S/MIME</b> firmado por el <b>DNI electrónico</b> de un administrador del sistema.<br/>
         <b>Respuesta:</b><br/>
         Si todo es correcto una respuesta HTTP con código de estado 200.
    </p>
	<HR>
</div>
