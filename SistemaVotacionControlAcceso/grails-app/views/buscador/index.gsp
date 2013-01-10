<u><h3>Búsquedas</h3></u>
<div>
  	Búsquedas sobre la información del servidor.
  	<h4>Métodos soportados</h4>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/buscador/guardarReindex">/buscador/guardarReindex</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: Documento en formato <b>S/MIME</b> firmado por el <b>DNI electrónico</b> de un administrador del sistema.<br/>
         <b>Respuesta:</b><br/>
         Si todo es correcto hará una indexación desde cero de todos los documentos y devolverá una respuesta HTTP con código de estado 200.
    </p>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/buscador/consultaJSON">/buscador/consultaJSON</a><br/>
      <b>Parámetros:</b><br/>
       - Documento <b>JSON</b> con los parámetros de la consulta: <br/>
       <code>{conVotaciones:true, textQuery:prueba}</code><br/>
         <b>Respuesta:</b><br/>
         Una lista en formato <b>JSON</b> con los documentos que cumplan el criterio de la búsqueda.
    </p>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/buscador/eventoPorEtiqueta">/buscador/eventoPorEtiqueta</a><br/>
      <b>Parámetros:</b><br/>
       - <u>etiqueta</u>: El valor de la <b>etiqueta</b> que se desee buscar<br/>
         <b>Respuesta:</b><br/>
         Una lista en formato <b>JSON</b> con los documentos que tengan asociada la etiqueta.
    </p>
	<HR>
</div>
