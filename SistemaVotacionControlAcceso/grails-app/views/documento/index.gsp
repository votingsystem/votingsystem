<u><h3>Obtención de PDFs</h3></u>
<div>
  	<p>Servicio de decargas de PDFs.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/documento/obtenerManifiesto">/documento/obtenerManifiesto</a><br/>
      <b>Parámetros:</b><br/>
       - <u>id</u>: El identificador del evento.<br/>
         <b>Respuesta:</b><br/>
         El PDF del manifiesto firmado por el usuario que lo publicó.
    </p>
	<HR>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/documento/obtenerFirmaManifiesto">/documento/obtenerFirmaManifiesto</a><br/>
      <b>Parámetros:</b><br/>
       - <u>id</u>: El identificador de la firma de manifiesto en la base de datos.<br/>
         <b>Respuesta:</b><br/>
         La firma del manifiesto en formato PDF.
    </p>
</div>