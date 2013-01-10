<u><h3>Servicio de Votos</h3></u>
<div>
  	<p>Servicio que procesa los votos recibidos.</p>
  	  	<h3>Métodos soportados</h3>
    	<p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/voto/guardarAdjuntandoValidacion">/voto/guardarAdjuntandoValidacion</a><br/>
      	<b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: El voto firmado por el 
       	<a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Certificado-de-voto">Certificado de Voto</a>.<br/>
         <b>Respuesta:</b><br/>
		El <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Recibo-de-Voto">Recibo del Voto</a>
   		 </p>
	<HR>
    	<p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/voto/obtener">/voto/obtener</a><br/>
      	<b>Parámetros:</b><br/>
       - <u>hashCertificadoVotoHex</u>: Hash en hexadecimal asociado al certificado con el que el usuario firmó el voto.<br/>
        <b>Respuesta:</b><br/> Documento JSON con la información del voto
   		 </p>
	
	
</div>



