
<u><h3> Servicio de Votos </h3></u>

<p> Servicio que procesa los votos recibidos. </p>


<h4>URLs de servicio</h4>
<div>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/voto/index">/voto/index</a><br/>  	

</p>
	
</p>



<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/voto'.</p>

<HR>

<p>
- <u>POST</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/voto/guardarAdjuntandoValidacion">/voto/guardarAdjuntandoValidacion</a><br/>  	
Servicio que recoge los votos enviados por los usuarios. <br/>
</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>archivoFirmado</u>: 	Obligatorio. El voto firmado por el        <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a><br/>
		
</p>
	
</p>



<p><b>Respuesta:</b><br/><a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a></p>

<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/voto/obtener">/voto/obtener</a><br/>  	
Servicio que devuelve la información de un voto a partir del hash asociado al mismo <br/>
</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>hashCertificadoVotoHex</u>: 	Obligatorio. Hash en hexadecimal asociado al voto.<br/>
		
</p>
	
</p>



<p><b>Respuesta:</b><br/>La información del voto solicitado en formato JSON.</p>

<HR>


</div>


