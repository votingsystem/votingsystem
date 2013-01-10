<u><h3>Certificados</h3></u>
<div>
  	<p>Claves públicas de certificados empleados por la aplicación.</p>
  	<h3>Métodos soportados</h3>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/certificado/cadenaCertificacion">/certificado/cadenaCertificacion</a><br/>
      <b>Respuesta:</b><br/>
         La cadena de certificación del <b>certificado</b> que el servidor emplea para firmar documentos.
    </p>
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/certificado/raizDNIe">/certificado/raizDNIe</a><br/>
      <b>Respuesta:</b><br/>
         La clave pública del certificado raíz del <b>DNI español</b>.
    </p>    
    <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/certificado/certificadoDeVoto">/certificado/certificadoDeVoto</a><br/>
      <b>Parámetros:</b><br/>
      - <u>hashCertificadoVotoHex</u>: Hash en hexadecimal del 
      <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Certificado-de-voto">Certificado de Voto</a>.<br/>
      <b>Respuesta:</b><br/>
         La clave pública del <a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Certificado-de-voto">Certificado de Voto</a>.
    </p> 
      <p>- <u>GET</u> - <a href="${grailsApplication.config.grails.serverURL}/certificado/certificadoCA_DeEvento">/certificado/certificadoCA_DeEvento</a><br/>
      <b>Parámetros:</b><br/>
      - <u>idEvento</u>: El identificador del evento cuyo certificado raíz se pretende obtener.<br/>
      <b>Respuesta:</b><br/>
         La clave pública del <b>certificado raíz</b> empleado para firmar los
		<a href="https://github.com/jgzornoza/SistemaVotacionClientePublicacion/wiki/Certificado-de-voto">Certificados de Voto</a>
		del evento.
    </p>       
	<HR>
</div>


