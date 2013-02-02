<u><h3>Solicitudes CSR (Certificate Signing Request)</h3></u>
<div>
  	Servicios relacionados con la certificación de usuarios.
  	<h4>Métodos soportados</h4>
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/csr/solicitar">/csr/solicitar</a><br/>
    	Servicio que tramita las solicitudes de los usurios que quieren obtener un certificado con el que poder identificarse
    	en las votaciones.
      	<b>Parámetros:</b><br/>
       	- <a href="http://en.wikipedia.org/wiki/Certificate_signing_request">Solicitud de certificación CSR</a>.<br/>
        	<b>Respuesta:</b><br/>
         	Si todo es correcto devolverá una respuesta HTTP con código de estado 200 y con el identificador de la 
			<b>solicitud de csr</b> en la base de datos.
    </p>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/csr/obtener">/csr/obtener</a><br/>
       <b>Parámetros:</b><br/>
      - <u>idSolicitudCSR</u>: El identificador en la base de datos de la <b>solicitud de csr </b> 
      cuyo certificado firmado se desea obtener.<br/>
      <b>Respuesta:</b><br/>
        - Si la solicitud ha sido validada se devolverá una respuesta HTTP con código de estado 200 y con el certificado firmado.<br/>
        - Si la solicitud no ha sido validada se devolverá una respuesta HTTP con código de estado 404.
    </p>   
    <p>- <u>POST</u> - <a href="${grailsApplication.config.grails.serverURL}/csr/firmar">/csr/firmar</a><br/>
      <b>Parámetros:</b><br/>
       - <u>archivoFirmado</u>: <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Documento-de-validaci%C3%B3n-de-usuario">
       Documento de validación de usuario</a>.<br/>
         <b>Respuesta:</b><br/>
         Si todo es correcto, una respuesta HTTP con código de estado 200.<br/>
    </p>
    </p>    
	<HR>
</div>
