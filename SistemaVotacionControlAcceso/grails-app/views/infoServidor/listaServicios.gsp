<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta name="layout" content="main" />
        <link rel="stylesheet" href="${resource(dir:'css',file:'tabmenu.css')}" />
    </head>
    <body>
 		<ul id="tabmenu">
			<li><a href="informacion">${message(code: 'infoLabel', null)}</a></li>
			<li><a class="active" href="listaServicios">${message(code: 'servicesListLabel', null)}</a></li>
			<li><a href="datosAplicacion">${message(code: 'appDataLabel', null)}</a></li>
		</ul>
		<div id="content" role="main">
			<g:include controller="infoServidor" />
			<g:include controller="certificado" />
			<g:include controller="eventoFirma" />
			<g:include controller="applet" />
			<g:include controller="anuladorVoto" />
			<g:include controller="buscador" />
			<g:include controller="documento" />
			<g:include controller="error400" />
			<g:include controller="error500" />
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
	</body>
</html>
