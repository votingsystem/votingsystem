<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta name="layout" content="main"/>
    </head>
    <body>
    	<div class="container">
   	    
	   	<ul class="nav nav-tabs">
			<li><a href="informacion"><g:message code="infoLabel"/></a></li>
			<li class="active"><a href="listaServicios"><g:message code="serviceURLSMsg"/></a></li>
			<li><a href="datosAplicacion"><g:message code="appDataLabel"/></a></li>
		</ul>

		<div id="content" role="main">
			<g:include controller="mensajeSMIME" />
			<g:include controller="eventoVotacion" />
			<g:include controller="voto" />
			<g:include controller="infoServidor" />
			<g:include controller="app" />
			<g:include controller="anuladorVoto" />
			<g:include controller="buscador" />
			<g:include controller="certificado" />
			<g:include controller="subscripcion" />
		</div>
	</div>		
	</body>
</html>
