<u><h3>Recolección de reclamaciones</h3></u>
<div>
  	<p>Servicio que recoger las reclamaciones.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/${params.controller}/guardarAdjuntandoValidacion">/${params.controller}/guardarAdjuntandoValidacion</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: El documento con la reclamación firmada con el <b>DNI</b> del usuario.<br/>
         <b>Respuesta:</b><br/>
         Si todo es correcto devolverá la reclamación con el certificado de servidor.
    </p>
	<HR>
</div>

