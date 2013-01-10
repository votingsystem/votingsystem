<u><h3>Subscripciones</h3></u>
<div>
  	<h4>Métodos soportados</h4>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/subscripcion/guardarAsociacionConCentroControl">/subscripcion/guardarAsociacionConCentroControl</a><br/>
      	Servicio que permite guardar asociaciones con <a href="https://github.com/jgzornoza/SistemaVotacionCentroControl/wiki/Centro-de-Control">Centros de Control</a>.<br/><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Solicitud-de-Asociaci%C3%B3n">Solicitud de Asociación</a>.<br/>
         <b>Respuesta:</b><br/>
         Si todo es correcto una respuesta HTTP con código de estado 200.
    </p>
	<HR>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/subscripcion/reclamaciones">/subscripcion/reclamaciones</a><br/>
      	Servicio de subscripciones (RSS, ATOM) de las reclamaciones publicadas en el sistema<br/><br/>
      <b>Parámetros:</b><br/>
       - <u>feedType</u>:Formato de la subscripción deseado, si no se proporciona ninguno devuelve el formato Atom 1.0</a>.<br/>
    </p>
	<HR>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/subscripcion/votaciones">/subscripcion/votaciones</a><br/>
      	Servicio de subscripciones (RSS, ATOM) de las votaciones publicadas en el sistema<br/><br/>
      <b>Parámetros:</b><br/>
       - <u>feedType</u>:Formato de la subscripción deseado, si no se proporciona ninguno devuelve el formato Atom 1.0</a>.<br/>
    </p>
	<HR>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/subscripcion/manifiestos">/subscripcion/manifiestos</a><br/>
      	Servicio de subscripciones (RSS, ATOM) de los manifiestos publicados en el sistema<br/><br/>
      <b>Parámetros:</b><br/>
       - <u>feedType</u>:Formato de la subscripción deseado, si no se proporciona ninguno devuelve el formato Atom 1.0</a>.<br/>
    </p>
	<HR>
	
	
</div>





