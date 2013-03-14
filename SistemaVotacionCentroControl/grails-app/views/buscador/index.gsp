
<u><h3> Búsquedas </h3></u>

<p> Servicios de búsqueda sobre los datos generados por la aplicación </p>


<h4>URLs de servicio</h4>
<div>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/buscador/index">/buscador/index</a><br/>  	

</p>
	
</p>



<p><b>Respuesta:</b><br/>Información sobre los servicios que tienen como url base '/buscador'</p>

<HR>

<p>
- <u>POST</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/buscador/consultaJSON">/buscador/consultaJSON</a><br/>  	

</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>consulta</u>:  Documento JSON con los parámetros de la consulta:<br/><code> 		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code><br/>
		
</p>
	
</p>



<p><b>Respuesta:</b><br/>Una lista en formato JSON con los documentos que cumplen el criterio de la búsqueda.</p>

<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/buscador/reindex">/buscador/reindex</a><br/>  	
Servicio que reindexa el motor de búsqueda <br/>
</p>
	
</p>





<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/buscador/evento">/buscador/evento</a><br/>  	
Servicio que busca la cadena de texto recibida entre las votaciones publicadas. <br/>
</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>max</u>:  Opcional (por defecto 20). Número máximo de documentos que 		  devuelve la consulta (tamaño de la página).<br/>
		
		- <u>consultaTexto</u>:  Obligatorio. Texto de la búsqueda.<br/>
		
		- <u>offset</u>:  Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
		
</p>
	
</p>





<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/buscador/guardarReindex">/buscador/guardarReindex</a><br/>  	
Servicio que reindexa los datos del motor de búsqueda <br/>
</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>archivoFirmado</u>:  Obligatorio. Solicitud firmada por un administrador de sistema.<br/>
		
</p>
	
</p>





<HR>

<p>
- <u>GET</u> - 
<a href="http://192.168.1.5:8080/SistemaVotacionCentroControl/buscador/eventoPorEtiqueta">/buscador/eventoPorEtiqueta</a><br/>  	
Servicio que busca los eventos que tienen la etiqueta que se pasa como parámetro. <br/>
</p>
	
	<p><b>Parámetros:</b><br/>

		
		- <u>max</u>:  Opcional (por defecto 20). Número máximo de documentos que 		  devuelve la consulta (tamaño de la página).<br/>
		
		- <u>offset</u>:  Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.<br/>
		
		- <u>etiqueta</u>:  Obligatorio. Texto de la etiqueta.<br/>
		
</p>
	
</p>





<HR>


</div>


