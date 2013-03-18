<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta name="layout" content="main" />
    </head>
    <body>
    <div class="container">
    
 		<ul class="nav nav-tabs">
			<li><a href="informacion"><g:message code="infoLabel"/></a></li>
			<li class="active"><a href="listaServicios"><g:message code="serviceURLSMsg"/></a></li>
			<li><a href="datosAplicacion"><g:message code="appDataLabel"/></a></li>
		</ul>
		
		<div id="content" role="main">
			<g:include controller="infoServidor" />
			<g:include controller="certificado" />
			<g:include controller="eventoFirma" />
			<g:include controller="applet" />
			<g:include controller="anuladorVoto" />
			<g:include controller="buscador" />
			<g:include controller="documento" />
			<g:include controller="evento" />
			<g:include controller="eventoReclamacion" />
			<g:include controller="eventoVotacion" />
			<g:include controller="recolectorReclamacion" />
			<g:include controller="recolectorFirma" />
			<g:include controller="solicitudAcceso" />
			<g:include controller="subscripcion" />
			<g:include controller="voto" />
			<g:include controller="mensajeSMIME" />
			<g:include controller="csr" />
			<g:include controller="actorConIP" />			
		</div>
	</div>		
	</body>
</html>
