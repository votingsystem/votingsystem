
<u><h3> Servicio de Certificados </h3></u>

<p> Servicios relacionados con los certificados manejados por la aplicación </p>


<h4>URLs de servicio</h4>
<div>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/certificado/index">/certificado/index</a><br/>  	

</p>
	
</p>



<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/certificado'.</p>

<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/certificado/certificadoUsuario">/certificado/certificadoUsuario</a><br/>  	
Servicio de consulta de certificados de usuario. <br/>
</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>usuarioId</u>:  Obligatorio. El identificador en la base de datos del usuario.<br/>
		
</p>
	
</p>



<p><b>Respuesta:</b><br/>El certificado en formato PEM.</p>

<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/certificado/certificadoCA_DeEvento">/certificado/certificadoCA_DeEvento</a><br/>  	
Servicio de consulta de los certificados emisores de certificados de voto para una votación. <br/>
</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>idEvento</u>:  el identificador de la votación que se desea consultar.<br/>
		
		- <u>controlAccesoId</u>:  el identificador en la base de datos del control de acceso en el 		  que se publicó la votación.<br/>
		
</p>
	
</p>



<p><b>Respuesta:</b><br/>Devuelve la cadena de certificación, en formato PEM, con la que se generan los 			certificados de los votos.</p>

<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/certificado/cadenaCertificacion">/certificado/cadenaCertificacion</a><br/>  	

</p>
	
</p>



<p><b>Respuesta:</b><br/>La cadena de certificación del servidor</p>

<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/certificado/certificadoDeVoto">/certificado/certificadoDeVoto</a><br/>  	
Servicio de consulta de certificados de voto. <br/>
</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>hashCertificadoVotoHex</u>:  Obligatorio. Hash en hexadecimal asociado al          certificado de voto que se desea consultar.<br/>
		
</p>
	
</p>



<p><b>Respuesta:</b><br/>El certificado en formato PEM.</p>

<HR>


</div>


